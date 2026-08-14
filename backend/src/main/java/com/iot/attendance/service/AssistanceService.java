package com.iot.attendance.service;

import com.iot.attendance.entity.AssistanceRequest;
import com.iot.attendance.entity.User;
import com.iot.attendance.repository.AssistanceRequestRepository;
import com.iot.attendance.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AssistanceService {
    @Autowired
    private AssistanceRequestRepository assistanceRequestRepository;

    @Autowired
    private UserRepository userRepository;

    public List<AssistanceRequest> getAllRequests() {
        return assistanceRequestRepository.findAll();
    }

    public AssistanceRequest createRequest(UUID userId, String message) {
        Optional<User> user = userId != null ? userRepository.findById(userId) : Optional.empty();
        AssistanceRequest request = new AssistanceRequest();
        user.ifPresent(request::setUser);
        request.setMessage(message);
        return assistanceRequestRepository.save(request);
    }
}
