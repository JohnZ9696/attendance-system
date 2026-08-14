package com.iot.attendance.controller;

import com.iot.attendance.entity.AttendanceRecord;
import com.iot.attendance.service.AttendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    @GetMapping
    public ResponseEntity<List<AttendanceRecord>> getAllRecords() {
        return ResponseEntity.ok(attendanceService.getAllRecords());
    }

    @GetMapping("/today")
    public ResponseEntity<List<AttendanceRecord>> getTodayRecords() {
        return ResponseEntity.ok(attendanceService.getTodayRecords());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AttendanceRecord>> getRecordsByUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(attendanceService.getRecordsByUser(userId));
    }

    @GetMapping("/between")
    public ResponseEntity<List<AttendanceRecord>> getRecordsBetween(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime end) {
        return ResponseEntity.ok(attendanceService.getRecordsBetween(start, end));
    }

    @PostMapping
    public ResponseEntity<AttendanceRecord> recordAttendance(
            @RequestParam UUID userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime checkInTime,
            @RequestParam String status,
            @RequestParam String method) {
        return ResponseEntity.ok(attendanceService.recordAttendance(userId, checkInTime, status, method));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAttendanceRecord(@PathVariable UUID id) {
        if (!attendanceService.deleteAttendanceRecord(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }
}
