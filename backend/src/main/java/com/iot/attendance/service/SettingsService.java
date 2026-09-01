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

    public static final String CUTOFF_TIME_KEY = "attendance.classStartTime";
    public static final String SIMILARITY_THRESHOLD_KEY = "face.similarityThresholdPercent";
    public static final String DAY_OFFS_KEY = "attendance.dayOffs";
    public static final String WEEKLY_DAY_OFFS_KEY = "attendance.weeklyDayOffs";

    private static final String DEFAULT_CUTOFF_TIME = "07:30:00";
    public static final int MIN_SIMILARITY_THRESHOLD_PERCENT = 30;
    public static final int MAX_SIMILARITY_THRESHOLD_PERCENT = 100;

    private static final int DEFAULT_SIMILARITY_THRESHOLD_PERCENT = 30;
    private static final String DEFAULT_WEEKLY_DAY_OFFS = "0,6";

    private final SystemSettingsRepository systemSettingsRepository;

    public SettingsService(SystemSettingsRepository systemSettingsRepository) {
        this.systemSettingsRepository = systemSettingsRepository;
    }

    public Map<String, Object> getAllSettings() {
        Map<String, Object> settings = new LinkedHashMap<>();
        for (SystemSetting setting : systemSettingsRepository.findAll()) {
            Object val = setting.getValue();
            if (setting.getKey().equals(DAY_OFFS_KEY)) {
                val = parseCsv(setting.getValue());
            } else if (setting.getKey().equals(WEEKLY_DAY_OFFS_KEY)) {
                val = parseIntList(setting.getValue());
            } else if (setting.getKey().equals(SIMILARITY_THRESHOLD_KEY)) {
                val = parseSimilarityThreshold(setting.getValue());
            }
            settings.put(setting.getKey(), val);
        }
        
        settings.putIfAbsent(CUTOFF_TIME_KEY, DEFAULT_CUTOFF_TIME);
        settings.putIfAbsent(SIMILARITY_THRESHOLD_KEY, DEFAULT_SIMILARITY_THRESHOLD_PERCENT);
        settings.putIfAbsent(DAY_OFFS_KEY, List.of());
        settings.putIfAbsent(WEEKLY_DAY_OFFS_KEY, parseCsv(DEFAULT_WEEKLY_DAY_OFFS).stream().map(Integer::parseInt).toList());
        
        return settings;
    }

    @Transactional
    public Map<String, Object> updateSettings(Map<String, Object> updates) {
        updates.forEach((key, value) -> {
            if (SIMILARITY_THRESHOLD_KEY.equals(key)) {
                save(key, String.valueOf(parseSimilarityThreshold(value)));
            } else if (value instanceof List<?> list) {
                save(key, list.stream().map(Object::toString).collect(Collectors.joining(",")));
            } else {
                save(key, String.valueOf(value));
            }
        });

        return getAllSettings();
    }

    public int getSimilarityThresholdPercent() {
        return systemSettingsRepository.findById(SIMILARITY_THRESHOLD_KEY)
                .map(SystemSetting::getValue)
                .map(SettingsService::parseSimilarityThreshold)
                .orElse(DEFAULT_SIMILARITY_THRESHOLD_PERCENT);
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

    private static int parseSimilarityThreshold(Object value) {
        final int threshold;
        try {
            threshold = Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("FACE_SIMILARITY_THRESHOLD_MUST_BE_AN_INTEGER");
        }

        if (threshold < MIN_SIMILARITY_THRESHOLD_PERCENT
                || threshold > MAX_SIMILARITY_THRESHOLD_PERCENT) {
            throw new IllegalArgumentException("FACE_SIMILARITY_THRESHOLD_OUT_OF_RANGE");
        }
        return threshold;
    }
}
