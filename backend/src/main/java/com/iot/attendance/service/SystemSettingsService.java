package com.iot.attendance.service;

import com.iot.attendance.dto.AttendanceSettings;
import com.iot.attendance.entity.SystemSetting;
import com.iot.attendance.repository.SystemSettingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Service
public class SystemSettingsService {
    public static final String START_TIME_KEY = "attendance.classStartTime";
    public static final String GRACE_MINUTES_KEY = "attendance.lateGraceMinutes";
    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    @Autowired
    private SystemSettingsRepository repository;

    public AttendanceSettings getAttendanceSettings() {
        AttendanceSettings settings = new AttendanceSettings();
        settings.setClassStartTime(LocalTime.parse(valueOf(START_TIME_KEY, "07:30"), HH_MM));
        settings.setLateGraceMinutes(intValueOf(GRACE_MINUTES_KEY, 15));
        return settings;
    }

    public AttendanceSettings updateAttendanceSettings(AttendanceSettings settings) {
        save(START_TIME_KEY, settings.getClassStartTime() == null ? "07:30" : settings.getClassStartTime().format(HH_MM));
        save(GRACE_MINUTES_KEY, String.valueOf(settings.getLateGraceMinutes()));
        return getAttendanceSettings();
    }

    private void save(String key, String value) {
        SystemSetting setting = new SystemSetting();
        setting.setKey(key);
        setting.setValue(value);
        repository.save(setting);
    }

    private String valueOf(String key, String fallback) {
        return repository.findById(key).map(SystemSetting::getValue).orElse(fallback);
    }

    private int intValueOf(String key, int fallback) {
        try {
            return Integer.parseInt(valueOf(key, String.valueOf(fallback)));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}