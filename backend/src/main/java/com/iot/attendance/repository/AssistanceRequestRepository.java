package com.iot.attendance.repository;

import com.iot.attendance.entity.AssistanceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface AssistanceRequestRepository extends JpaRepository<AssistanceRequest, UUID> {
}
