package com.iot.attendance.service;

import com.iot.attendance.entity.AttendanceRecord;
import com.iot.attendance.entity.User;
import com.iot.attendance.repository.AttendanceRecordRepository;
import com.iot.attendance.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AttendanceService {
    @Autowired
    private AttendanceRecordRepository attendanceRecordRepository;

    @Autowired
    private UserRepository userRepository;

    public List<AttendanceRecord> getAllRecords() {
        return attendanceRecordRepository.findAll();
    }

    public List<AttendanceRecord> getTodayRecords() {
        LocalDate today = LocalDate.now();
        return getRecordsBetween(today.atStartOfDay().atOffset(ZoneOffset.UTC), today.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC));
    }

    public List<AttendanceRecord> getRecordsByUser(UUID userId) {
        Optional<User> user = userRepository.findById(userId);
        return user.map(attendanceRecordRepository::findByUser).orElse(List.of());
    }

    public List<AttendanceRecord> getRecordsBetween(OffsetDateTime start, OffsetDateTime end) {
        return attendanceRecordRepository.findByCheckInTimeBetween(start, end);
    }

    public AttendanceRecord recordAttendance(UUID userId, OffsetDateTime checkInTime, String status, String method) {
        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty()) {
            throw new RuntimeException("User not found");
        }
        AttendanceRecord record = new AttendanceRecord();
        record.setUser(user.get());
        record.setCheckInTime(checkInTime);
        record.setStatus(status);
        record.setMethod(method);
        return attendanceRecordRepository.save(record);
    }
}
