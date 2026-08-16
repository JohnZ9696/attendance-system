package com.iot.attendance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record StudentRequest(
        String name,
        String mssv,
        String rfidUid,
        @JsonProperty("is_active") Boolean isActive,
        String parentPhone,
        String parentEmail
) {}