package com.iot.attendance.service;

import com.iot.attendance.entity.SystemSetting;
import com.iot.attendance.repository.SystemSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SettingsService {

    private final SystemSettingsRepository systemSettingsRepository;

    public Map<String, String> getAllSettings() {
        Map<String, String> settingsMap = new HashMap<>();
        for (SystemSetting setting : systemSettingsRepository.findAll()) {
            settingsMap.put(setting.getKey(), setting.getValue());
        }
        return settingsMap;
    }

    @Transactional
    public Map<String, String> updateSettings(Map<String, String> updates) {
        for (Map.Entry<String, String> entry : updates.entrySet()) {
            SystemSetting setting = systemSettingsRepository.findById(entry.getKey()).orElse(new SystemSetting());
            setting.setKey(entry.getKey());
            setting.setValue(entry.getValue());
            systemSettingsRepository.save(setting);
        }
        return getAllSettings();
    }
}
