# Product Specification

## 1. Overview
The Attendance System uses RFID and Facial Recognition to automate attendance for schools and businesses. It features a dual-layer security check (RFID + Face) to prevent fraud and proxy attendance.

## 2. Functional Requirements (FR)

| Code | Function | Description |
|---|---|---|
| FR-01 | Wi-Fi Connectivity | ESP32 connects to Wi-Fi and shows status. |
| FR-02 | RFID Authentication | Read UID, check validity, and record scan attempt. |
| FR-03 | Facial Authentication | Detect face, compare with database, return result, blink liveness. |
| FR-04 | Cloud Storage | Store attendance, verification logs, and incident reports on Supabase. |
| FR-05 | User Management (Web) | Add, edit, delete users; enroll faces and RFID tags. Lead Proctor only. |
| FR-06 | Realtime Monitoring (Web) | View live camera, ESP32 status, and live attendance via SSE. |
| FR-07 | Attendance History (Web) | Search and filter past attendance records. |
| FR-08 | Reports & Stats (Web) | Display charts, late statistics, and export to Excel. |
| FR-09 | System Settings (Web) | Configure start time (07:30 cutoff), late time, and thresholds. Lead Proctor only. |
| FR-10 | Notifications | Send email, SMS, and buzzer alerts on events. |
| FR-11 | Hardware Help Button | Push button on ESP32 sends a hardware help request to the web dashboard. |

## 3. Scope Adjustments (SDD Alignment)
- Check-in only. No check-out.
- One check-in per day per user.
- Late is strictly after 07:30:00 (Asia/Ho_Chi_Minh).
- Website access is restricted to PROCTOR and LEAD_PROCTOR roles. Students do not access the web interface.
