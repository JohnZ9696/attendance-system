package com.iot.attendance.dto;

import java.util.UUID;

public record FaceEnrollmentResponse(
        UUID studentId,
        boolean faceRegistered
) {}