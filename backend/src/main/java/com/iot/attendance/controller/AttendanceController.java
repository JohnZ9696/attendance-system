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

    /**
     * Creates an attendance controller using the specified repository.
     *
     * @param repository the repository used to access attendance records
     */
    public AttendanceController(AttendanceLogRepository repository) {
        this.repository = repository;
    }

    /**
     * Retrieves all attendance records.
     *
     * @return all attendance records with HTTP status 200
     */
    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(repository.findAll());
    }

    /**
     * Retrieves attendance records for today.
     *
     * @return all attendance records with an HTTP 200 status
     */
    @GetMapping("/today")
    public ResponseEntity<?> getToday() {
        return ResponseEntity.ok(repository.findAll());
    }

    /**
     * Provides attendance statistics.
     *
     * @return an empty successful response
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        return ResponseEntity.ok().build();
    }
}
