package com.iot.attendance.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

@Service
public class FastApiClient {

    private final WebClient webClient;

    /**
     * Creates a client configured with the specified FastAPI base URL.
     *
     * @param fastApiUrl the base URL of the FastAPI service
     */
    public FastApiClient(WebClient.Builder webClientBuilder,
                         @Value("${fastapi.url:http://localhost:8000}") String fastApiUrl) {
        this.webClient = webClientBuilder.baseUrl(fastApiUrl).build();
    }

    /**
     * Submits a face-verification request for a student.
     *
     * @param studentId      the student's identifier
     * @param expectedUserId the user identifier expected for the verification
     * @return the verification response
     */
    public Mono<Map> requestFaceVerification(UUID studentId, String expectedUserId) {
        return webClient.post()
                .uri("/internal/v1/verifications")
                .bodyValue(Map.of(
                        "expectedUserId", expectedUserId,
                        "studentId", studentId
                ))
                .retrieve()
                .bodyToMono(Map.class);
    }
}
