package com.iot.attendance.service;

import com.iot.attendance.entity.User;
import com.iot.attendance.repository.AssistanceRequestRepository;
import com.iot.attendance.repository.AttendanceRecordRepository;
import com.iot.attendance.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AttendanceRecordRepository attendanceRecordRepository;
    @Autowired
    private AssistanceRequestRepository assistanceRequestRepository;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(UUID id) {
        return userRepository.findById(id);
    }

    public Optional<User> getUserByRfidUid(String rfidUid) {
        return userRepository.findByRfidUid(rfidUid);
    }

    public User createUser(User user) {
        normalizeOptionalFields(user);
        return userRepository.save(user);
    }

    public Optional<User> updateUser(UUID id, User update) {
        return userRepository.findById(id).map(user -> {
            user.setRfidUid(update.getRfidUid());
            user.setName(update.getName());
            user.setMssv(update.getMssv());
            user.setFaceEmbedding(normalizeFaceEmbedding(update.getFaceEmbedding()));
            return userRepository.save(user);
        });
    }

    private void normalizeOptionalFields(User user) {
        user.setFaceEmbedding(normalizeFaceEmbedding(user.getFaceEmbedding()));
    }

    private String normalizeFaceEmbedding(String faceEmbedding) {
        return faceEmbedding == null || faceEmbedding.isBlank() ? null : faceEmbedding;
    }

    @Transactional
    public boolean deleteUser(UUID id) {
        if (!userRepository.existsById(id)) {
            return false;
        }
        attendanceRecordRepository.deleteByUserId(id);
        assistanceRequestRepository.deleteByUserId(id);
        userRepository.deleteById(id);
        return true;
    }
}
