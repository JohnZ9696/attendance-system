package com.iot.attendance.service;

import com.iot.attendance.dto.CvVerificationResponse;
import com.iot.attendance.entity.Student;
import com.iot.attendance.entity.VerificationLog;
import com.iot.attendance.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class CheckInOrchestrationService {

    private final StudentRepository studentRepository;
    private final VerificationService verificationService;
    private final FastApiClient fastApiClient;
    private final VerificationCompletionService completionService;
    private final String cameraId;

    public CheckInOrchestrationService(
            StudentRepository studentRepository,
            VerificationService verificationService,
            FastApiClient fastApiClient,
            VerificationCompletionService completionService,
            @Value("${attendance.camera-id:cam-01}") String cameraId
    ) {
        this.studentRepository = studentRepository;
        this.verificationService = verificationService;
        this.fastApiClient = fastApiClient;
        this.completionService = completionService;
        this.cameraId = cameraId;
    }

    public UUID handleRfidScan(String deviceId, String rfidUid) {
        if (rfidUid == null || rfidUid.isBlank()) {
            throw new IllegalArgumentException("RFID_UID_REQUIRED");
        }

        String normalizedUid = rfidUid.toUpperCase().replaceAll("[:\\s]", "");
        Student student = studentRepository.findByUidAndIsActiveTrue(normalizedUid)
                .orElseThrow(() -> new IllegalArgumentException("STUDENT_NOT_FOUND_OR_INACTIVE"));

        if (student.getFaceEmbedding() == null || student.getFaceEmbedding().isBlank()) {
            throw new IllegalStateException("FACE_NOT_ENROLLED");
        }

        VerificationLog log = verificationService.createPendingVerification(
                student,
                normalizedUid
        );

        try {
            CvVerificationResponse response = fastApiClient.requestFaceVerification(
                            log.getId(),
                            student.getId(),
                            cameraId
                    )
                    .block(); // Wait synchronously for FastAPI result

            if (response != null) {
                completionService.complete(log.getId(), response);
            } else {
                completionService.fail(log.getId(), "FASTAPI_NO_RESPONSE");
            }
        } catch (Exception e) {
            completionService.fail(log.getId(), "FASTAPI_UNAVAILABLE_OR_TIMEOUT: " + e.getMessage());
        }

        return log.getId();
    }
}
