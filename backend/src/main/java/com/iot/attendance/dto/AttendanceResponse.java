package com.iot.attendance.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AttendanceResponse(
        UUID id,
        UUID studentId,
        String studentName,
        String studentMssv,
        LocalDate attendanceDate,
        OffsetDateTime checkInTime,
        String status,
        Integer lateMinutes
) {
    public static AttendanceResponse from(com.iot.attendance.entity.AttendanceLog log) {
        return new AttendanceResponse(
                log.getId(),
                log.getStudent().getId(),
                log.getStudent().getFullName(),
                log.getStudent().getMssv(),
                log.getAttendanceDate(),
                log.getCheckTime(),
                log.getStatus(),
                log.getLateMinutes()
        );
    }
}