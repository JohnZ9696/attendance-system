package com.iot.attendance.controller;

import com.iot.attendance.dto.StudentRequest;
import com.iot.attendance.dto.StudentResponse;
import com.iot.attendance.entity.Student;
import com.iot.attendance.repository.StudentRepository;
import com.iot.attendance.service.FaceEnrollmentService;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final StudentRepository repository;
    private final FaceEnrollmentService faceEnrollmentService;

    public UserController(StudentRepository repository, FaceEnrollmentService faceEnrollmentService) {
        this.repository = repository;
        this.faceEnrollmentService = faceEnrollmentService;
    }

    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(
                repository.findAll().stream()
                        .map(StudentResponse::from)
                        .toList()
        );
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody StudentRequest request) {
        Student student = new Student();
        student.setFullName(request.name());
        student.setMssv(request.mssv());
        if (request.rfidUid() != null) student.setUid(normalizeUid(request.rfidUid()));
        student.setParentPhone(request.parentPhone());
        student.setParentEmail(request.parentEmail());
        student.setIsActive(request.isActive() == null || request.isActive());
        Student saved = repository.save(student);
        return ResponseEntity.ok(StudentResponse.from(saved));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable UUID id, @RequestBody StudentRequest update) {
        return repository.findById(id).map(student -> {
            if (update.name() != null) student.setFullName(update.name());
            if (update.mssv() != null) student.setMssv(update.mssv());
            if (update.rfidUid() != null) student.setUid(normalizeUid(update.rfidUid()));
            if (update.parentPhone() != null) student.setParentPhone(update.parentPhone());
            if (update.parentEmail() != null) student.setParentEmail(update.parentEmail());
            if (update.isActive() != null) student.setIsActive(update.isActive());
            Student saved = repository.save(student);
            return ResponseEntity.ok(StudentResponse.from(saved));
        }).orElse(ResponseEntity.notFound().build());
    }

    private static String normalizeUid(String uid) {
        return uid.toUpperCase().replaceAll("[\\s:]", "");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(
            value = "/{id}/face",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> updateFace(
            @PathVariable UUID id,
            @RequestPart("image") MultipartFile image
    ) {
        return ResponseEntity.ok(faceEnrollmentService.enroll(id, image));
    }
}