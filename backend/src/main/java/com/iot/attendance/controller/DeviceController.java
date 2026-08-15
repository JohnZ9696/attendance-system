package com.iot.attendance.controller;

import com.iot.attendance.dto.RfidScanResponse;
import com.iot.attendance.service.CheckInOrchestrationService;
import com.iot.attendance.service.SseEventService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/devices")
public class DeviceController {

    private final CheckInOrchestrationService orchestrationService;
    private final SseEventService sseEventService;

    public DeviceController(CheckInOrchestrationService orchestrationService, SseEventService sseEventService) {
        this.orchestrationService = orchestrationService;
        this.sseEventService = sseEventService;
    }

    @PostMapping("/{deviceId}/rfid-scans")
    public ResponseEntity<RfidScanResponse> handleScan(@PathVariable String deviceId, @RequestBody ScanRequest payload) {
        UUID verificationId = orchestrationService.handleRfidScan(deviceId, payload.uid());
        return ResponseEntity.ok(new RfidScanResponse(verificationId, "Verification started"));
    }

    @PostMapping("/{deviceId}/heartbeat")
    public ResponseEntity<Void> heartbeat(@PathVariable String deviceId) {
        sseEventService.publishEvent("device_status", Map.of(
                "deviceId", deviceId,
                "status", "ONLINE",
                "lastHeartbeat", java.time.OffsetDateTime.now().toString()
        ));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{deviceId}/incidents")
    public ResponseEntity<Void> incidents(@PathVariable String deviceId) {
        sseEventService.publishEvent("incident", Map.of(
                "deviceId", deviceId,
                "message", "Incident reported",
                "source", "DEVICE"
        ));
        return ResponseEntity.ok().build();
    }

    public record ScanRequest(String uid) {}
}
