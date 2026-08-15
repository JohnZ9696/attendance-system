package com.iot.attendance.service;

import com.iot.attendance.entity.Student;
import com.iot.attendance.entity.VerificationLog;
import com.iot.attendance.repository.StudentRepository;
import org.springframework.stereotype.Service;

@Service
public class CheckInOrchestrationService {
    private final StudentRepository studentRepository;
    private final VerificationService verificationService;
    private final FastApiClient fastApiClient;

    public CheckInOrchestrationService(StudentRepository studentRepository,
                                       VerificationService verificationService,
                                       FastApiClient fastApiClient) {
        this.studentRepository = studentRepository;
        this.verificationService = verificationService;
        this.fastApiClient = fastApiClient;
    }

    /**
     * Processes an RFID scan by locating the active student and initiating face verification.
     *
     * @param rfidUid the scanned RFID identifier, with colons and whitespace ignored
     * @throws RuntimeException if no active student matches the scanned identifier
     */
    public void handleRfidScan(String deviceId, String rfidUid) {
        String normalizedUid = rfidUid.toUpperCase().replaceAll("[:\\s]", "");
        Student student = studentRepository.findByUidAndIsActiveTrue(normalizedUid)
                .orElseThrow(() -> new RuntimeException("Student not found or inactive"));

        VerificationLog log = verificationService.createPendingVerification(student, normalizedUid);
        fastApiClient.requestFaceVerification(log.getId(), student.getId().toString())
                .subscribe();
    }
}
