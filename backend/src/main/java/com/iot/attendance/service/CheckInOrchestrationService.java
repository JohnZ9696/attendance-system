package com.iot.attendance.service;

import com.iot.attendance.entity.Student;
import com.iot.attendance.entity.VerificationLog;
import com.iot.attendance.entity.VerificationResult;
import com.iot.attendance.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CheckInOrchestrationService {

    private final StudentRepository studentRepository;
    private final VerificationService verificationService;
    private final FastApiClient fastApiClient;

    public void handleRfidScan(String deviceId, String rfidUid) {
        String normalizedUid = rfidUid.toUpperCase().replaceAll("[:\\s]", "");
        Student student = studentRepository.findByUidAndIsActiveTrue(normalizedUid)
                .orElseThrow(() -> new IllegalArgumentException("STUDENT_NOT_FOUND_OR_INACTIVE"));

        if (student.getFaceEmbedding() == null
                || student.getFaceEmbedding().isBlank()) {
            throw new IllegalStateException("FACE_NOT_ENROLLED");
        }

        VerificationLog log = verificationService.createPendingVerification(student, normalizedUid);
        fastApiClient.requestFaceVerification(log.getId(), student.getId(), deviceId)
                .subscribe();
    }
}
