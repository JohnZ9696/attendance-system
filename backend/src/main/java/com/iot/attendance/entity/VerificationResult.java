package com.iot.attendance.entity;

public enum VerificationResult {
    PENDING,
    VERIFIED,
    RFID_INVALID,
    FACE_NOT_ENROLLED,
    CAMERA_OFFLINE,
    CAPTURE_TIMEOUT,
    LIVENESS_FAILED,
    FACE_BELOW_THRESHOLD,
    FACE_MATCH_TIMEOUT,
    MULTIPLE_FACES,
    ALREADY_CHECKED_IN,
    ERROR
}