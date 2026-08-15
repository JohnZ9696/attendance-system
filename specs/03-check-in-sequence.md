# Check-In Sequence

## 1. Sequence Diagram

```mermaid
sequenceDiagram
    participant ESP as ESP32 + RFID
    participant SB as Spring Boot
    participant CV as FastAPI
    participant CAM as ESP32-CAM
    participant DB as Supabase
    participant WEB as React

    ESP->>SB: RFID scan(uid, deviceId)
    SB->>DB: Find user by normalized UID
    DB-->>SB: expectedUserId
    SB->>SB: Create verification session
    SB->>CV: Verify(sessionId, expectedUserId, cameraId)
    CAM->>CV: Frames(cameraId, capturedAt)
    CV->>DB: Load expected user face profile
    CV->>CV: Face + blink liveness + similarity
    CV-->>SB: Verification result
    SB->>SB: Validate session and calculate late
    SB->>DB: Insert one daily check-in
    SB-->>ESP: LED/buzzer result
    SB-->>WEB: Attendance event via SSE
```

## 2. State Machine

```mermaid
stateDiagram-v2
    [*] --> RFID_PENDING
    RFID_PENDING --> REJECTED: UID invalid
    RFID_PENDING --> FACE_WAITING: UID valid
    FACE_WAITING --> LIVENESS_CHECK: fresh frames
    FACE_WAITING --> REJECTED: capture timeout
    LIVENESS_CHECK --> FACE_MATCHING: blink passed
    LIVENESS_CHECK --> REJECTED: liveness failed
    FACE_MATCHING --> VERIFIED: similarity >= 30%
    FACE_MATCHING --> REJECTED: below threshold / timeout
    VERIFIED --> COMPLETED: daily check-in inserted
    VERIFIED --> ALREADY_CHECKED_IN: daily record exists
    REJECTED --> [*]
    COMPLETED --> [*]
    ALREADY_CHECKED_IN --> [*]
```

## 3. Timeout Rules
- **Capture/Liveness Window:** 10,000 ms. If no valid face or blink is detected, return `CAPTURE_TIMEOUT`.
- **Matching Timeout:** 5,000 ms after valid frame/liveness. If processing takes too long, return `FACE_MATCH_TIMEOUT`.
- Total theoretical maximum time: 15,000 ms.

## 4. Late Calculation
```text
timezone = Asia/Ho_Chi_Minh
cutoff   = 07:30:00

if localCheckInTime <= 07:30:00:
    status = ON_TIME
    lateMinutes = 0
else:
    status = LATE
    lateMinutes = ceil((localCheckInTime - 07:30:00) / 60 seconds)
```
Spring Boot handles this entirely. ESP32, React, and FastAPI do not calculate late status.
