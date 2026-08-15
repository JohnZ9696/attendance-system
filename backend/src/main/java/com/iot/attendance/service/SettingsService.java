package com.iot.attendance.service;

import com.iot.attendance.entity.SystemSetting;
import com.iot.attendance.repository.SystemSettingsRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SettingsService {
    private final SystemSettingsRepository repository;

    /**
     * Creates a settings service backed by the specified repository.
     *
     * @param repository the repository used to access system settings
     */
    public SettingsService(SystemSettingsRepository repository) {
        this.repository = repository;
    }

    /**
     * Retrieves all system settings.
     *
     * @return all persisted system settings
     */
    public List<SystemSetting> getAllSettings() {
        return repository.findAll();
    }

    /**
     * Creates or updates a system setting with the specified key and value.
     *
     * @param key   the setting key
     * @param value the setting value
     * @return the persisted system setting
     */
    public SystemSetting updateSetting(String key, String value) {
        SystemSetting setting = repository.findById(key)
                .orElse(new SystemSetting());
        setting.setKey(key);
        setting.setValue(value);
        return repository.save(setting);
    }
}
