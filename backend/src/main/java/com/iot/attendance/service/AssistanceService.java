package com.iot.attendance.service;

import com.iot.attendance.dto.AssistanceCreateRequest;
import com.iot.attendance.entity.AssistanceRequest;
import com.iot.attendance.repository.AssistanceRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AssistanceService {
    private final AssistanceRequestRepository repository;
    private final SseEventService sseEventService;

    public AssistanceService(AssistanceRequestRepository repository, SseEventService sseEventService) {
        this.repository = repository;
        this.sseEventService = sseEventService;
    }

    @Transactional
    public AssistanceRequest create(AssistanceCreateRequest request) {
        AssistanceRequest entity = new AssistanceRequest();
        entity.setSource(request.source());
        entity.setMessage(request.type());
        entity.setStatus("OPEN");

        // createdAt is auto-set by @CreationTimestamp
        return repository.save(entity);
    }

    public List<AssistanceRequest> getAll() {
        return repository.findAll();
    }

    public AssistanceRequest updateStatus(UUID id, String status) {
        AssistanceRequest request = repository.findById(id).orElseThrow();
        request.setStatus(status);
        AssistanceRequest saved = repository.save(request);
        sseEventService.publishEvent("ASSISTANCE_UPDATE", saved);
        return saved;
    }
}
