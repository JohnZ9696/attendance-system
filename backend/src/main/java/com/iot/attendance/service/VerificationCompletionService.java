package com.iot.attendance.service;

import com.iot.attendance.dto.CvVerificationResponse;
import com.iot.attendance.entity.VerificationLog;
import com.iot.attendance.entity.VerificationResult;
import com.iot.attendance.repository.VerificationLogRepository;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class VerificationCompletionService {

    private final VerificationLogRepository verificationLogRepository;
    private final AttendanceService attendanceService;
    private final SseEventService sseEventService;

    public VerificationCompletionService(
            VerificationLogRepository verificationLogRepository,
            AttendanceService attendanceService,
            SseEventService sseEventService
    ) {
        this.verificationLogRepository = verificationLogRepository;
        this.attendanceService = attendanceService;
        this.sseEventService = sseEventService;
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

        // Publish verification_update event
        sseEventService.publishEvent("verification_update", Map.of(
                "verificationId", verificationId.toString(),
                "result", response.result().name(),
                "similarityPercent", response.similarityPercent(),
                "livenessPassed", response.livenessPassed(),
                "failureReason", response.failureReason()
        ));

        if (response.result() == VerificationResult.VERIFIED) {
            try {
                var attendanceLog = attendanceService.recordAttendance(log.getStudent(), log);
                // Publish attendance_event
                sseEventService.publishEvent("attendance_event", Map.of(
                        "studentId", log.getStudent().getId().toString(),
                        "studentName", log.getStudent().getFullName(),
                        "studentMssv", log.getStudent().getMssv(),
                        "status", attendanceLog.getStatus(),
                        "lateMinutes", attendanceLog.getLateMinutes(),
                        "checkTime", attendanceLog.getCheckTime().toString()
                ));
            } catch (RuntimeException exception) {
                if (!"ALREADY_CHECKED_IN".equals(exception.getMessage())) {
                    throw exception;
                }
            }
        }
    }

    @Transactional
    public void fail(UUID verificationId, String reason) {
        verificationLogRepository.findById(verificationId).ifPresent(log -> {
            if (log.getResult() == VerificationResult.PENDING) {
                log.setResult(VerificationResult.ERROR);
                log.setFailureReason(reason);
                log.setCompletedAt(OffsetDateTime.now());
                verificationLogRepository.save(log);

                sseEventService.publishEvent("verification_update", Map.of(
                        "verificationId", verificationId.toString(),
                        "result", "ERROR",
                        "failureReason", reason
                ));
            }
        });
    }
}