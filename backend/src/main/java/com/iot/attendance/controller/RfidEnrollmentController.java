package com.iot.attendance.controller;

import com.iot.attendance.service.RfidEnrollmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/rfid-enrollment")
public class RfidEnrollmentController {
    private final RfidEnrollmentService enrollmentService;

    public RfidEnrollmentController(RfidEnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @PostMapping("/start")
    public ResponseEntity<RfidEnrollmentService.EnrollmentState> start() {
        return ResponseEntity.ok(enrollmentService.start());
    }

    @GetMapping
    public ResponseEntity<RfidEnrollmentService.EnrollmentState> getState() {
        return ResponseEntity.ok(enrollmentService.getState());
    }

    @PostMapping("/cancel")
    public ResponseEntity<RfidEnrollmentService.EnrollmentState> cancel() {
        return ResponseEntity.ok(enrollmentService.cancel());
    }

    @PostMapping("/scan")
    public ResponseEntity<RfidEnrollmentService.EnrollmentState> submitScan(@RequestBody ScanRequest request) {
        if (request.uid() == null || request.uid().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(enrollmentService.submit(request.uid()));
    }

    public record ScanRequest(String uid) {}
}
