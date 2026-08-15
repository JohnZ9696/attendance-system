package com.iot.attendance.controller;

import com.iot.attendance.service.CheckInOrchestrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/devices")
public class DeviceController {

    private final CheckInOrchestrationService orchestrationService;

    public DeviceController(CheckInOrchestrationService orchestrationService) {
        this.orchestrationService = orchestrationService;
    }

    @PostMapping("/{deviceId}/rfid-scans")
    public ResponseEntity<?> handleScan(@PathVariable String deviceId, @RequestBody Map<String, String> payload) {
        orchestrationService.handleRfidScan(deviceId, payload.get("uid"));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{deviceId}/heartbeat")
    public ResponseEntity<?> heartbeat(@PathVariable String deviceId) {
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{deviceId}/incidents")
    public ResponseEntity<?> incidents(@PathVariable String deviceId) {
        return ResponseEntity.ok().build();
    }
}
