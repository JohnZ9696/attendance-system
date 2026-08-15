package com.iot.attendance.dto;

import com.iot.attendance.entity.VerificationResult;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CvVerificationResponse(
        UUID verificationSessionId,
        UUID expectedUserId,
        String cameraId,
        VerificationResult result,
        Double similarityPercent,
        Double thresholdPercent,
        Boolean livenessPassed,
        String modelName,
        String modelVersion,
        OffsetDateTime processedAt,
        String failureReason
) {}