package com.iot.attendance.dto;

public record AssistanceCreateRequest(
        String deviceId,
        String type,
        String source,
        String occurredAt
) {
}