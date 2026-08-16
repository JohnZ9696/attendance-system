package com.iot.attendance.controller;

import com.iot.attendance.service.CheckInOrchestrationService;
import com.iot.attendance.service.SseEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.OffsetDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final CheckInOrchestrationService orchestrationService;
    private final SseEventService sseEventService;

    @PostMapping("/{deviceId}/rfid-scans")
    public ResponseEntity<?> handleScan(@PathVariable String deviceId, @RequestBody Map<String, String> payload) {
        sseEventService.publishEvent("rfid_scan", Map.of(
                "deviceId", deviceId,
                "uid", payload.getOrDefault("uid", ""),
                "scannedAt", OffsetDateTime.now().toString()
        ));

        orchestrationService.handleRfidScan(deviceId, payload.get("uid"));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{deviceId}/heartbeat")
    public ResponseEntity<?> heartbeat(@PathVariable String deviceId) {
        sseEventService.publishEvent("device_status", Map.of(
                "deviceId", deviceId,
                "status", "ONLINE",
                "lastHeartbeat", OffsetDateTime.now().toString()
        ));

        return ResponseEntity.ok().build();
    }

    @PostMapping("/{deviceId}/incidents")
    public ResponseEntity<?> incidents(@PathVariable String deviceId) {
        return ResponseEntity.ok().build();
    }

    public record ScanRequest(String uid) {}
}
