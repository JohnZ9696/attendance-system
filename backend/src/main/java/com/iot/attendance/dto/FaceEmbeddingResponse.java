package com.iot.attendance.dto;

import java.util.List;

public record FaceEmbeddingResponse(
        List<Double> embedding,
        Integer faceCount,
        Double blurScore
) {}