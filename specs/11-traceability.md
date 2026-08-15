# Traceability Matrix

| Requirement | Contract | Owner | Data | Verification | Milestone |
|---|---|---|---|---|---|
| FR-01 Wi-Fi/device | heartbeat API | Spring + ESP32 | devices | integration + hardware | M4 |
| FR-02 RFID | rfid-scans API | Spring + ESP32 | credentials/session | integration | M4 |
| FR-03 Face/liveness | verification API | FastAPI | profiles/attempts | CV integration + E2E | M5–M7 |
| FR-04 Cloud | repository/migration | Spring/FastAPI | Supabase | DB integration | M1/M3 |
| FR-05 User management | users API | Spring + React | users/credentials | API integration | M3 |
| FR-06 Realtime | SSE | Spring + React | events/devices | E2E smoke | M8/M9 |
| FR-07 History | attendance API | Spring + React | attendance | API/E2E | M8 |
| FR-08 Report | report API | Spring + React | attendance | reconciliation | M10 |
| FR-09 Settings | settings API | Spring + React | settings | integration | M8/M10 |
| FR-10 Alerts | notification adapter | Spring | alerts/deliveries | integration | M10 |
| FR-11 Error report | incident API/SSE | ESP32 + Spring + React | incidents | hardware E2E | M9 |
