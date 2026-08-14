package com.iot.attendance.service;

import org.springframework.stereotype.Service;

@Service
public class RfidEnrollmentService {
    private Status status = Status.IDLE;
    private String uid;

    public synchronized EnrollmentState start() {
        status = Status.WAITING;
        uid = null;
        return state();
    }

    public synchronized EnrollmentState getState() {
        return state();
    }

    public synchronized EnrollmentState cancel() {
        status = Status.IDLE;
        uid = null;
        return state();
    }

    public synchronized EnrollmentState submit(String scannedUid) {
        if (status == Status.WAITING) {
            uid = scannedUid.trim().toUpperCase();
            status = Status.SCANNED;
        }
        return state();
    }

    private EnrollmentState state() {
        return new EnrollmentState(status.name(), uid);
    }

    // ponytail: one transient enrollment session is enough for one ESP32;
    // persist sessions when multiple readers or backend replicas are introduced.
    private enum Status { IDLE, WAITING, SCANNED }

    public record EnrollmentState(String status, String uid) {}
}
