package com.iot.attendance.service;

import com.iot.attendance.dto.CvVerificationRequest;
import com.iot.attendance.dto.CvVerificationResponse;
import com.iot.attendance.dto.FaceEmbeddingResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
public class FastApiClient {

    private final WebClient webClient;
    private final String internalApiKey;
    private final int captureLivenessTimeoutMs;
    private final int faceMatchingTimeoutMs;

    public FastApiClient(
            WebClient.Builder builder,
            @Value("${fastapi.url:http://172.20.10.5:8000}") String fastApiUrl,
            @Value("${fastapi.internal-api-key}") String internalApiKey,
            @Value("${attendance.capture-liveness-timeout-ms:25000}") int captureLivenessTimeoutMs,
            @Value("${attendance.face-matching-timeout-ms:20000}") int faceMatchingTimeoutMs
    ) {
        this.webClient = builder.baseUrl(fastApiUrl).build();
        this.internalApiKey = internalApiKey;
        this.captureLivenessTimeoutMs = captureLivenessTimeoutMs;
        this.faceMatchingTimeoutMs = faceMatchingTimeoutMs;
    }

    public FaceEmbeddingResponse createFaceEmbedding(MultipartFile image) {
        MultipartBodyBuilder multipart = new MultipartBodyBuilder();
        MediaType contentType = MediaType.parseMediaType(
                Optional.ofNullable(image.getContentType()).orElse("image/jpeg")
        );

        multipart.part("image", image.getResource())
                .filename(Optional.ofNullable(image.getOriginalFilename()).orElse("face.jpg"))
                .contentType(contentType);

        return webClient.post()
                .uri("/internal/v1/face-embeddings")
                .header("INTERNAL-API-KEY", internalApiKey)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(multipart.build()))
                .retrieve()
                .bodyToMono(FaceEmbeddingResponse.class)
                .timeout(Duration.ofSeconds(60))
                .block();
    }

    public Mono<CvVerificationResponse> requestFaceVerification(
            UUID sessionId,
            UUID expectedUserId,
            String cameraId
    ) {
        CvVerificationRequest request = new CvVerificationRequest(
                sessionId,
                expectedUserId,
                cameraId,
                captureLivenessTimeoutMs,
                faceMatchingTimeoutMs
        );

        return webClient.post()
                .uri("/internal/v1/verifications")
                .header("INTERNAL-API-KEY", internalApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(CvVerificationResponse.class)
                .timeout(Duration.ofSeconds(60));
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
