# RFID & Face Recognition Attendance System

An automated attendance system that combines **RFID card scanning** and **facial
recognition (with liveness)** for two-factor check-in. The system connects
ESP32 IoT hardware to a cloud database and a web dashboard for real-time
monitoring and management.

> Current status: the ESP32 firmware and web dashboard are fully working with
> the **RFID** flow. Face recognition (ESP32-CAM + AI service) is scaffolded
> and can be enabled later.

## Features

- **RFID check-in** — ESP32 + RC522 card reader resolves a card UID to a
  registered user and records attendance in real time.
- **Late detection** — configurable class start time and grace period; check-ins
  after the threshold are stored as `LATE`.
- **Only one check-in per day** — the first `IN` of a day is kept; later scans of
  the same card are ignored.
- **Help button** — a physical button on the device sends a help request that
  pops up on the dashboard.
- **Web card enrollment** — start enrollment from the web, scan the new card on
  the device, and the UID is automatically filled into the new-user form.
- **User management** — add / edit / delete users; deleting a user also removes
  their attendance and help logs.
- **History & export** — filter attendance by date / name / status and export CSV.
- **Live dashboard** — today's stats, per-hour check-in chart, recent activity,
  and help-request alerts (auto-refresh every 3 s).

## Architecture

```
+----------------+        HTTP        +-----------------+        SQL/JSON       +-----------+
|  ESP32 hardware | -----------------> |  Spring Boot API | -----------------> |  Supabase |
|  (RC522 + CAM)  | <----------------- |  (backend)       | <----------------- | (Postgres)|
+----------------+   JSON responses   +-----------------+   Realtime sync      +-----------+
                                         ^        |
                                    CORS |        | image payload (future)
                                         |        v
                                   +----------+  +------------+
                                   | Web dash |  | AI service |
                                   | (React)  |  | (FastAPI)  |
                                   +----------+  +------------+
```

- **backend/** — Spring Boot (Java 17) REST API + JPA. Port `8080`.
- **frontend/** — React + Vite admin dashboard. Dev port `5173`.
- **esp32-service/** — PlatformIO firmware for ESP32 + MFRC522 (and ESP32-CAM in
  the future).
- **ai-service/** — FastAPI image-analysis service (placeholder for face
  recognition / liveness). Port `8000`.

## Prerequisites

- Java 17+ and Maven (or use the bundled `mvnw` / `mvnw.cmd`).
- Node.js 18+ and npm.
- A Supabase (PostgreSQL) project.
- PlatformIO (CLI or VS Code extension) to build the ESP32 firmware.
- Python 3.10+ (only if you want to run the AI service).

## 1. Database setup (Supabase)

1. Create a project on [supabase.com](https://supabase.com).
2. Open **SQL Editor** and run the contents of
   [`backend/sql/schema.sql`](backend/sql/schema.sql). It creates:
   - `students` — user profiles with MSSV, full name, RFID UID, face encoding.
   - `attendance_logs` — check-ins (`IN` / `OUT` / `LATE`).
   - `assistance_requests` — help requests.
   - `system_settings` — late-threshold configuration.
3. Copy the **pooler connection string** from
   **Project Settings → Database → Connection string**.

> Note: use the **pooler host** (`<region>.pooler.supabase.com`), not the direct
> `db.<ref>.supabase.co` host — the direct host is often IPv6-only and may be
> unreachable from many networks.

## 2. Backend

```bash
cd backend
```

Copy the example config and fill in your Supabase values:

```bash
cp src/main/resources/application-local.example.properties src/main/resources/application-local.properties
# edit the datasource url / username / password
```

Run with the `local` profile (Windows PowerShell):

```powershell
$env:SPRING_PROFILES_ACTIVE = "local"
.\mvnw.cmd spring-boot:run
```

Or on macOS / Linux:

```bash
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
```

**Alternative (no extra file):** set the `DB_PASSWORD` environment variable and
run the default profile — `application.properties` already points at a pooler
URL and uses `${DB_PASSWORD}`.

The API listens on `http://localhost:8080`. Health check:
`http://localhost:8080/api/users`.

> **Pooler notes:** use the transaction pooler port `6543` and keep
> `prepareThreshold=0` in the JDBC URL — the transaction pooler does not support
> server-side prepared statements and will otherwise fail with
> `prepared statement "S_1" already exists`.

## 3. Frontend

```bash
cd frontend
npm install
cp .env.example .env      # adjust VITE_API_BASE_URL if needed
npm run dev
```

Open `http://localhost:5173`. Login-free for now; CORS is configured for
`localhost:5173` and `localhost:3000`.

Production build:

```bash
npm run build   # outputs to dist/
```

## 4. ESP32 firmware (RFID)

Install PlatformIO, then:

```bash
cd esp32-service
pio run --target upload
pio device monitor      # baud 115200
```

Edit **`src/main.cpp`** before flashing:

- `kWiFiSsid` / `kWiFiPassword` — your Wi-Fi / hotspot credentials.
- `kServerCandidates` — the backend URL(s), e.g. `http://192.168.111.185:8080/api`.
  The firmware probes each candidate and caches the first that answers.

### Pin mapping (ESP32 DevKit + RC522)

| Function        | GPIO |
| --------------- | ---- |
| RFID SS (SDA)   | 16   |
| RFID RST        | 17   |
| SPI SCK         | 18   |
| SPI MISO        | 19   |
| SPI MOSI        | 23   |
| Buzzer          | 25   |
| LED green       | 26   |
| LED red         | 27   |
| Help button     | 32   |

Feedback: success → green LED 2 s + 2 quick beeps; failure → red LED 2 s + long
beep. The button sends a help request to the dashboard.

## 5. AI service (optional / future)

```bash
cd ai-service
python -m venv .venv
.venv\Scripts\activate        # Windows
pip install -r requirements.txt
python -m app.main
```

Listens on `http://localhost:8000`; docs at `/docs`. The `/api/v1/analyze`
endpoint currently validates uploads and returns a placeholder — face matching
and liveness detection are not wired up yet.

## API overview

| Method | Path                            | Description                                  |
| ------ | ------------------------------- | -------------------------------------------- |
| GET    | `/api/users`                    | List users                                   |
| POST   | `/api/users`                    | Create user                                  |
| GET    | `/api/users/rfid/{uid}`         | Resolve user by RFID UID                     |
| PUT    | `/api/users/{id}`               | Update user                                  |
| DELETE | `/api/users/{id}`               | Delete user + their logs                     |
| GET    | `/api/attendance`               | All attendance records                       |
| POST   | `/api/attendance`               | Record attendance (query params)             |
| GET    | `/api/attendance/today`         | Today's records                              |
| GET    | `/api/attendance/between`       | Records between two ISO timestamps           |
| DELETE | `/api/attendance/{id}`          | Delete a single attendance record            |
| GET    | `/api/assistance`               | List help requests                           |
| POST   | `/api/assistance`               | Create help request                          |
| GET    | `/api/settings`                 | Attendance settings (start time, grace)      |
| PUT    | `/api/settings`                 | Update attendance settings                   |
| POST   | `/api/rfid-enrollment/start`    | Start card enrollment mode                   |
| POST   | `/api/rfid-enrollment/cancel`   | Cancel card enrollment                       |
| POST   | `/api/rfid-enrollment/scan`     | Submit scanned UID from the device           |

## Behavior notes

- **LATE threshold** — set under **Cài đặt (Settings)** on the web: class start
  time + allowed grace minutes. Check-ins in `Asia/Ho_Chi_Minh` after
  `start + grace` are stored as `LATE`.
- **First check-in wins** — for each user, only the first `IN` of a day is
  saved; later `IN` scans return the existing record.
- **Delete cascade** — deleting a user deletes their attendance and help logs.
- **Help button** — dashboard polls `/api/assistance` every 3 s and pops up an
  alert for new requests.

## Troubleshooting

- **`password authentication failed`** — the Supabase pooler password changed
  when the pool settings were edited. Copy the current connection string from
  the dashboard.
- **`prepared statement "S_1" already exists`** — the JDBC URL is missing
  `prepareThreshold=0` (required for the transaction pooler).
- **ESP32 `[NET] Candidate -> HTTP -1`** — the backend IP in `kServerCandidates`
  is stale or the backend is not running. Update the IP to match your machine on
  the hotspot.
- **ESP32 `[WIFI] Connection failed`** — the hotspot is 5 GHz only or the
  credentials are wrong; the ESP32 supports 2.4 GHz only.
