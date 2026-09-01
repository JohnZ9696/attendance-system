package com.iot.attendance.service;

import com.iot.attendance.dto.CvVerificationResponse;
import com.iot.attendance.dto.RfidScanResponse;
import com.iot.attendance.entity.AttendanceLog;
import com.iot.attendance.entity.Student;
import com.iot.attendance.entity.VerificationLog;
import com.iot.attendance.entity.VerificationResult;
import com.iot.attendance.repository.AttendanceLogRepository;
import com.iot.attendance.repository.StudentRepository;
import com.iot.attendance.repository.VerificationLogRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class CheckInFlowService {

    private static final Logger log = LoggerFactory.getLogger(CheckInFlowService.class);

    private final StudentRepository studentRepository;
    private final VerificationLogRepository verificationLogRepository;
    private final FastApiClient fastApiClient;
    private final AttendanceLogRepository attendanceLogRepository;
    private final AttendanceService attendanceService;
    private final SettingsService settingsService;
    private final SseEventService sseEventService;
    private final ParentNotificationService parentNotificationService;

    @Value("${attendance.camera-id:cam-01}")
    private String cameraId;

    public CheckInFlowService(StudentRepository studentRepository, VerificationLogRepository verificationLogRepository, FastApiClient fastApiClient, AttendanceLogRepository attendanceLogRepository, AttendanceService attendanceService, SettingsService settingsService, SseEventService sseEventService, ParentNotificationService parentNotificationService) {
        this.studentRepository = studentRepository;
        this.verificationLogRepository = verificationLogRepository;
        this.fastApiClient = fastApiClient;
        this.attendanceLogRepository = attendanceLogRepository;
        this.attendanceService = attendanceService;
        this.settingsService = settingsService;
        this.sseEventService = sseEventService;
        this.parentNotificationService = parentNotificationService;
    }

    public RfidScanResponse handleRfidScan(String deviceId, String rfidUid) {
        String normalizedUid = rfidUid.toUpperCase().replaceAll("[:\\s]", "");
        Student student = studentRepository.findByUidAndIsActiveTrue(normalizedUid)
                .orElseThrow(() -> new IllegalArgumentException("STUDENT_NOT_FOUND_OR_INACTIVE"));

        if (student.getFaceEmbedding() == null || student.getFaceEmbedding().isBlank()) {
            throw new IllegalStateException("FACE_NOT_ENROLLED");
        }

        VerificationLog verificationLog = new VerificationLog();
        verificationLog.setStudent(student);
        verificationLog.setScannedUid(normalizedUid);
        verificationLog.setResult(VerificationResult.PENDING);
        verificationLog.setStartedAt(OffsetDateTime.now());
        verificationLog = verificationLogRepository.save(verificationLog);

        try {
            int similarityThresholdPercent = settingsService.getSimilarityThresholdPercent();
            var response = fastApiClient.requestFaceVerification(
                    verificationLog.getId(),
                    student.getId(),
                    cameraId,
                    similarityThresholdPercent
            ).block();
            log.info("[VERIFICATION RESULT] {}", response.result());
            String finalResult = complete(verificationLog.getId(), response, similarityThresholdPercent);
            
            AttendanceLog attendance = attendanceLogRepository.findByVerification_Id(verificationLog.getId()).orElse(null);
            
            return new RfidScanResponse(
                verificationLog.getId(),
                finalResult != null ? finalResult : response.result().name(),
                finalResult != null ? finalResult : response.result().name(),
                attendance != null ? attendance.getStatus() : null,
                attendance != null ? attendance.getLateMinutes() : null,
                student.getFullName(),
                attendance != null ? attendance.getAttendanceDate() : null,
                attendance != null ? attendance.getCheckTime() : null
            );
        } catch (Exception error) {
            log.error("Face verification failed", error);
            fail(verificationLog.getId(), "FASTAPI_UNAVAILABLE_OR_TIMEOUT");
            return new RfidScanResponse(
                verificationLog.getId(),
                "ERROR",
                "Face verification service unavailable",
                null, null, student.getFullName(), null, null
            );
        }
    }

    @Transactional
    public String complete(UUID verificationId, CvVerificationResponse response, int similarityThresholdPercent) {
        VerificationLog log = verificationLogRepository.findById(verificationId)
                .orElseThrow(() -> new IllegalArgumentException("VERIFICATION_NOT_FOUND"));

        if (log.getResult() != VerificationResult.PENDING) {
            return log.getResult().name();
        }

        validateCvResponse(log, response, similarityThresholdPercent);
        VerificationResult mappedResult = decideVerificationResult(response, similarityThresholdPercent);
        String failureReason = failureReasonFor(mappedResult, response.failureReason());
        
        log.setResult(mappedResult);
        log.setSimilarityPercent(BigDecimal.valueOf(response.similarityPercent()));
        log.setLivenessPassed(response.livenessPassed());
        log.setFailureReason(failureReason);
        log.setModelName(response.modelName());
        log.setModelVersion(response.modelVersion());
        log.setCompletedAt(OffsetDateTime.now());
        verificationLogRepository.save(log);

        String resultName = mappedResult.name();
        String message = failureReason;

        if (mappedResult == VerificationResult.VERIFIED) {
            try {
                var attendanceLog = attendanceService.recordAttendance(log.getStudent(), log);
                message = "Diem danh thanh cong";

                sseEventService.publishEvent("attendance_event", Map.of(
                        "studentId", log.getStudent().getId().toString(),
                        "studentName", log.getStudent().getFullName(),
                        "studentMssv", log.getStudent().getMssv(),
                        "status", attendanceLog.getStatus(),
                        "lateMinutes", attendanceLog.getLateMinutes(),
                        "checkTime", attendanceLog.getCheckTime().toString()
                ));

                try {
                    parentNotificationService.notifyCheckIn(log.getStudent(), attendanceLog, log);
                } catch (RuntimeException notificationException) {
                    CheckInFlowService.log.error("Parent notification failed for verification {}", verificationId, notificationException);
                }
            } catch (RuntimeException exception) {
                if ("ALREADY_CHECKED_IN".equals(exception.getMessage())) {
                    resultName = "ALREADY_CHECKED_IN";
                    message = "Sinh vien da diem danh hom nay";
                } else {
                    throw exception;
                }
            }
        }

        if (message == null || message.isBlank()) {
            message = resultName;
        }

        Map<String, Object> event = new LinkedHashMap<>();
        event.put("verificationId", log.getId());
        event.put("studentId", log.getStudent().getId());
        event.put("mssv", log.getStudent().getMssv());
        event.put("fullName", log.getStudent().getFullName());
        event.put("result", resultName);
        event.put("message", message);
        event.put("similarityPercent", response.similarityPercent());
        event.put("thresholdPercent", similarityThresholdPercent);
        event.put("livenessPassed", response.livenessPassed());

        sseEventService.publishEvent("verification_update", event);
        
        return resultName;
    }

    @Transactional
    public void fail(UUID verificationId, String reason) {
        verificationLogRepository.findById(verificationId).ifPresent(log -> {
            if (log.getResult() == VerificationResult.PENDING) {
                log.setResult(VerificationResult.ERROR);
                log.setFailureReason(reason);
                log.setCompletedAt(OffsetDateTime.now());
                verificationLogRepository.save(log);

                Map<String, Object> event = new LinkedHashMap<>();
                event.put("verificationId", verificationId);
                event.put("result", "ERROR");
                event.put("message", reason);

                sseEventService.publishEvent("verification_update", event);
            }
        });
    }

    private VerificationResult mapFailureReason(VerificationResult fastApiResult, String failureReason) {
        if (failureReason == null) {
            return fastApiResult;
        }
        
        return switch (failureReason) {
            case "MULTIPLE_FACES" -> VerificationResult.MULTIPLE_FACES;
            case "SIMILARITY_BELOW_THRESHOLD" -> VerificationResult.FACE_BELOW_THRESHOLD;
            case "OPEN_CLOSED_OPEN_BLINK_NOT_COMPLETED" -> VerificationResult.LIVENESS_FAILED;
            case "NO_FACE_IN_CAPTURE_WINDOW" -> VerificationResult.CAPTURE_TIMEOUT;
            case "NO_FRESH_CAMERA_FRAME" -> VerificationResult.CAMERA_OFFLINE;
            case "FACE_MODEL_TIMEOUT", "NO_VALID_FACE_FOR_MATCHING" -> VerificationResult.FACE_MATCH_TIMEOUT;
            case "FACE_PROFILE_NOT_FOUND_OR_INACTIVE" -> VerificationResult.FACE_NOT_ENROLLED;
            default -> fastApiResult;
        };
    }

    private VerificationResult decideVerificationResult(
            CvVerificationResponse response,
            int similarityThresholdPercent
    ) {
        VerificationResult fastApiResult = mapFailureReason(response.result(), response.failureReason());
        boolean comparisonCompleted = fastApiResult == VerificationResult.VERIFIED
                || fastApiResult == VerificationResult.FACE_BELOW_THRESHOLD;

        if (!comparisonCompleted) {
            return fastApiResult;
        }
        if (!Boolean.TRUE.equals(response.livenessPassed())) {
            return VerificationResult.LIVENESS_FAILED;
        }
        if (response.similarityPercent() < similarityThresholdPercent) {
            return VerificationResult.FACE_BELOW_THRESHOLD;
        }
        return VerificationResult.VERIFIED;
    }

    private void validateCvResponse(
            VerificationLog verificationLog,
            CvVerificationResponse response,
            int similarityThresholdPercent
    ) {
        boolean correlationMatches = response != null
                && verificationLog.getId().equals(response.verificationSessionId())
                && verificationLog.getStudent().getId().equals(response.expectedUserId())
                && cameraId.equals(response.cameraId());
        boolean scoreIsValid = response != null
                && response.similarityPercent() != null
                && response.similarityPercent() >= 0
                && response.similarityPercent() <= 100;
        boolean thresholdMatches = response != null
                && response.thresholdPercent() != null
                && Math.abs(response.thresholdPercent() - similarityThresholdPercent) < 0.001;

        if (!correlationMatches || !scoreIsValid || !thresholdMatches) {
            throw new IllegalStateException("INVALID_CV_VERIFICATION_RESPONSE");
        }
    }

    private String failureReasonFor(VerificationResult result, String fastApiFailureReason) {
        return switch (result) {
            case VERIFIED -> null;
            case LIVENESS_FAILED -> "OPEN_CLOSED_OPEN_BLINK_NOT_COMPLETED";
            case FACE_BELOW_THRESHOLD -> "SIMILARITY_BELOW_THRESHOLD";
            default -> fastApiFailureReason;
        };
    }
}
