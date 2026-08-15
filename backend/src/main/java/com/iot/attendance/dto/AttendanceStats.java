package com.iot.attendance.dto;

public record AttendanceStats(
        long totalRecords,
        long onTimeCount,
        long lateCount,
        long uniqueStudents
) {}