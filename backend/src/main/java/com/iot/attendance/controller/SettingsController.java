package com.iot.attendance.controller;

import com.iot.attendance.service.SettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/settings")
public class SettingsController {

    private final SettingsService service;

    public SettingsController(SettingsService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<?> getSettings() {
        return ResponseEntity.ok(service.getAllSettings());
    }

    @PatchMapping
    public ResponseEntity<?> updateSettings(@RequestBody Map<String, String> payload) {
        payload.forEach(service::updateSetting);
        return ResponseEntity.ok().build();
    }
}
