# Acceptance Tests

## 1. Check-In Flow
- **Happy Path:** Valid UID, liveness pass, similarity 30%, not yet checked in -> exactly one record created.
- **Threshold Boundary:** Similarity 29.99% -> Rejected, no attendance created.
- **Liveness Fail:** High similarity but liveness fail -> Rejected.
- **Mismatched Face:** UID of User A, but face does not match User A's profile above threshold -> Rejected (do not search for User B).
- **Duplicate Scan:** Already checked in, scans again -> `ALREADY_CHECKED_IN`, no new record created.

## 2. Timeout
- No valid frame/liveness in 10s -> `CAPTURE_TIMEOUT`.
- Valid frame, but matching exceeds 5s -> `FACE_MATCH_TIMEOUT`.
- Timeouts must not create attendance.

## 3. Late Calculation
- Check-in at `07:29:59` -> `ON_TIME`.
- Check-in at `07:30:00` -> `ON_TIME`.
- Check-in at `07:30:01` -> `LATE`, `lateMinutes=1`.
- Display on React dashboard must match Spring Boot's output.

## 4. Push Button
- Single press does not create multiple incidents (debounce).
- Incident appears in realtime on the proctor website.
- Proctor can acknowledge/resolve the incident.
- If Spring/Supabase is down, device does not show false success.

## 5. Connectivity & Security
- React build does not contain Supabase service-role key.
- Spring connects to Supabase, FastAPI, and receives ESP32 requests.
- FastAPI receives frames and reads face profile.
- Health endpoint correctly differentiates between service running and dependencies (model/db) being ready.
- ESP32 and ESP32-CAM use separate identifiers.
- PROCTOR role cannot call settings/user management APIs; LEAD_PROCTOR can.
