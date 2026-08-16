package com.iot.attendance.service;

import com.iot.attendance.entity.Student;
import com.iot.attendance.entity.VerificationLog;
import com.iot.attendance.entity.VerificationResult;
import com.iot.attendance.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CheckInOrchestrationService {

    private final StudentRepository studentRepository;
    private final VerificationService verificationService;
    private final FastApiClient fastApiClient;
    private final VerificationCompletionService verificationCompletionService;

    @Value("${attendance.camera-id:cam-01}")
    private String cameraId;

    public void handleRfidScan(String deviceId, String rfidUid) {
        String normalizedUid = rfidUid.toUpperCase().replaceAll("[:\\s]", "");
        Student student = studentRepository.findByUidAndIsActiveTrue(normalizedUid)
                .orElseThrow(() -> new IllegalArgumentException("STUDENT_NOT_FOUND_OR_INACTIVE"));

        if (student.getFaceEmbedding() == null
                || student.getFaceEmbedding().isBlank()) {
            throw new IllegalStateException("FACE_NOT_ENROLLED");
        }

        VerificationLog verificationLog = verificationService.createPendingVerification(student, normalizedUid);
        fastApiClient.requestFaceVerification(verificationLog.getId(), student.getId(), cameraId)
                .subscribe(
                        response -> verificationCompletionService.complete(verificationLog.getId(), response),
                        error -> verificationCompletionService.fail(verificationLog.getId(), error.getMessage())
                );
    }
}
