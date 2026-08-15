# ADR-004: Single Daily Check-In

## Context
The system needs to manage how often a student is recorded as attending. Initial requirements had ambiguities regarding check-out and multiple scans.

## Decision
The system will only record a **single check-in per user per business day**. There is no check-out. This rule will be strictly enforced at the database level using a `UNIQUE (student_id, attendance_date)` constraint.

## Rationale
- Simplifies the business logic and user experience (no need to track state).
- The unique constraint prevents race conditions where simultaneous hardware inputs could bypass application-level checks and create duplicate records.
- Repeated valid scans on the same day will yield an `ALREADY_CHECKED_IN` error, not a 500 error or a duplicate entry.

## Consequences
- Requires dropping any previous `status = 'OUT'` or duplicate check-in records during database migration.
- Spring Boot must catch the unique constraint violation exception and map it gracefully to the `ALREADY_CHECKED_IN` event.
