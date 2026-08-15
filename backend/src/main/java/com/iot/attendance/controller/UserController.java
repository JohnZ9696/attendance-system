package com.iot.attendance.controller;

import com.iot.attendance.repository.StudentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final StudentRepository repository;

    public UserController(StudentRepository repository) {
        this.repository = repository;
    }

    /**
     * Retrieves all students.
     *
     * @return the collection of students
     */
    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(repository.findAll());
    }

    /**
     * Handles requests to create a user.
     *
     * @return an empty successful response
     */
    @PostMapping
    public ResponseEntity<?> create() {
        return ResponseEntity.ok().build();
    }

    /**
     * Accepts a user update request.
     *
     * @return an empty successful response
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> update() {
        return ResponseEntity.ok().build();
    }

    /**
     * Handles requests to delete a user.
     *
     * @return an empty successful response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete() {
        return ResponseEntity.ok().build();
    }

    /**
     * Updates a user's face data.
     *
     * @return an empty successful response
     */
    @PostMapping("/{id}/face")
    public ResponseEntity<?> updateFace() {
        return ResponseEntity.ok().build();
    }
}
