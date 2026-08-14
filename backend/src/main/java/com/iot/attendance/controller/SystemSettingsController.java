package com.iot.attendance.controller;

import com.iot.attendance.dto.AttendanceSettings;
import com.iot.attendance.service.SystemSettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings")
public class SystemSettingsController {
    @Autowired
    private SystemSettingsService systemSettingsService;

    @GetMapping
    public AttendanceSettings getAttendanceSettings() {
        return systemSettingsService.getAttendanceSettings();
    }

    @PutMapping
    public AttendanceSettings updateAttendanceSettings(@RequestBody AttendanceSettings settings) {
        return systemSettingsService.updateAttendanceSettings(settings);
    }
}