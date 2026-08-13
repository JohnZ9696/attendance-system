package com.iot.attendance.controller;

import com.iot.attendance.entity.AttendanceRecord;
import com.iot.attendance.service.AttendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
@CrossOrigin(origins = "*")
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    @GetMapping
    public ResponseEntity<List<AttendanceRecord>> getAllRecords() {
        return ResponseEntity.ok(attendanceService.getAllRecords());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AttendanceRecord>> getRecordsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(attendanceService.getRecordsByUser(userId));
    }

    @GetMapping("/between")
    public ResponseEntity<List<AttendanceRecord>> getRecordsBetween(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(attendanceService.getRecordsBetween(start, end));
    }

    @PostMapping
    public ResponseEntity<AttendanceRecord> recordAttendance(
            @RequestParam Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime checkInTime,
            @RequestParam String status,
            @RequestParam String method) {
        return ResponseEntity.ok(attendanceService.recordAttendance(userId, checkInTime, status, method));
    }
}