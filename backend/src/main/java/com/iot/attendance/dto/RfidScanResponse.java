package com.iot.attendance.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record RfidScanResponse(
        UUID verificationSessionId,
        String message,
        String errorCode,
        String status,
        Integer lateMinutes,
        String studentName,
        LocalDate attendanceDate,
        OffsetDateTime checkTime
) {}