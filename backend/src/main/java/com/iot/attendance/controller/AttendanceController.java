package com.iot.attendance.controller;

import com.iot.attendance.dto.AttendanceResponse;
import com.iot.attendance.dto.AttendanceStats;
import com.iot.attendance.entity.AttendanceLog;
import com.iot.attendance.repository.AttendanceLogRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/attendance")
public class AttendanceController {

    private final AttendanceLogRepository repository;

    public AttendanceController(AttendanceLogRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public ResponseEntity<List<AttendanceResponse>> getAll(
            @RequestParam(required = false) String studentId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to
    ) {
        List<AttendanceLog> logs;
        if (studentId != null) {
            logs = repository.findByStudentIdOrderByCheckTimeDesc(java.util.UUID.fromString(studentId));
        } else if (from != null && to != null) {
            logs = repository.findByAttendanceDateBetweenOrderByCheckTimeDesc(
                    LocalDate.parse(from), LocalDate.parse(to));
        } else {
            logs = repository.findAllByOrderByCheckTimeDesc();
        }
        return ResponseEntity.ok(logs.stream().map(AttendanceResponse::from).collect(Collectors.toList()));
    }

    @GetMapping("/today")
    public ResponseEntity<List<AttendanceResponse>> getToday() {
        LocalDate today = LocalDate.now();
        List<AttendanceLog> logs = repository.findByAttendanceDateOrderByCheckTimeDesc(today);
        return ResponseEntity.ok(logs.stream().map(AttendanceResponse::from).collect(Collectors.toList()));
    }

    @GetMapping("/stats")
    public ResponseEntity<AttendanceStats> getStats() {
        List<AttendanceLog> logs = repository.findAll();
        long total = logs.size();
        long onTime = logs.stream().filter(l -> "ON_TIME".equals(l.getStatus())).count();
        long late = logs.stream().filter(l -> "LATE".equals(l.getStatus())).count();
        long uniqueStudents = logs.stream().map(l -> l.getStudent().getId()).distinct().count();
        return ResponseEntity.ok(new AttendanceStats(total, onTime, late, uniqueStudents));
    }
}
