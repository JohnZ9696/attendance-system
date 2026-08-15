package com.iot.attendance.controller;

import com.iot.attendance.dto.StudentResponse;
import com.iot.attendance.entity.Student;
import com.iot.attendance.repository.StudentRepository;
import com.iot.attendance.service.FaceEnrollmentService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final StudentRepository repository;
    private final FaceEnrollmentService faceEnrollmentService;

    public UserController(
            StudentRepository repository,
            FaceEnrollmentService faceEnrollmentService
    ) {
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
    public ResponseEntity<?> create(@RequestBody Student student) {
        student.setId(null);
        Student saved = repository.save(student);
        return ResponseEntity.ok(StudentResponse.from(saved));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable UUID id, @RequestBody Student update) {
        return repository.findById(id).map(student -> {
            student.setFullName(update.getFullName());
            student.setMssv(update.getMssv());
            student.setUid(update.getUid());
            student.setIsActive(update.getIsActive());
            Student saved = repository.save(student);
            return ResponseEntity.ok(StudentResponse.from(saved));
        }).orElse(ResponseEntity.notFound().build());
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
            consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> updateFace(
            @PathVariable UUID id,
            @RequestPart("image") MultipartFile image
    ) {
        return ResponseEntity.ok(faceEnrollmentService.enroll(id, image));
    }
}
