package com.iot.attendance.controller;

import com.iot.attendance.dto.AttendanceResponse;
import com.iot.attendance.dto.AttendanceStats;
import com.iot.attendance.entity.AttendanceLog;
import com.iot.attendance.repository.AttendanceLogRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/attendance")
public class AttendanceController {

    private final AttendanceLogRepository repository;

    public AttendanceController(AttendanceLogRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public ResponseEntity<List<AttendanceResponse>> getAll(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,

            @RequestParam(required = false)
            UUID studentId
    ) {
        List<AttendanceLog> logs;

        if (date != null && studentId != null) {
            logs = repository
                    .findAllByStudentIdAndAttendanceDateOrderByCheckTimeDesc(
                            studentId,
                            date
                    );
        } else if (date != null) {
            logs = repository
                    .findAllByAttendanceDateOrderByCheckTimeDesc(date);
        } else if (studentId != null) {
            logs = repository
                    .findAllByStudentIdOrderByCheckTimeDesc(studentId);
        } else {
            logs = repository.findAllByOrderByCheckTimeDesc();
        }

        return ResponseEntity.ok(
                logs.stream()
                        .map(AttendanceResponse::from)
                        .toList()
        );
    }

    @GetMapping("/today")
    public ResponseEntity<List<AttendanceResponse>> getToday() {
        List<AttendanceResponse> result = repository
                .findAllByAttendanceDateOrderByCheckTimeDesc(
                        LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"))
                )
                .stream()
                .map(AttendanceResponse::from)
                .toList();

        return ResponseEntity.ok(result);
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