# Attendance System — Features

## 1. Backend API (Spring Boot, port 8080)

### 1.1 User Management — `/api/users`
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/users` | List all users |
| GET | `/api/users/{id}` | Get user by UUID |
| GET | `/api/users/rfid/{rfidUid}` | Look up user by RFID card UID |
| POST | `/api/users` | Create user (name, mssv, rfidUid, faceEmbedding) |
| PUT | `/api/users/{id}` | Update user fields |
| DELETE | `/api/users/{id}` | Delete user + cascade delete attendance & assistance records |

### 1.2 Attendance Recording — `/api/attendance`
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/attendance` | List all attendance records |
| GET | `/api/attendance/today` | Today's records |
| GET | `/api/attendance/user/{userId}` | Records for a specific user |
| GET | `/api/attendance/between?start=&end=` | Records in an ISO date range |
| POST | `/api/attendance?userId=&checkInTime=&status=&method=` | Record check-in |
| DELETE | `/api/attendance/{id}` | Delete a single record |

**Rules:**
- Timezone: `Asia/Ho_Chi_Minh`
- First-IN-per-day: only first `IN` record per user per calendar day is saved; subsequent attempts return the existing record.
- Auto-LATE: if check-in is after `classStartTime + graceMinutes`, status is overridden to `LATE`.

### 1.3 Assistance Requests — `/api/assistance`
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/assistance` | List all assistance requests |
| POST | `/api/assistance?userId=&message=` | Create help request (userId optional, supports anonymous) |

### 1.4 RFID Enrollment — `/api/rfid-enrollment`
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/rfid-enrollment/start` | Start enrollment (state → WAITING) |
| GET | `/api/rfid-enrollment` | Get current state (IDLE / WAITING / SCANNED + uid) |
| POST | `/api/rfid-enrollment/cancel` | Cancel enrollment |
| POST | `/api/rfid-enrollment/scan?uid=` | Submit scanned UID from ESP32 |

### 1.5 System Settings — `/api/settings`
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/settings` | Get attendance settings |
| PUT | `/api/settings` | Update attendance settings |

**Settings stored in `system_settings` table:**
- `attendance.classStartTime` — default `07:30`
- `attendance.lateGraceMinutes` — default `15`
- `attendance.dayOffs` — comma-separated ISO dates (specific days off)
- `attendance.weeklyDayOffs` — comma-separated day-of-week numbers (0=Sun…6=Sat), default `0,6`

---

## 2. Frontend Pages (React + Vite)

### 2.1 Dashboard (`/`)
- 4 stat cards: total users, checked-in, late, not-yet-checked-in
- Per-hour check-in area chart (on-time vs late by hour)
- Recent activity feed (last 6 check-ins)
- Auto-refresh every 3 seconds
- Error banner with retry

### 2.2 Users (`/users`)
- User table with search (name, MSSV, RFID)
- Create / edit user panel
- Delete user with confirmation (cascades to logs)
- RFID enrollment flow: modal → start enrolment → ESP32 scans → UID auto-fills form
- User detail dialog:
  - Attendance history table
  - Absence calculation: configurable range (this month / 7 days / 30 days)
  - Excludes weekly day-offs and special day-offs (from settings)
  - Stat chips: present, late, absent count
  - Absent day list (Vietnamese locale)

### 2.3 History (`/history`)
- Date picker filter
- Keyword search (name, MSSV)
- Status filter (ALL / IN / OUT / LATE)
- Attendance table with delete button
- Auto-refresh every 3 seconds
- CSV export (UTF-8 BOM, headers: Time, MSSV, Name, Method, Status)

### 2.4 Monitoring (`/monitoring`)
- ESP32-CAM stream placeholder
- Device status cards: ESP32 Gateway, ESP32-CAM, RFID Reader
- Real-time log panel (simulated data)

### 2.5 Reports (`/reports`)
- Weekly stacked bar chart (present, late, absent per weekday)
- Today's donut chart (on-time, late, absent percentages)
- Uses Recharts

### 2.6 Settings (`/settings`)
- Class start time + grace period with live late-threshold display
- Weekly day-off toggle chips (T2–CN)
- Interactive calendar: month navigation, click to toggle day-offs, today highlight
- Selected day-off list with remove buttons
- Save button persists all settings
- Notification/SMTP config (mock)
- System info panel (mock: firmware version, DB status, liveness)

### 2.7 Support (`/support`)
- Issue report form (type, description, device)
- Report history sidebar (mock entries with status badges)

---

## 3. ESP32 Firmware (C++, MFRC522)

### Hardware
- RFID RC522: SS=16, RST=17, SCK=18, MISO=19, MOSI=23
- Buzzer=25, Green LED=26, Red LED=27, Help button=32 (INPUT_PULLUP)

### Network
- WiFi station mode (SSID/password configurable)
- Auto-backend discovery: probes candidate URLs, caches the first responding one
- WiFi auto-reconnect, 15s connection timeout

### Card Handling
- MFRC522 UID reading (uppercase hex)
- 500ms debounce cooldown
- **Attendance mode**: scan → resolve user → POST attendance
- **Enrollment mode**: poll backend state → scan → POST UID

### Help Button
- Physical button with 50ms debounce
- POSTs assistance request (with userId if a card was previously scanned)

### Time Sync
- NTP from `pool.ntp.org` / `time.nist.gov`
- ISO-8601 UTC timestamp generation

### Visual / Audio Feedback
- **Success**: green LED 2s + 2 quick beeps
- **Failure**: red LED 2s + 1 long beep
- Non-blocking state machine (ignores scans while animating)

### Enrollment Polling
- Polls `GET /api/rfid-enrollment` every 1s
- Enters/exits enrollment mode based on backend state

---

## 4. Database (PostgreSQL / Supabase)

### `students`
| Column | Type | Notes |
|--------|------|-------|
| id | uuid | PK, `gen_random_uuid()` |
| mssv | text | UNIQUE, NOT NULL |
| full_name | text | NOT NULL |
| uid | text | UNIQUE, RFID card UID |
| face_encoding | jsonb | nullable |
| created_at | timestamptz | default `now()` |

### `attendance_logs`
| Column | Type | Notes |
|--------|------|-------|
| id | uuid | PK |
| student_id | uuid | FK → students(id), NOT NULL |
| check_time | timestamptz | NOT NULL |
| method | text | CHECK ('RFID','FACE') |
| status | text | CHECK ('IN','OUT','LATE') |

### `assistance_requests`
| Column | Type | Notes |
|--------|------|-------|
| id | uuid | PK |
| student_id | uuid | FK → students(id), nullable |
| message | text | NOT NULL |
| created_at | timestamptz | default `now()` |

### `system_settings`
| Column | Type | Notes |
|--------|------|-------|
| key | text | PK |
| value | text | NOT NULL |
| updated_at | timestamptz | default `now()` |

### Indexes
- `idx_attendance_student` on `attendance_logs(student_id)`
- `idx_attendance_check_time` on `attendance_logs(check_time)`
- `idx_assistance_student` on `assistance_requests(student_id)`

---

## 5. Architecture

```
ESP32 (RC522 + button + LEDs/buzzer)
  │
  │ HTTP REST (JSON)
  ▼
Spring Boot Backend (port 8080)
  │
  │ JPA / JDBC (Supabase pooler: 6543, prepareThreshold=0)
  ▼
Supabase PostgreSQL
  ▲
  │
  │ HTTP
  │
React Frontend (Vite, port 5173)
  └── AI Service (FastAPI, port 8000 — scaffold only)
```