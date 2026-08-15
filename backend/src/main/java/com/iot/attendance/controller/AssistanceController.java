package com.iot.attendance.controller;

import com.iot.attendance.service.AssistanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/assistance")
public class AssistanceController {

    private final AssistanceService service;

    /**
     * Creates a controller backed by the specified assistance service.
     *
     * @param service the service used to process assistance operations
     */
    public AssistanceController(AssistanceService service) {
        this.service = service;
    }

    /**
     * Retrieves all assistance records.
     *
     * @return the assistance records with HTTP status 200
     */
    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    /**
     * Updates the status of an assistance request.
     *
     * @param id   the assistance request identifier
     * @param body the request body containing the new {@code status}
     * @return the updated assistance request
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(service.updateStatus(id, body.get("status")));
    }
}
