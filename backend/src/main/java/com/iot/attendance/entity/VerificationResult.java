package com.iot.attendance.entity;

public enum VerificationResult {
    PENDING,
    VERIFIED,
    RFID_INVALID,
    CAMERA_OFFLINE,
    CAPTURE_TIMEOUT,
    LIVENESS_FAILED,
    FACE_BELOW_THRESHOLD,
    FACE_MATCH_TIMEOUT,
    ERROR
}