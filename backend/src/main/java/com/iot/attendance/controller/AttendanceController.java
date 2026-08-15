package com.iot.attendance.controller;

import com.iot.attendance.repository.AttendanceLogRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/attendance")
public class AttendanceController {

    private final AttendanceLogRepository repository;

    public AttendanceController(AttendanceLogRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(repository.findAll());
    }

    @GetMapping("/today")
    public ResponseEntity<?> getToday() {
        return ResponseEntity.ok(repository.findAll());
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        return ResponseEntity.ok().build();
    }
}
