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

    public VerificationService(VerificationLogRepository repository) {
        this.repository = repository;
    }

    public VerificationLog createPendingVerification(Student student, String scannedUid) {
        VerificationLog log = new VerificationLog();
        log.setStudent(student);
        log.setScannedUid(scannedUid);
        log.setResult(VerificationResult.PENDING);
        log.setStartedAt(OffsetDateTime.now());
        return repository.save(log);
    }
}
