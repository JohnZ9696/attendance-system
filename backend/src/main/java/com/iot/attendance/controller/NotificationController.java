package com.iot.attendance.controller;

import com.iot.attendance.service.OledNotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for OLED notifications.
 *
 * POST  /api/v1/notifications          – admin frontend enqueues a message
 * GET   /api/v1/notifications/pending  – ESP32 polls and consumes the message
 */
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final OledNotificationService oledNotificationService;

    public NotificationController(OledNotificationService oledNotificationService) {
        this.oledNotificationService = oledNotificationService;
    }

    /**
     * Accept a notification message from the admin frontend.
     * Body: { "message": "..." }
     */
    @PostMapping
    public ResponseEntity<Map<String, String>> sendNotification(
            @RequestBody Map<String, String> body) {

        String message = body.get("message");
        if (message == null || message.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "message must not be blank"));
        }
        if (message.length() > 128) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "message must not exceed 128 characters"));
        }

        oledNotificationService.enqueue(message.trim());
        return ResponseEntity.ok(Map.of("status", "QUEUED", "message", message.trim()));
    }

    /**
     * ESP32 polls this endpoint to consume the pending OLED message.
     * Returns 200 with the message if one is pending, or 204 (No Content) if not.
     */
    @GetMapping("/pending")
    public ResponseEntity<Map<String, String>> getPending() {
        String message = oledNotificationService.consume();
        if (message == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(Map.of("message", message));
    }
}
