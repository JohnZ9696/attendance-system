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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckInFlowService {

    private final StudentRepository studentRepository;
    private final VerificationLogRepository verificationLogRepository;
    private final FastApiClient fastApiClient;
    private final AttendanceLogRepository attendanceLogRepository;
    private final AttendanceService attendanceService;
    private final SseEventService sseEventService;
    private final ParentNotificationService parentNotificationService;

    @Value("${attendance.camera-id:cam-01}")
    private String cameraId;

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
            var response = fastApiClient.requestFaceVerification(verificationLog.getId(), student.getId(), cameraId).block();
            log.info("[VERIFICATION RESULT] {}", response.result());
            String finalResult = complete(verificationLog.getId(), response);
            
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
    public String complete(UUID verificationId, CvVerificationResponse response) {
        VerificationLog log = verificationLogRepository.findById(verificationId)
                .orElseThrow(() -> new IllegalArgumentException("VERIFICATION_NOT_FOUND"));

        if (log.getResult() != VerificationResult.PENDING) {
            return log.getResult().name();
        }

        log.setResult(response.result());
        log.setSimilarityPercent(BigDecimal.valueOf(response.similarityPercent()));
        log.setLivenessPassed(response.livenessPassed());
        log.setFailureReason(response.failureReason());
        log.setModelName(response.modelName());
        log.setModelVersion(response.modelVersion());
        log.setCompletedAt(OffsetDateTime.now());
        verificationLogRepository.save(log);

        String resultName = response.result().name();
        String message = response.failureReason();

        if (response.result() == VerificationResult.VERIFIED) {
            try {
                var attendanceLog = attendanceService.recordAttendance(log.getStudent(), log);
                message = "Điểm danh thành công";

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
                    message = "Sinh viên đã điểm danh hôm nay";
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
}
