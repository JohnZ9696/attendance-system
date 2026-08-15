# ADR-001: Spring Boot as Main Backend

## Context
The attendance system requires a central orchestrator to manage business logic, database transactions, API requests from the frontend, and coordination between IoT devices and AI services.

## Decision
We will use Spring Boot as the main backend and API Gateway for the system. It will be the **sole owner** of attendance decisions and data writing.

## Rationale
- Spring Boot provides robust transaction management and security features out-of-the-box.
- Centralizing business logic (like late calculation and check-in constraints) in one place prevents split-brain scenarios.
- The web frontend (React) and IoT devices will interact exclusively with Spring Boot for business operations, ensuring a unified security boundary.

## Consequences
- Requires running a JVM-based server.
- AI tasks (which require Python/C++ libraries) cannot run efficiently directly within Spring Boot, necessitating a separate CV microservice.
