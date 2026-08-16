package com.iot.attendance.service;

import com.iot.attendance.entity.SystemSetting;
import com.iot.attendance.repository.SystemSettingsRepository;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettingsService {

    public static final String CUTOFF_TIME_KEY = "attendance.cutoffTime";
    public static final String SIMILARITY_THRESHOLD_KEY = "face.similarityThresholdPercent";
    public static final String DAY_OFFS_KEY = "attendance.dayOffs";
    public static final String WEEKLY_DAY_OFFS_KEY = "attendance.weeklyDayOffs";

    private static final String DEFAULT_CUTOFF_TIME = "07:30:00";
    private static final String DEFAULT_SIMILARITY_THRESHOLD = "60";
    private static final String DEFAULT_WEEKLY_DAY_OFFS = "0,6";

    private final SystemSettingsRepository systemSettingsRepository;

    public SettingsService(SystemSettingsRepository systemSettingsRepository) {
        this.systemSettingsRepository = systemSettingsRepository;
    }

    public Map<String, Object> getAllSettings() {
        Map<String, String> stored = new LinkedHashMap<>();
        for (SystemSetting setting : systemSettingsRepository.findAll()) {
            stored.put(setting.getKey(), setting.getValue());
        }

        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put("attendance_cutoff_time", stored.getOrDefault(CUTOFF_TIME_KEY, DEFAULT_CUTOFF_TIME));
        settings.put("face_similarity_threshold_percent",
                Integer.parseInt(stored.getOrDefault(SIMILARITY_THRESHOLD_KEY, DEFAULT_SIMILARITY_THRESHOLD)));
        settings.put("day_offs", parseCsv(stored.get(DAY_OFFS_KEY)));
        settings.put("weekly_day_offs", parseIntList(stored.getOrDefault(WEEKLY_DAY_OFFS_KEY, DEFAULT_WEEKLY_DAY_OFFS)));
        return settings;
    }

    @Transactional
    public Map<String, Object> updateSettings(Map<String, Object> updates) {
        Object cutoffTime = updates.get("attendance_cutoff_time");
        if (cutoffTime != null) {
            save(CUTOFF_TIME_KEY, cutoffTime.toString());
        }

        Object similarityThreshold = updates.get("face_similarity_threshold_percent");
        if (similarityThreshold != null) {
            save(SIMILARITY_THRESHOLD_KEY, similarityThreshold.toString());
        }

        Object dayOffs = updates.get("day_offs");
        if (dayOffs instanceof List<?> list) {
            save(DAY_OFFS_KEY, list.stream().map(Object::toString).collect(Collectors.joining(",")));
        }

        Object weeklyDayOffs = updates.get("weekly_day_offs");
        if (weeklyDayOffs instanceof List<?> list) {
            save(WEEKLY_DAY_OFFS_KEY, list.stream().map(Object::toString).collect(Collectors.joining(",")));
        }

        return getAllSettings();
    }

    private void save(String key, String value) {
        SystemSetting setting = systemSettingsRepository.findById(key)
                .orElseGet(SystemSetting::new);
        setting.setKey(key);
        setting.setValue(value);
        systemSettingsRepository.save(setting);
    }

    private static List<String> parseCsv(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private static List<Integer> parseIntList(String value) {
        return parseCsv(value).stream()
                .map(Integer::parseInt)
                .toList();
    }
}