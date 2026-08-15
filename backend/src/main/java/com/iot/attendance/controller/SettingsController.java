package com.iot.attendance.controller;

import com.iot.attendance.dto.AttendanceSettings;
import com.iot.attendance.service.SystemSettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/settings")
public class SettingsController {

    private final SystemSettingsService service;

    public SettingsController(SystemSettingsService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<AttendanceSettings> getSettings() {
        return ResponseEntity.ok(service.getAttendanceSettings());
    }

    @PatchMapping
    public ResponseEntity<AttendanceSettings> updateSettings(@RequestBody AttendanceSettings payload) {
        return ResponseEntity.ok(service.updateAttendanceSettings(payload));
    }
}
