package com.iot.attendance.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import reactor.core.publisher.Mono;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.http.client.MultipartBodyBuilder;

import java.util.Map;
import java.util.UUID;

@Service
public class FastApiClient {

    private final WebClient webClient;

    public FastApiClient(WebClient.Builder webClientBuilder,
                         @Value("${fastapi.url:http://localhost:8000}") String fastApiUrl) {
        this.webClient = webClientBuilder.baseUrl(fastApiUrl).build();
    }

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
    
    public Mono<Map> extractFaceEmbedding(MultipartFile file) {
        try {
            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("file", file.getResource());
            
            return webClient.post()
                    .uri("/internal/v1/extract-embedding")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .bodyToMono(Map.class);
        } catch (Exception e) {
            return Mono.error(e);
        }
    }
}
