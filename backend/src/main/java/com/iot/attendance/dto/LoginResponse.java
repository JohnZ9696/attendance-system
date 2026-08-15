package com.iot.attendance.dto;

public record LoginResponse(String token, String role, String username) {}