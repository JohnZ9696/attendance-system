package com.iot.attendance.dto;

import com.iot.attendance.entity.AttendanceLog;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AttendanceResponse(
        UUID id,
        LocalDate attendanceDate,
        OffsetDateTime checkInTime,
        String status,
        Integer lateMinutes,
        UserSummary user
) {
    public record UserSummary(
            UUID id,
            String mssv,
            String name
    ) {}

    public static AttendanceResponse from(AttendanceLog log) {
        return new AttendanceResponse(
                log.getId(),
                log.getAttendanceDate(),
                log.getCheckTime(),
                log.getStatus(),
                log.getLateMinutes(),
                new UserSummary(
                        log.getStudent().getId(),
                        log.getStudent().getMssv(),
                        log.getStudent().getFullName()
                )
        );
    }
}