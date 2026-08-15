# Device Contracts

## 1. ESP32 + RC522 (RFID)
- Must debounce button presses (min 50ms, 2s cooldown).
- Must normalize UID (uppercase, remove spaces/colons) before sending to Spring Boot.
- Sends POST to `/api/v1/devices/{deviceId}/rfid-scans` with JSON:
  ```json
  {
    "uid": "A1B2C3D4",
    "scannedAt": "2026-08-15T07:25:00Z"
  }
  ```
- No long blocking `delay()` functions that could drop scans or button events. Uses state machine / millis().
- Controls LED and buzzer based on the error code/response from Spring Boot.

## 2. ESP32 Push Button
- Uses `INPUT_PULLUP`, press is `LOW`.
- Sends POST to `/api/v1/devices/{deviceId}/incidents` with JSON:
  ```json
  {
    "type": "HARDWARE_HELP_REQUEST",
    "source": "PUSH_BUTTON",
    "occurredAt": "device timestamp"
  }
  ```

## 3. ESP32-CAM
- Sends JPEG frames continuously or upon trigger to FastAPI.
- Sends POST to `/internal/v1/cameras/{cameraId}/frames` with binary payload.
- Does not wait for matching result, purely a streaming/upload client.
