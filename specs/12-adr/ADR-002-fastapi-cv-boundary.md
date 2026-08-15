# ADR-002: FastAPI CV Boundary

## Context
Facial recognition and liveness detection require Python-based computer vision and deep learning libraries (OpenCV, DeepFace). Spring Boot (Java) is not suited for these tasks.

## Decision
We will use FastAPI as an independent microservice dedicated purely to Computer Vision (CV) tasks. FastAPI will **never** write attendance records.

## Rationale
- FastAPI handles Python dependencies efficiently and provides high-performance asynchronous request handling.
- Restricting FastAPI to CV-only tasks enforces separation of concerns. It evaluates frames and returns a score/result, while Spring Boot decides the business outcome (e.g., late status, duplicate check).

## Consequences
- Requires maintaining two separate backend services.
- FastAPI must fetch the user's face profile directly from Supabase to maintain performance (under 5s timeout) without bottlenecking through Spring Boot for heavy data payloads.
