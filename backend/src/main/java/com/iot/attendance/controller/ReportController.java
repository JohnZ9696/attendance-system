package com.iot.attendance.controller;

import com.iot.attendance.entity.AttendanceLog;
import com.iot.attendance.repository.AttendanceLogRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final AttendanceLogRepository attendanceLogRepository;

    public ReportController(AttendanceLogRepository attendanceLogRepository) {
        this.attendanceLogRepository = attendanceLogRepository;
    }

    @GetMapping("/attendance.xlsx")
    public ResponseEntity<byte[]> exportExcel() throws IOException {
        List<AttendanceLog> logs = attendanceLogRepository.findAllByOrderByCheckTimeDesc();

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Attendance");

            // Header row
            Row header = sheet.createRow(0);
            String[] headers = {"ID", "Student ID", "Student Name", "MSSV", "Attendance Date", "Check-in Time", "Status", "Late Minutes"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
            }

            // Data rows
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            int rowNum = 1;
            for (AttendanceLog log : logs) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(log.getId().toString());
                row.createCell(1).setCellValue(log.getStudent().getId().toString());
                row.createCell(2).setCellValue(log.getStudent().getFullName());
                row.createCell(3).setCellValue(log.getStudent().getMssv());
                row.createCell(4).setCellValue(log.getAttendanceDate().format(dateFormatter));
                row.createCell(5).setCellValue(log.getCheckTime().format(timeFormatter));
                row.createCell(6).setCellValue(log.getStatus());
                row.createCell(7).setCellValue(log.getLateMinutes() != null ? log.getLateMinutes() : 0);
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=attendance.xlsx")
                    .header(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .body(out.toByteArray());
        }
    }
}
