package com.iot.attendance.service;

import com.iot.attendance.dto.AssistanceCreateRequest;
import com.iot.attendance.entity.AssistanceRequest;
import com.iot.attendance.repository.AssistanceRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AssistanceService {

    private static final Logger log = LoggerFactory.getLogger(AssistanceService.class);

    private final AssistanceRequestRepository repository;
    private final SseEventService sseEventService;
    private final AssistanceNotificationService notificationService;

    public AssistanceService(
            AssistanceRequestRepository repository,
            SseEventService sseEventService,
            AssistanceNotificationService notificationService) {
        this.repository = repository;
        this.sseEventService = sseEventService;
        this.notificationService = notificationService;
    }

    @Transactional
    public AssistanceRequest create(AssistanceCreateRequest request) {
        AssistanceRequest entity = new AssistanceRequest();
        entity.setSource(request.source());
        entity.setMessage(request.type());
        entity.setStatus("OPEN");

        AssistanceRequest saved = repository.save(entity);

        sseEventService.publishEvent("incident", saved);

        try {
            notificationService.notifyIncident(saved);
        } catch (RuntimeException exception) {
            log.error("Incident notification failed for assistance {}", saved.getId(), exception);
        }

        return repository.save(saved);
    }

    @Transactional(readOnly = true)
    public List<AssistanceRequest> getAll() {
        return repository.findAll();
    }

    @Transactional
    public AssistanceRequest updateStatus(UUID id, String status) {
        AssistanceRequest request = repository.findById(id).orElseThrow();
        request.setStatus(status);
        if ("RESOLVED".equals(status)) {
            request.setResolvedAt(OffsetDateTime.now());
        }
        AssistanceRequest saved = repository.save(request);
        sseEventService.publishEvent("ASSISTANCE_UPDATE", saved);
        return saved;
    }
}
