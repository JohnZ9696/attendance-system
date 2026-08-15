package com.iot.attendance.repository;

import com.iot.attendance.entity.AttendanceRecord;
import com.iot.attendance.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
@Deprecated
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, UUID> {
    List<AttendanceRecord> findByUser(User user);
    List<AttendanceRecord> findByCheckInTimeBetween(OffsetDateTime start, OffsetDateTime end);
    List<AttendanceRecord> findByUserAndCheckInTimeBetween(User user, OffsetDateTime start, OffsetDateTime end);
    void deleteByUserId(UUID userId);
}
