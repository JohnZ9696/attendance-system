package com.iot.attendance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.iot.attendance.entity.Student;
import java.time.OffsetDateTime;
import java.util.UUID;

public record StudentResponse(
        UUID id,
        String name,
        String mssv,
        String rfidUid,
        @JsonProperty("is_active") Boolean isActive,
        boolean faceRegistered,
        String parentPhone,
        String parentEmail,
        OffsetDateTime createdAt
) {
    public static StudentResponse from(Student student) {
        boolean registered = student.getFaceEmbedding() != null
                && !student.getFaceEmbedding().isBlank();

        return new StudentResponse(
                student.getId(),
                student.getFullName(),
                student.getMssv(),
                student.getUid(),
                student.getIsActive(),
                registered,
                student.getParentPhone(),
                student.getParentEmail(),
                student.getCreatedAt()
        );
    }
}