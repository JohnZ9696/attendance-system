package com.iot.attendance.controller;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.time.format.DateTimeFormatter;

import com.iot.attendance.repository.AttendanceLogRepository;
import com.iot.attendance.entity.AttendanceLog;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.time.LocalDate;
import java.time.ZoneId;
import com.iot.attendance.repository.StudentRepository;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final AttendanceLogRepository attendanceLogRepository;
    private final StudentRepository studentRepository;

    public ReportController(AttendanceLogRepository attendanceLogRepository, StudentRepository studentRepository) {
        this.attendanceLogRepository = attendanceLogRepository;
        this.studentRepository = studentRepository;
    }

    @GetMapping("/attendance.xlsx")
    public ResponseEntity<byte[]> exportExcel() throws IOException {
        List<AttendanceLog> logs = attendanceLogRepository.findAllByOrderByCheckTimeDesc();

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Attendance");
            
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Date");
            header.createCell(1).setCellValue("Time");
            header.createCell(2).setCellValue("Student Name");
            header.createCell(3).setCellValue("MSSV");
            header.createCell(4).setCellValue("Status");
            header.createCell(5).setCellValue("Late Minutes");

            int rowIdx = 1;
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
            for (AttendanceLog log : logs) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(log.getAttendanceDate() != null ? log.getAttendanceDate().toString() : "");
                row.createCell(1).setCellValue(log.getCheckTime() != null ? log.getCheckTime().format(timeFormatter) : "");
                row.createCell(2).setCellValue(log.getStudent() != null ? log.getStudent().getFullName() : "");
                row.createCell(3).setCellValue(log.getStudent() != null ? log.getStudent().getMssv() : "");
                row.createCell(4).setCellValue(log.getStatus() != null ? log.getStatus() : "");
                row.createCell(5).setCellValue(log.getLateMinutes() != null ? log.getLateMinutes() : 0);
            }

            workbook.write(out);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", "attendance.xlsx");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(out.toByteArray());
                    
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getMetrics() {
        long totalStudents = studentRepository.count();
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        List<AttendanceLog> todayLogs = attendanceLogRepository.findAllByAttendanceDateOrderByCheckTimeDesc(today);
        
        long onTimeCount = todayLogs.stream().filter(log -> "ON_TIME".equals(log.getStatus())).count();
        long lateCount = todayLogs.stream().filter(log -> "LATE".equals(log.getStatus())).count();
        long presentCount = onTimeCount + lateCount;
        
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            List<AttendanceLog> dayLogs = attendanceLogRepository.findAllByAttendanceDateOrderByCheckTimeDesc(d);
            long o = dayLogs.stream().filter(log -> "ON_TIME".equals(log.getStatus())).count();
            long l = dayLogs.stream().filter(log -> "LATE".equals(log.getStatus())).count();
            Map<String, Object> dm = new HashMap<>();
            dm.put("date", d.toString());
            dm.put("onTime", o);
            dm.put("late", l);
            trend.add(dm);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalStudents", totalStudents);
        result.put("presentToday", presentCount);
        result.put("onTimeToday", onTimeCount);
        result.put("lateToday", lateCount);
        result.put("trend", trend);

        return ResponseEntity.ok(result);
    }
}