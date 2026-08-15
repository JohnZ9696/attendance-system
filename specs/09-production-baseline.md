# Production Baseline

## 1. Security Requirements
- **No Secrets in Code:** Service keys, passwords, and tokens must not be committed to the repository. Use environment variables.
- **Supabase Credentials:** Keep the Supabase service-role key exclusively in the Spring Boot and FastAPI environment. Do not include it in the React build.
- **Data Privacy:** Do not expose the face bucket/profiles to public anonymous access. Do not log base64 images, embeddings, passwords, tokens, or service keys.
- **Service-to-Service Auth:** Validate requests between Spring Boot and FastAPI.
- **CORS:** Restrict CORS only to the configured website origin.
- **Rate Limiting:** Implement basic rate limiting for login, RFID scans, frame uploads, and button endpoints.

## 2. Image Processing Safety
- Validate image size and content type before FastAPI processes the image to prevent overload or malicious payloads.

## 3. Access Control
- Proctors must be logged in; tokens must have an expiration.
- Only LEAD_PROCTOR can manage users, settings, and schedules. PROCTOR can only view.
