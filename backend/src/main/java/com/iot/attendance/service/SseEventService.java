package com.iot.attendance.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SseEventService {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(error -> emitters.remove(emitter));

        try {
            emitter.send(SseEmitter.event()
                    .data(Map.of(
                            "type", "connected",
                            "data", Map.of("status", "CONNECTED"))));
        } catch (IOException exception) {
            emitters.remove(emitter);
            emitter.completeWithError(exception);
        }

        return emitter;
    }

    public void publishEvent(String eventType, Object data) {
        List<SseEmitter> deadEmitters = new java.util.ArrayList<>();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", eventType);
        payload.put("data", data);

        for (SseEmitter emitter : emitters) {
            try {
                // Không dùng .name(eventType).
                // Frontend sẽ nhận tất cả bằng onmessage.
                emitter.send(
                        SseEmitter.event()
                                .data(payload)
                );
            } catch (IOException exception) {
                deadEmitters.add(emitter);
            }
        }

        emitters.removeAll(deadEmitters);
    }
}