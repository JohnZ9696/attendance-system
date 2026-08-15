package com.iot.attendance.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SseEventService {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    /**
     * Creates and registers a server-sent events subscription.
     *
     * @return the registered emitter with a 60-second timeout
     */
    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(60000L); // 1 minute timeout
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError((e) -> emitters.remove(emitter));

        return emitter;
    }

    /**
     * Publishes an event with the specified type and data to all active subscribers.
     *
     * @param eventType the name of the event to publish
     * @param data      the data included with the event
     */
    public void publishEvent(String eventType, Object data) {
        List<SseEmitter> deadEmitters = new java.util.ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventType)
                        .data(data));
            } catch (IOException e) {
                deadEmitters.add(emitter);
            }
        }
        emitters.removeAll(deadEmitters);
    }
}
