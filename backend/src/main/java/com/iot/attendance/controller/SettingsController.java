package com.iot.attendance.controller;

import com.iot.attendance.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService service;

    @GetMapping
    public ResponseEntity<?> getSettings() {
        return ResponseEntity.ok(service.getAllSettings());
    }

    @PatchMapping
    public ResponseEntity<Map<String, Object>> updateSettings(@RequestBody Map<String, Object> payload) {
        return ResponseEntity.ok(service.updateSettings(payload));
    }
}
