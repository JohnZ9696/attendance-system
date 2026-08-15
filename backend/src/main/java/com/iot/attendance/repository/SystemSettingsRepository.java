package com.iot.attendance.repository;

import com.iot.attendance.entity.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemSettingsRepository extends JpaRepository<SystemSetting, String> {
}