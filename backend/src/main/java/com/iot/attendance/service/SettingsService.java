package com.iot.attendance.service;

import com.iot.attendance.entity.SystemSetting;
import com.iot.attendance.repository.SystemSettingsRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SettingsService {
    private final SystemSettingsRepository repository;

    public SettingsService(SystemSettingsRepository repository) {
        this.repository = repository;
    }

    public List<SystemSetting> getAllSettings() {
        return repository.findAll();
    }

    public SystemSetting updateSetting(String key, String value) {
        SystemSetting setting = repository.findById(key)
                .orElse(new SystemSetting());
        setting.setKey(key);
        setting.setValue(value);
        return repository.save(setting);
    }
}
