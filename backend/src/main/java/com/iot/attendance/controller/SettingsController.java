package com.iot.attendance.controller;

import com.iot.attendance.service.SettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/settings")
public class SettingsController {

    private final SettingsService service;

    /**
     * Creates a controller backed by the specified settings service.
     *
     * @param service the service used to retrieve and update settings
     */
    public SettingsController(SettingsService service) {
        this.service = service;
    }

    /**
     * Retrieves all application settings.
     *
     * @return the settings with an HTTP 200 status
     */
    @GetMapping
    public ResponseEntity<?> getSettings() {
        return ResponseEntity.ok(service.getAllSettings());
    }

    /**
     * Updates the application settings provided in the request payload.
     *
     * @param payload setting names and their new values
     * @return an empty successful response
     */
    @PatchMapping
    public ResponseEntity<?> updateSettings(@RequestBody Map<String, String> payload) {
        payload.forEach(service::updateSetting);
        return ResponseEntity.ok().build();
    }
}
