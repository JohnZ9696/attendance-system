package com.iot.attendance.controller;

import com.iot.attendance.service.RfidEnrollmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rfid-enrollment")
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
    public ResponseEntity<RfidEnrollmentService.EnrollmentState> submitScan(@RequestParam String uid) {
        if (uid.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(enrollmentService.submit(uid));
    }
}
