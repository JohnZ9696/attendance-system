package com.iot.attendance.repository;

import com.iot.attendance.entity.AttendanceLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface AttendanceLogRepository extends JpaRepository<AttendanceLog, UUID> {
    Optional<AttendanceLog> findByStudentIdAndAttendanceDate(UUID studentId, LocalDate attendanceDate);
}
