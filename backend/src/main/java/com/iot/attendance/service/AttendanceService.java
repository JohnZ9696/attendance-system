package com.iot.attendance.service;

import com.iot.attendance.entity.AttendanceLog;
import com.iot.attendance.entity.Student;
import com.iot.attendance.entity.VerificationLog;
import com.iot.attendance.repository.AttendanceLogRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class AttendanceService {

    private final AttendanceLogRepository attendanceLogRepository;
    private final SettingsService settingsService;

    public AttendanceService(
            AttendanceLogRepository attendanceLogRepository,
            SettingsService settingsService
    ) {
        this.attendanceLogRepository = attendanceLogRepository;
        this.settingsService = settingsService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AttendanceLog recordAttendance(
            Student student,
            VerificationLog verificationLog
    ) {
        ZoneId zoneId = ZoneId.of("Asia/Ho_Chi_Minh");
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        LocalDate date = now.toLocalDate();
        LocalTime time = now.toLocalTime();

        if (attendanceLogRepository
                .findByStudentIdAndAttendanceDate(student.getId(), date)
                .isPresent()) {
            throw new RuntimeException("ALREADY_CHECKED_IN");
        }

        String cutoffStr = String.valueOf(
                settingsService.getAllSettings()
                        .get(SettingsService.CUTOFF_TIME_KEY)
        );

        LocalTime cutoff;
        try {
            cutoff = LocalTime.parse(cutoffStr);
        } catch (Exception e) {
            cutoff = LocalTime.of(7, 30);
        }

        String status = "ON_TIME";
        int lateMinutes = 0;

        if (time.isAfter(cutoff)) {
            status = "LATE";
            lateMinutes = (int) Math.ceil(
                    ChronoUnit.SECONDS.between(cutoff, time) / 60.0
            );
        }

        AttendanceLog log = new AttendanceLog();
        log.setStudent(student);
        log.setVerification(verificationLog);
        log.setAttendanceDate(date);
        log.setCheckTime(OffsetDateTime.now());
        log.setStatus(status);
        log.setLateMinutes(lateMinutes);
        log.setMethod("RFID_FACE");

        try {
            return attendanceLogRepository.saveAndFlush(log);
        } catch (DataIntegrityViolationException e) {
            throw new RuntimeException("ATTENDANCE_SAVE_FAILED", e);
        }
    }
}