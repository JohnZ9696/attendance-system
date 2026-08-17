package com.iot.attendance.service;

import com.iot.attendance.dto.CvVerificationResponse;
import com.iot.attendance.entity.VerificationLog;
import com.iot.attendance.entity.VerificationResult;
import com.iot.attendance.repository.VerificationLogRepository;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class VerificationCompletionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(VerificationCompletionService.class);

    private final VerificationLogRepository verificationLogRepository;
    private final AttendanceService attendanceService;
    private final SseEventService sseEventService;
    private final ParentNotificationService parentNotificationService;

    public VerificationCompletionService(
            VerificationLogRepository verificationLogRepository,
            AttendanceService attendanceService,
            SseEventService sseEventService,
            ParentNotificationService parentNotificationService
    ) {
        this.verificationLogRepository = verificationLogRepository;
        this.attendanceService = attendanceService;
        this.sseEventService = sseEventService;
        this.parentNotificationService = parentNotificationService;
    }

    @Transactional
    public void complete(UUID verificationId, CvVerificationResponse response) {
        VerificationLog log = verificationLogRepository.findById(verificationId)
                .orElseThrow(() -> new IllegalArgumentException("VERIFICATION_NOT_FOUND"));

        if (log.getResult() != VerificationResult.PENDING) {
            return;
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

                // Publish attendance_event cho History tự động tải lại
                sseEventService.publishEvent("attendance_event", Map.of(
                        "studentId", log.getStudent().getId().toString(),
                        "studentName", log.getStudent().getFullName(),
                        "studentMssv", log.getStudent().getMssv(),
                        "status", attendanceLog.getStatus(),
                        "lateMinutes", attendanceLog.getLateMinutes(),
                        "checkTime", attendanceLog.getCheckTime().toString()
                ));

                // Notify parents
                try {
                    parentNotificationService.notifyCheckIn(log.getStudent(), attendanceLog, log);
                } catch (RuntimeException notificationException) {
                    LOGGER.error("Parent notification failed for verification {}", verificationId, notificationException);
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