# Decisions and Open Questions

## Locked Decisions

| ID | Decision |
|---|---|
| DEC-01 | System only records check-in, no check-out. |
| DEC-02 | Maximum one check-in per user per business day. |
| DEC-03 | Spring Boot is the main backend and sole decider for attendance. |
| DEC-04 | React only communicates with Spring Boot. |
| DEC-05 | ESP32 + RC522 communicates with Spring Boot to authenticate UID. |
| DEC-06 | ESP32-CAM streams frames directly to FastAPI. |
| DEC-07 | FastAPI reads face data directly from Supabase for matching. |
| DEC-08 | FastAPI only returns CV results, it does not write attendance. |
| DEC-09 | UID determines the expected user; FastAPI only compares against that specific user's face profile. |
| DEC-10 | Similarity below 30% is a fail; >= 30% is considered a pass. Configurable. |
| DEC-11 | Blink liveness is required to pass. |
| DEC-12 | Matching timeout is 5 seconds after a valid frame; capture/liveness window is 10 seconds. |
| DEC-13 | Late is strictly after 07:30:00 Asia/Ho_Chi_Minh. Exactly 07:30:00 is on time. |
| DEC-14 | Website is only for logged-in proctors. PROCTOR can view; LEAD_PROCTOR can edit. |
| DEC-15 | ESP32 push button creates a hardware help request incident on the website. |
| DEC-16 | No complex queue/idempotency/retry for this version. |
| DEC-17 | No separate face policy document, but technical baselines must be maintained. |
| DEC-18 | No unit tests; integration, contract, smoke, and E2E tests are required. |

## Mentor Corrections

1. **Daily Check-in Unique Constraint:** DEC-02 dictates a single check-in per day. The database must enforce this with a `UNIQUE (student_id, attendance_date)` constraint.
2. **Timeout Split:** A single 5s timeout is insufficient. We have a 10s capture/liveness window and a 5s matching timeout after a frame is acquired.
3. **Similarity Percentage:** The 30% threshold implies CV tools returning a distance metric must normalize it to a 0..100 percentage.

## Open Questions

- What specific email/SMS provider should be used for notifications? Currently unspecified, relying on generic adapter.
- Should there be an automated cleanup for resolved assistance requests over time?
