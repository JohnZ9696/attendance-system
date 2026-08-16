package com.iot.attendance.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.attendance.dto.FaceEmbeddingResponse;
import com.iot.attendance.dto.FaceEnrollmentResponse;
import com.iot.attendance.entity.Student;
import com.iot.attendance.repository.StudentRepository;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FaceEnrollmentService {

    private static final long MAX_IMAGE_BYTES = 5L * 1024L * 1024L;
    private static final Set<String> ACCEPTED_TYPES = Set.of("image/jpeg", "image/png");

    private final StudentRepository studentRepository;
    private final FastApiClient fastApiClient;
    private final ObjectMapper objectMapper;

    public FaceEnrollmentService(
            StudentRepository studentRepository,
            FastApiClient fastApiClient,
            ObjectMapper objectMapper
    ) {
        this.studentRepository = studentRepository;
        this.fastApiClient = fastApiClient;
        this.objectMapper = objectMapper;
    }

    public FaceEnrollmentResponse enroll(UUID studentId, MultipartFile image) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("STUDENT_NOT_FOUND"));

        if (image.isEmpty() || image.getSize() > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("IMAGE_EMPTY_OR_TOO_LARGE");
        }

        String contentType = image.getContentType();
        if (contentType == null || contentType.isBlank()) {
            String filename = image.getOriginalFilename() == null ? "" : image.getOriginalFilename().toLowerCase();
            if (filename.endsWith(".png")) {
                contentType = "image/png";
            } else if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
                contentType = "image/jpeg";
            }
        }
        if (!ACCEPTED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("ONLY_JPEG_OR_PNG_ALLOWED");
        }

        FaceEmbeddingResponse result = fastApiClient.createFaceEmbedding(image);
        if (result == null || result.embedding() == null || result.embedding().size() != 512) {
            throw new IllegalStateException("INVALID_AI_EMBEDDING");
        }

        try {
            student.setFaceEmbedding(objectMapper.writeValueAsString(result.embedding()));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("CANNOT_SERIALIZE_EMBEDDING", exception);
        }

        studentRepository.save(student);

        return new FaceEnrollmentResponse(
                student.getId(),
                true
        );
    }
}