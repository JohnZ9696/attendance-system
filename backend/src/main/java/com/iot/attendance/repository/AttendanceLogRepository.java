package com.iot.attendance.repository;

import com.iot.attendance.entity.AttendanceLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttendanceLogRepository extends JpaRepository<AttendanceLog, UUID> {
    Optional<AttendanceLog> findByStudentIdAndAttendanceDate(UUID studentId, LocalDate attendanceDate);

    List<AttendanceLog> findByStudentIdOrderByCheckTimeDesc(UUID studentId);

    List<AttendanceLog> findByAttendanceDateOrderByCheckTimeDesc(LocalDate attendanceDate);

    List<AttendanceLog> findByAttendanceDateBetweenOrderByCheckTimeDesc(LocalDate from, LocalDate to);

    List<AttendanceLog> findAllByOrderByCheckTimeDesc();
}
