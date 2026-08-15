package com.iot.attendance.service;

import com.iot.attendance.entity.AttendanceLog;
import com.iot.attendance.entity.Student;
import com.iot.attendance.entity.VerificationLog;
import com.iot.attendance.entity.VerificationResult;
import com.iot.attendance.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CheckInOrchestrationService {
    private final StudentRepository studentRepository;
    private final VerificationService verificationService;
    private final FastApiClient fastApiClient;
    private final AttendanceService attendanceService;

    public Map<String, Object> handleRfidScan(String deviceId, String rfidUid) {
        Map<String, Object> response = new HashMap<>();
        
        String normalizedUid = rfidUid.toUpperCase().replaceAll("[:\\s]", "");
        Student student = studentRepository.findByUidAndIsActiveTrue(normalizedUid).orElse(null);
        
        if (student == null) {
            response.put("resultCode", "RFID_INVALID");
            response.put("message", "Student not found or inactive");
            return response;
        }

        VerificationLog log = verificationService.createPendingVerification(student, normalizedUid);
        
        Map<String, Object> fastApiResult;
        try {
            fastApiResult = fastApiClient.requestFaceVerification(log.getId(), student.getId().toString())
                    .block(Duration.ofSeconds(15));
        } catch (Exception e) {
            log.setResult(VerificationResult.CAMERA_OFFLINE);
            log.setFailureReason(e.getMessage());
            verificationService.updateVerification(log);
            response.put("resultCode", "CAMERA_OFFLINE");
            response.put("message", "Camera offline or timeout");
            return response;
        }

        if (fastApiResult == null) {
            log.setResult(VerificationResult.ERROR);
            log.setFailureReason("Empty response from FastAPI");
            verificationService.updateVerification(log);
            response.put("resultCode", "ERROR");
            response.put("message", "Verification failed");
            return response;
        }

        String resultStr = (String) fastApiResult.getOrDefault("result", "ERROR");
        VerificationResult verificationResult;
        try {
            verificationResult = VerificationResult.valueOf(resultStr);
        } catch (IllegalArgumentException e) {
            verificationResult = VerificationResult.ERROR;
        }
        log.setResult(verificationResult);
        
        if (fastApiResult.containsKey("similarityPercent")) {
            log.setSimilarityPercent(new java.math.BigDecimal(fastApiResult.get("similarityPercent").toString()));
        }
        if (fastApiResult.containsKey("livenessPassed")) {
            log.setLivenessPassed((Boolean) fastApiResult.get("livenessPassed"));
        }
        if (fastApiResult.containsKey("modelName")) {
            log.setModelName((String) fastApiResult.get("modelName"));
        }
        if (fastApiResult.containsKey("modelVersion")) {
            log.setModelVersion((String) fastApiResult.get("modelVersion"));
        }
        if (fastApiResult.containsKey("failureReason")) {
            log.setFailureReason((String) fastApiResult.get("failureReason"));
        }
        
        verificationService.updateVerification(log);
        
        response.put("studentName", student.getFullName());

        if (verificationResult == VerificationResult.VERIFIED) {
            try {
                AttendanceLog attendanceLog = attendanceService.recordAttendance(student, log);
                response.put("resultCode", "ON_TIME".equals(attendanceLog.getStatus()) ? "CHECK_IN_ON_TIME" : "CHECK_IN_LATE");
                response.put("status", attendanceLog.getStatus());
                response.put("lateMinutes", attendanceLog.getLateMinutes());
                response.put("message", "Check-in successful");
            } catch (RuntimeException e) {
                if ("ALREADY_CHECKED_IN".equals(e.getMessage())) {
                    response.put("resultCode", "ALREADY_CHECKED_IN");
                    response.put("message", "Student already checked in today");
                } else {
                    response.put("resultCode", "ERROR");
                    response.put("message", e.getMessage());
                }
            }
        } else {
            response.put("resultCode", resultStr);
            response.put("message", "Verification failed: " + resultStr);
        }

        return response;
    }
}
