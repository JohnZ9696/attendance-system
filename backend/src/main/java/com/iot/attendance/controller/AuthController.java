package com.iot.attendance.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    /**
     * Handles login requests with an empty successful response.
     *
     * @return an HTTP 200 response with no body
     */
    @PostMapping("/login")
    public ResponseEntity<?> login() {
        return ResponseEntity.ok().build();
    }
}
