package com.iot.attendance.service;

import com.iot.attendance.dto.AttendanceSettings;
import com.iot.attendance.entity.SystemSetting;
import com.iot.attendance.repository.SystemSettingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SystemSettingsService {
    public static final String START_TIME_KEY = "attendance.classStartTime";
    public static final String GRACE_MINUTES_KEY = "attendance.lateGraceMinutes";
    public static final String DAY_OFFS_KEY = "attendance.dayOffs";
    public static final String WEEKLY_DAY_OFFS_KEY = "attendance.weeklyDayOffs";
    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    @Autowired
    private SystemSettingsRepository repository;

    public AttendanceSettings getAttendanceSettings() {
        AttendanceSettings settings = new AttendanceSettings();
        settings.setClassStartTime(LocalTime.parse(valueOf(START_TIME_KEY, "07:30"), HH_MM));
        settings.setLateGraceMinutes(intValueOf(GRACE_MINUTES_KEY, 15));
        settings.setDayOffs(parseDayOffs(valueOf(DAY_OFFS_KEY, "")));
        settings.setWeeklyDayOffs(parseWeeklyDayOffs(valueOf(WEEKLY_DAY_OFFS_KEY, "0,6")));
        return settings;
    }

    public AttendanceSettings updateAttendanceSettings(AttendanceSettings settings) {
        save(START_TIME_KEY, settings.getClassStartTime() == null ? "07:30" : settings.getClassStartTime().format(HH_MM));
        save(GRACE_MINUTES_KEY, String.valueOf(settings.getLateGraceMinutes()));
        save(DAY_OFFS_KEY, serializeDayOffs(settings.getDayOffs()));
        save(WEEKLY_DAY_OFFS_KEY, serializeWeeklyDayOffs(settings.getWeeklyDayOffs()));
        return getAttendanceSettings();
    }

    private List<LocalDate> parseDayOffs(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(LocalDate::parse)
                .sorted()
                .collect(Collectors.toList());
    }

    private String serializeDayOffs(List<LocalDate> dayOffs) {
        if (dayOffs == null) {
            return "";
        }
        return dayOffs.stream().map(LocalDate::toString).sorted().collect(Collectors.joining(","));
    }

    private List<Integer> parseWeeklyDayOffs(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(Integer::parseInt)
                .sorted()
                .collect(Collectors.toList());
    }

    private String serializeWeeklyDayOffs(List<Integer> weeklyDayOffs) {
        if (weeklyDayOffs == null) {
            return "";
        }
        return weeklyDayOffs.stream().map(String::valueOf).sorted().collect(Collectors.joining(","));
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