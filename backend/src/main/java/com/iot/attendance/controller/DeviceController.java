package com.iot.attendance.controller;

import com.iot.attendance.entity.AssistanceRequest;
import com.iot.attendance.repository.AssistanceRequestRepository;
import com.iot.attendance.service.CheckInOrchestrationService;
import com.iot.attendance.service.SseEventService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final CheckInOrchestrationService orchestrationService;
    private final SseEventService sseEventService;
    private final AssistanceRequestRepository assistanceRequestRepository;
    
    private final Map<String, OffsetDateTime> deviceHeartbeats = new ConcurrentHashMap<>();

    @PostMapping("/{deviceId}/rfid-scans")
    public ResponseEntity<?> handleRfidScan(@PathVariable String deviceId, @RequestBody RfidScanRequest request) {
        Map<String, Object> result = orchestrationService.handleRfidScan(deviceId, request.getUid());
        
        sseEventService.publishEvent("rfid_scan", Map.of(
                "deviceId", deviceId,
                "uid", request.getUid(),
                "result", result
        ));
        
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{deviceId}/heartbeat")
    public ResponseEntity<?> handleHeartbeat(@PathVariable String deviceId) {
        deviceHeartbeats.put(deviceId, OffsetDateTime.now());
        sseEventService.publishEvent("device_status", Map.of(
                "deviceId", deviceId,
                "status", "ONLINE",
                "timestamp", OffsetDateTime.now().toString()
        ));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{deviceId}/incidents")
    public ResponseEntity<?> handleIncident(@PathVariable String deviceId, @RequestBody IncidentRequest request) {
        AssistanceRequest assistanceRequest = new AssistanceRequest();
        assistanceRequest.setSource("PUSH_BUTTON");
        assistanceRequest.setMessage(request.getType() != null ? request.getType() : "Emergency Push Button");
        assistanceRequest.setStatus("OPEN");
        assistanceRequestRepository.save(assistanceRequest);
        
        sseEventService.publishEvent("incident", Map.of(
                "deviceId", deviceId,
                "incident", assistanceRequest
        ));
        
        return ResponseEntity.ok(Map.of("resultCode", "INCIDENT_RECORDED"));
    }

    @Data
    public static class RfidScanRequest {
        private String uid;
    }

    @Data
    public static class IncidentRequest {
        private String type;
        private String source;
        private String occurredAt;
    }
}
