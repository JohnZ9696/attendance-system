package com.iot.attendance.repository;

import com.iot.attendance.entity.VerificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface VerificationLogRepository extends JpaRepository<VerificationLog, UUID> {
}
