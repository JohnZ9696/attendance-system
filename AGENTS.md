# Production SDD Working Agreement

## Architecture
- Spring Boot is the main backend and only owner of attendance decisions.
- React communicates only with Spring Boot.
- ESP32 RFID and push-button events go to Spring Boot.
- ESP32-CAM frames go directly to FastAPI.
- FastAPI reads face profiles from Supabase and returns CV results to Spring Boot.
- FastAPI must never write attendance.

## Locked business rules
- Check-in only; no check-out.
- At most one attendance record per user per business date.
- Database unique (user_id, attendance_date) is mandatory.
- Late means server-local Asia/Ho_Chi_Minh time strictly after 07:30:00.
- Similarity below 30% fails; 30% or above may pass only with liveness.
- Capture/liveness window is 10s; matching timeout after a valid frame is 5s.

## Testing
- Do not create unit tests unless explicitly requested later.
- Contract, integration, smoke, database-constraint, and E2E tests are mandatory.
- Stop and repair failures before moving to the next milestone.

## Production constraints
- No secrets in code, logs, or React bundles.
- Do not expose Supabase service-role credentials to clients.
- Do not log face images or embeddings.
- Do not change architecture/business rules without updating specs and ADR.
- Keep traceability from FR to contract, implementation, and verification.
