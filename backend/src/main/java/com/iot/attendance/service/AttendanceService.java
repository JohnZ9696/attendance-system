package com.iot.attendance.service;

import com.iot.attendance.entity.AttendanceRecord;
import com.iot.attendance.entity.User;
import com.iot.attendance.repository.AttendanceRecordRepository;
import com.iot.attendance.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AttendanceService {
    @Autowired
    private AttendanceRecordRepository attendanceRecordRepository;

    @Autowired
    private UserRepository userRepository;

    public List<AttendanceRecord> getAllRecords() {
        return attendanceRecordRepository.findAll();
    }

    public List<AttendanceRecord> getRecordsByUser(Long userId) {
        Optional<User> user = userRepository.findById(userId);
        return user.map(attendanceRecordRepository::findByUser).orElse(List.of());
    }

    public List<AttendanceRecord> getRecordsBetween(LocalDateTime start, LocalDateTime end) {
        return attendanceRecordRepository.findByCheckInTimeBetween(start, end);
    }

    public AttendanceRecord recordAttendance(Long userId, LocalDateTime checkInTime, String status, String method) {
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