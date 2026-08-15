package com.iot.attendance.controller;

import com.iot.attendance.entity.AttendanceLog;
import com.iot.attendance.repository.AttendanceLogRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final AttendanceLogRepository attendanceLogRepository;

    @GetMapping("/attendance.xlsx")
    public ResponseEntity<byte[]> exportAttendanceExcel(
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {

        List<AttendanceLog> logs;

        if (startDate != null && endDate != null) {
            logs = attendanceLogRepository.findByAttendanceDateBetween(startDate, endDate);
        } else if (date != null) {
            logs = attendanceLogRepository.findByAttendanceDate(date);
        } else {
            logs = attendanceLogRepository.findAll();
        }

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Attendance");
            Row headerRow = sheet.createRow(0);
            
            String[] columns = {"MSSV", "Full Name", "Date", "Check Time", "Status", "Late Minutes"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
            }

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

            int rowIdx = 1;
            for (AttendanceLog log : logs) {
                Row row = sheet.createRow(rowIdx++);
                
                String mssv = log.getStudent() != null ? log.getStudent().getMssv() : "";
                String fullName = log.getStudent() != null ? log.getStudent().getFullName() : "";
                
                row.createCell(0).setCellValue(mssv);
                row.createCell(1).setCellValue(fullName);
                row.createCell(2).setCellValue(log.getAttendanceDate() != null ? log.getAttendanceDate().format(dateFormatter) : "");
                row.createCell(3).setCellValue(log.getCheckTime() != null ? log.getCheckTime().format(timeFormatter) : "");
                row.createCell(4).setCellValue(log.getStatus() != null ? log.getStatus() : "");
                row.createCell(5).setCellValue(log.getLateMinutes() != null ? log.getLateMinutes() : 0);
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", "attendance.xlsx");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(out.toByteArray());
                    
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
