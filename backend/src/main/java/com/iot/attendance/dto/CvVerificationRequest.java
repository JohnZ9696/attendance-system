package com.iot.attendance.dto;

import java.util.UUID;

public record CvVerificationRequest(
        UUID sessionId,
        UUID expectedUserId,
        String cameraId,
        Integer captureTimeoutMs,
        Integer matchTimeoutMs
) {}