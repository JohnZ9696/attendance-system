# ADR-003: ESP32-CAM Frame Flow

## Context
The ESP32-CAM captures images for facial verification. These images need to be processed by the CV service (FastAPI).

## Decision
The ESP32-CAM will send frames **directly** to the FastAPI service, bypassing Spring Boot.

## Rationale
- Routing heavy JPEG frames through Spring Boot just to forward them to FastAPI creates unnecessary network congestion and memory overhead on the main backend.
- Direct streaming to FastAPI minimizes latency, crucial for the 10s capture/liveness window and 5s matching timeout.

## Consequences
- Spring Boot must coordinate a "session" (verification request) with FastAPI before FastAPI begins processing frames for a given camera.
- ESP32-CAM needs network access to FastAPI directly.
