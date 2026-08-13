package com.iot.attendance.repository;

import com.iot.attendance.entity.AttendanceRecord;
import com.iot.attendance.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {
    List<AttendanceRecord> findByUser(User user);
    List<AttendanceRecord> findByCheckInTimeBetween(LocalDateTime start, LocalDateTime end);
}