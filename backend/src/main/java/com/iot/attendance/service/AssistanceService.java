package com.iot.attendance.service;

import com.iot.attendance.entity.AssistanceRequest;
import com.iot.attendance.repository.AssistanceRequestRepository;
import org.springframework.stereotype.Service;

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
