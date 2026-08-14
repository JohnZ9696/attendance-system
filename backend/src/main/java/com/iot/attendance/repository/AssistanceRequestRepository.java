package com.iot.attendance.repository;

import com.iot.attendance.entity.AssistanceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AssistanceRequestRepository extends JpaRepository<AssistanceRequest, UUID> {
    void deleteByUserId(UUID userId);
}
