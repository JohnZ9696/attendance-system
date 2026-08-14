package com.iot.attendance.controller;

import com.iot.attendance.entity.AssistanceRequest;
import com.iot.attendance.service.AssistanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/assistance")
public class AssistanceController {

    @Autowired
    private AssistanceService assistanceService;

    @GetMapping
    public ResponseEntity<List<AssistanceRequest>> getAllRequests() {
        return ResponseEntity.ok(assistanceService.getAllRequests());
    }

    @PostMapping
    public ResponseEntity<AssistanceRequest> createRequest(
            @RequestParam(required = false) UUID userId,
            @RequestParam String message) {
        return ResponseEntity.ok(assistanceService.createRequest(userId, message));
    }
}
