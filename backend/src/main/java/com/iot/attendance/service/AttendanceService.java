package com.iot.attendance.service;

import com.iot.attendance.dto.AttendanceSettings;
import com.iot.attendance.entity.AttendanceRecord;
import com.iot.attendance.entity.User;
import com.iot.attendance.repository.AttendanceRecordRepository;
import com.iot.attendance.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AttendanceService {
    private static final ZoneId SCHOOL_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Autowired
    private AttendanceRecordRepository attendanceRecordRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SystemSettingsService systemSettingsService;

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
        if ("IN".equals(status)) {
            AttendanceRecord existing = findExistingOnDay(user.get(), checkInTime);
            if (existing != null) {
                return existing;
            }
        }
        AttendanceRecord record = new AttendanceRecord();
        record.setUser(user.get());
        record.setCheckInTime(checkInTime);
        record.setStatus("IN".equals(status) && isLate(checkInTime) ? "LATE" : status);
        record.setMethod(method);
        return attendanceRecordRepository.save(record);
    }

    public boolean deleteAttendanceRecord(UUID id) {
        if (!attendanceRecordRepository.existsById(id)) {
            return false;
        }
        attendanceRecordRepository.deleteById(id);
        return true;
    }

    private AttendanceRecord findExistingOnDay(User user, OffsetDateTime checkInTime) {
        LocalDate localDay = checkInTime.atZoneSameInstant(SCHOOL_ZONE).toLocalDate();
        OffsetDateTime dayStart = localDay.atStartOfDay().atZone(SCHOOL_ZONE).toOffsetDateTime();
        OffsetDateTime dayEnd = localDay.plusDays(1).atStartOfDay().atZone(SCHOOL_ZONE).toOffsetDateTime();
        return attendanceRecordRepository.findByUserAndCheckInTimeBetween(user, dayStart, dayEnd)
                .stream().findFirst().orElse(null);
    }

    private boolean isLate(OffsetDateTime checkInTime) {
        AttendanceSettings settings = systemSettingsService.getAttendanceSettings();
        LocalTime threshold = settings.getClassStartTime().plusMinutes(settings.getLateGraceMinutes());
        LocalTime localTime = checkInTime.atZoneSameInstant(SCHOOL_ZONE).toLocalTime();
        return localTime.isAfter(threshold);
    }
}
