package com.iot.attendance.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Holds the latest OLED message sent by an admin from the frontend.
 * The ESP32 polls GET /api/v1/notifications/pending to consume it.
 * Only the most-recent unread message is kept; older ones are overwritten.
 */
@Service
public class OledNotificationService {

    private final AtomicReference<String> pending = new AtomicReference<>(null);

    /** Store a new message (overwrites any previously unread one). */
    public void enqueue(String message) {
        pending.set(message);
    }

    /**
     * Consume the pending message.
     * Returns the message and clears it atomically so it is delivered once.
     */
    public String consume() {
        return pending.getAndSet(null);
    }

    /** Non-destructive peek – used by the controller for the polling response. */
    public boolean hasPending() {
        return pending.get() != null;
    }
}
