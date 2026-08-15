package com.iot.attendance.dto;

import java.util.UUID;

public record RfidScanResponse(UUID verificationSessionId, String message) {}