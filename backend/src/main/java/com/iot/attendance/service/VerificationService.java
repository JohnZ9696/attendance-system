package com.iot.attendance.service;

import com.iot.attendance.entity.Student;
import com.iot.attendance.entity.VerificationLog;
import com.iot.attendance.entity.VerificationResult;
import com.iot.attendance.repository.VerificationLogRepository;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class VerificationService {
    private final VerificationLogRepository repository;

    /**
     * Creates a verification service.
     */
    public VerificationService(VerificationLogRepository repository) {
        this.repository = repository;
    }

    /**
     * Creates and persists a pending verification log for a student and scanned UID.
     *
     * @param student   the student associated with the verification
     * @param scannedUid the UID scanned for the verification
     * @return the persisted pending verification log
     */
    public VerificationLog createPendingVerification(Student student, String scannedUid) {
        VerificationLog log = new VerificationLog();
        log.setStudent(student);
        log.setScannedUid(scannedUid);
        log.setResult(VerificationResult.PENDING);
        log.setStartedAt(OffsetDateTime.now());
        return repository.save(log);
    }
}
