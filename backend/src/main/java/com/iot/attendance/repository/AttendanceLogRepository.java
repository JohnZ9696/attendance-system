package com.iot.attendance.repository;

import com.iot.attendance.entity.AttendanceLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface AttendanceLogRepository extends JpaRepository<AttendanceLog, UUID> {
    /**
 * Finds an attendance log for a student on a specified date.
 *
 * @param studentId       the student's identifier
 * @param attendanceDate  the attendance date
 * @return the matching attendance log, if one exists
 */
Optional<AttendanceLog> findByStudentIdAndAttendanceDate(UUID studentId, LocalDate attendanceDate);
}
