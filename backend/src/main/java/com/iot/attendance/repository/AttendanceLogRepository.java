package com.iot.attendance.repository;

import com.iot.attendance.entity.AttendanceLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttendanceLogRepository extends JpaRepository<AttendanceLog, UUID> {
    Optional<AttendanceLog> findByStudentIdAndAttendanceDate(UUID studentId, LocalDate attendanceDate);
    
    List<AttendanceLog> findByAttendanceDate(LocalDate date);
    List<AttendanceLog> findByAttendanceDateAndStatus(LocalDate date, String status);
    List<AttendanceLog> findByStudentId(UUID studentId);
    List<AttendanceLog> findByAttendanceDateBetween(LocalDate start, LocalDate end);
    
    Page<AttendanceLog> findByAttendanceDate(LocalDate date, Pageable pageable);
    Page<AttendanceLog> findByStudentId(UUID studentId, Pageable pageable);
    Page<AttendanceLog> findByStatus(String status, Pageable pageable);
    Page<AttendanceLog> findByAttendanceDateAndStatus(LocalDate date, String status, Pageable pageable);
    Page<AttendanceLog> findByStudentIdAndStatus(UUID studentId, String status, Pageable pageable);
    Page<AttendanceLog> findByAttendanceDateAndStudentId(LocalDate date, UUID studentId, Pageable pageable);
    Page<AttendanceLog> findByAttendanceDateAndStudentIdAndStatus(LocalDate date, UUID studentId, String status, Pageable pageable);
}
