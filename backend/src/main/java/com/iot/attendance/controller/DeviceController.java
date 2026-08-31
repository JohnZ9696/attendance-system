package com.iot.attendance.controller;

import com.iot.attendance.dto.RfidScanResponse;
import com.iot.attendance.service.CheckInFlowService;
import com.iot.attendance.service.SseEventService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.OffsetDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/devices")
public class DeviceController {

    private final CheckInFlowService orchestrationService;
    private final SseEventService sseEventService;

    public DeviceController(CheckInFlowService orchestrationService, SseEventService sseEventService) {
        this.orchestrationService = orchestrationService;
        this.sseEventService = sseEventService;
    }

    @PostMapping("/{deviceId}/rfid-scans")
    public ResponseEntity<?> handleScan(@PathVariable String deviceId, @RequestBody Map<String, String> payload) {
        sseEventService.publishEvent("rfid_scan", Map.of(
                "deviceId", deviceId,
                "uid", payload.getOrDefault("uid", ""),
                "scannedAt", OffsetDateTime.now().toString()
        ));

        try {
            var response = orchestrationService.handleRfidScan(deviceId, payload.get("uid"));
            
            if (response.errorCode() != null && !"ALREADY_CHECKED_IN".equals(response.errorCode()) && !"VERIFIED".equals(response.errorCode())) {
                int status = switch (response.errorCode()) {
                    case "RFID_INVALID", "STUDENT_NOT_FOUND_OR_INACTIVE" -> 404;
                    case "FACE_NOT_ENROLLED", "MULTIPLE_FACES",
                         "CAMERA_OFFLINE", "CAPTURE_TIMEOUT",
                         "LIVENESS_FAILED", "FACE_BELOW_THRESHOLD",
                         "FACE_MATCH_TIMEOUT" -> 422;
                    default -> 422;
                };
                return ResponseEntity.status(status).body(response);
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("message", e.getMessage(), "errorCode", "INVALID_REQUEST"));
        }
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