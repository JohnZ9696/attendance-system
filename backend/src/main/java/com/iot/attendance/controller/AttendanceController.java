package com.iot.attendance.controller;

import com.iot.attendance.entity.AttendanceLog;
import com.iot.attendance.repository.AttendanceLogRepository;
import com.iot.attendance.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceLogRepository attendanceLogRepository;
    private final StudentRepository studentRepository;

    @GetMapping
    public ResponseEntity<?> getAttendance(
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) UUID studentId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
            
        Pageable pageable = PageRequest.of(page, size);
        Page<AttendanceLog> result;

        if (date != null && studentId != null && status != null) {
            result = attendanceLogRepository.findByAttendanceDateAndStudentIdAndStatus(date, studentId, status, pageable);
        } else if (date != null && studentId != null) {
            result = attendanceLogRepository.findByAttendanceDateAndStudentId(date, studentId, pageable);
        } else if (date != null && status != null) {
            result = attendanceLogRepository.findByAttendanceDateAndStatus(date, status, pageable);
        } else if (studentId != null && status != null) {
            result = attendanceLogRepository.findByStudentIdAndStatus(studentId, status, pageable);
        } else if (date != null) {
            result = attendanceLogRepository.findByAttendanceDate(date, pageable);
        } else if (studentId != null) {
            result = attendanceLogRepository.findByStudentId(studentId, pageable);
        } else if (status != null) {
            result = attendanceLogRepository.findByStatus(status, pageable);
        } else {
            result = attendanceLogRepository.findAll(pageable);
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/today")
    public ResponseEntity<?> getTodayAttendance() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        List<AttendanceLog> todayLogs = attendanceLogRepository.findByAttendanceDate(today);
        return ResponseEntity.ok(todayLogs);
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        List<AttendanceLog> todayLogs = attendanceLogRepository.findByAttendanceDate(today);
        
        long totalStudents = studentRepository.findAll().stream().filter(s -> s.getIsActive()).count();
        long checkedInToday = todayLogs.size();
        long lateToday = todayLogs.stream().filter(log -> "LATE".equals(log.getStatus())).count();
        long notCheckedIn = totalStudents - checkedInToday;

        return ResponseEntity.ok(Map.of(
                "totalStudents", totalStudents,
                "checkedInToday", checkedInToday,
                "lateToday", lateToday,
                "notCheckedIn", Math.max(0, notCheckedIn)
        ));
    }
}
