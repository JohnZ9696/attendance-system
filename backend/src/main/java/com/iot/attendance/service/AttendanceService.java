package com.iot.attendance.service;

import com.iot.attendance.entity.AttendanceLog;
import com.iot.attendance.entity.Student;
import com.iot.attendance.entity.VerificationLog;
import com.iot.attendance.repository.AttendanceLogRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class AttendanceService {

    private final AttendanceLogRepository attendanceLogRepository;

    public AttendanceService(AttendanceLogRepository attendanceLogRepository) {
        this.attendanceLogRepository = attendanceLogRepository;
    }

    public AttendanceLog recordAttendance(Student student, VerificationLog verificationLog) {
        ZoneId zoneId = ZoneId.of("Asia/Ho_Chi_Minh");
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        LocalDate date = now.toLocalDate();
        LocalTime time = now.toLocalTime();
        
        LocalTime cutoff = LocalTime.of(7, 30, 0);
        
        String status = "ON_TIME";
        Integer lateMinutes = 0;
        
        if (time.isAfter(cutoff)) {
            status = "LATE";
            lateMinutes = (int) Math.ceil(ChronoUnit.SECONDS.between(cutoff, time) / 60.0);
        }

        AttendanceLog log = new AttendanceLog();
        log.setStudent(student);
        log.setVerification(verificationLog);
        log.setAttendanceDate(date);
        log.setCheckTime(OffsetDateTime.now());
        log.setStatus(status);
        log.setLateMinutes(lateMinutes);

        try {
            return attendanceLogRepository.save(log);
        } catch (DataIntegrityViolationException e) {
            throw new RuntimeException("ALREADY_CHECKED_IN");
        }
    }
}
