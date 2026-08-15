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

    /**
     * Creates an assistance service with the repository and event service it uses.
     *
     * @param repository     the repository for assistance requests
     * @param sseEventService the service for publishing assistance events
     */
    public AssistanceService(AssistanceRequestRepository repository, SseEventService sseEventService) {
        this.repository = repository;
        this.sseEventService = sseEventService;
    }

    /**
     * Retrieves all assistance requests.
     *
     * @return all assistance requests
     */
    public List<AssistanceRequest> getAll() {
        return repository.findAll();
    }

    /**
     * Updates the status of an assistance request and publishes the updated request.
     *
     * @param id     the identifier of the assistance request
     * @param status the new status
     * @return the saved assistance request
     * @throws NoSuchElementException if no request exists with the specified identifier
     */
    public AssistanceRequest updateStatus(UUID id, String status) {
        AssistanceRequest request = repository.findById(id).orElseThrow();
        request.setStatus(status);
        AssistanceRequest saved = repository.save(request);
        sseEventService.publishEvent("ASSISTANCE_UPDATE", saved);
        return saved;
    }
}
