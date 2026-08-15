# Error Codes and Responses

| Code | Meaning | LED/Buzzer | Attendance Recorded |
|---|---|---|---|
| `RFID_INVALID` | UID does not exist or is inactive | Red + short beep | No |
| `CAMERA_OFFLINE` | No new frames received | Red + 2 beeps | No |
| `CAPTURE_TIMEOUT` | No face/liveness detected within 10s | Red + 2 beeps | No |
| `LIVENESS_FAILED` | Blink/liveness check failed | Red + 2 beeps | No |
| `FACE_BELOW_THRESHOLD` | Similarity < 30% | Red + long beep | No |
| `FACE_MATCH_TIMEOUT` | Matching took over 5s | Red + 2 beeps | No |
| `ALREADY_CHECKED_IN` | User already checked in today | Blue blink (or distinct color) | No (duplicate blocked) |
| `CHECK_IN_ON_TIME` | Success before/at 07:30 | Green + short beep | Yes, `ON_TIME` |
| `CHECK_IN_LATE` | Success after 07:30 | Green + 2 short beeps | Yes, `LATE` |
| `CLOUD_WRITE_FAILED` | Failed to save to Supabase | Red + error beep | No (do not report false success) |
| `INCIDENT_RECORDED` | Button event successfully sent to web | Confirm LED | N/A |
