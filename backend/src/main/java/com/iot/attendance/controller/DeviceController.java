package com.iot.attendance.controller;

import com.iot.attendance.service.CheckInOrchestrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/devices")
public class DeviceController {

    private final CheckInOrchestrationService orchestrationService;

    /**
     * Creates a controller backed by the specified orchestration service.
     *
     * @param orchestrationService service used to process device events
     */
    public DeviceController(CheckInOrchestrationService orchestrationService) {
        this.orchestrationService = orchestrationService;
    }

    /**
     * Processes an RFID scan reported by a device.
     *
     * @param deviceId the identifier of the device reporting the scan
     * @param payload  the request payload containing the RFID tag UID
     * @return an HTTP 200 response with an empty body
     */
    @PostMapping("/{deviceId}/rfid-scans")
    public ResponseEntity<?> handleScan(@PathVariable String deviceId, @RequestBody Map<String, String> payload) {
        orchestrationService.handleRfidScan(deviceId, payload.get("uid"));
        return ResponseEntity.ok().build();
    }

    /**
     * Acknowledges a heartbeat from a device.
     *
     * @param deviceId the identifier of the device sending the heartbeat
     * @return an empty successful response
     */
    @PostMapping("/{deviceId}/heartbeat")
    public ResponseEntity<?> heartbeat(@PathVariable String deviceId) {
        return ResponseEntity.ok().build();
    }

    /**
     * Acknowledges an incident reported by a device.
     *
     * @param deviceId the identifier of the reporting device
     * @return an empty successful response
     */
    @PostMapping("/{deviceId}/incidents")
    public ResponseEntity<?> incidents(@PathVariable String deviceId) {
        return ResponseEntity.ok().build();
    }
}
