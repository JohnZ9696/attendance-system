package com.iot.attendance.controller;

import com.iot.attendance.config.JwtTokenProvider;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtTokenProvider tokenProvider;

    @Value("${proctor.username:proctor}")
    private String proctorUsername;

    @Value("${proctor.password:proctor123}")
    private String proctorPassword;

    @Value("${lead.proctor.username:admin}")
    private String leadProctorUsername;

    @Value("${lead.proctor.password:admin123}")
    private String leadProctorPassword;
    
    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        String role = null;
        if (proctorUsername.equals(request.getUsername()) && proctorPassword.equals(request.getPassword())) {
            role = "PROCTOR";
        } else if (leadProctorUsername.equals(request.getUsername()) && leadProctorPassword.equals(request.getPassword())) {
            role = "LEAD_PROCTOR";
        }

        if (role == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        }

        String token = tokenProvider.generateToken(request.getUsername(), role);
        long expiresAt = System.currentTimeMillis() + jwtExpiration;

        return ResponseEntity.ok(Map.of(
                "token", token,
                "role", role,
                "expiresAt", new Date(expiresAt).toString()
        ));
    }

    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }
}
