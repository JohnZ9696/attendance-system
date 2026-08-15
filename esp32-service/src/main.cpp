#include <Arduino.h>
#include <SPI.h>
#include <WiFi.h>
#include <HTTPClient.h>
#include <MFRC522.h>
#include <ArduinoJson.h>

// ============================================================================
// Pin Definitions (mandatory pinmap)
// ============================================================================
constexpr uint8_t RFID_SS_PIN   = 16;
constexpr uint8_t RFID_RST_PIN  = 17;
// Default ESP32 SPI (VSPI) pins
constexpr uint8_t SPI_SCK_PIN   = 18;
constexpr uint8_t SPI_MISO_PIN  = 19;
constexpr uint8_t SPI_MOSI_PIN  = 23;
constexpr uint8_t BUZZER_PIN    = 25;
constexpr uint8_t LED_GREEN_PIN = 26;
constexpr uint8_t LED_RED_PIN   = 27;
constexpr uint8_t BUTTON_PIN    = 32;  // INPUT_PULLUP: LOW = pressed

// ============================================================================
// Network configuration
// ============================================================================
constexpr char kWiFiSsid[]     = "Public APCS 4.2";  // phone hotspot SSID
constexpr char kWiFiPassword[] = "PublicApcs";

// Backend candidates, tried in order. The PC's hotspot IP can change between
// reconnects, so keep a small list and the firmware uses the first that answers.
constexpr char kServerCandidates[][40] = {
  "http://10.122.5.62:8080/api",  // PC on current hotspot
};
constexpr int  kServerCandidatesCount = sizeof(kServerCandidates) / sizeof(kServerCandidates[0]);
constexpr char kHelpMessage[]  = "Student needs help";

// ============================================================================
// Timing / Behavior constants
// ============================================================================
constexpr unsigned long LED_FEEDBACK_MS    = 2000UL;  // LED stays on 2 s
constexpr unsigned long BEEP_SHORT_MS      = 100UL;   // success beep length
constexpr unsigned long BEEP_GAP_MS        = 100UL;   // gap between success beeps
constexpr unsigned long BEEP_LONG_MS       = 1000UL;  // fail beep length
constexpr unsigned long BUTTON_DEBOUNCE_MS = 50UL;    // software debounce window
constexpr unsigned long CARD_COOLDOWN_MS   = 500UL;   // min gap between reads
constexpr unsigned long ENROLLMENT_POLL_MS = 1000UL;  // backend command polling
constexpr int           WIFI_TIMEOUT_S     = 15;      // max WiFi connect wait

// ============================================================================
// Global objects & state
// ============================================================================
MFRC522 rfid(RFID_SS_PIN, RFID_RST_PIN);

enum class FeedbackState : uint8_t { IDLE, SUCCESS, FAIL };
FeedbackState feedbackState = FeedbackState::IDLE;
unsigned long stateStartMs = 0UL;

// Button debounce state (pull-up => HIGH = released)
bool lastStableButtonState = HIGH;
bool lastButtonReading     = HIGH;
unsigned long lastButtonChangeMs = 0UL;

String lastScannedUid;  // last card UID, reused for help requests
bool enrollmentMode = false;
unsigned long lastEnrollmentPollMs = 0UL;

// ----------------------------------------------------------------------------
// Low-level peripheral helpers
// ----------------------------------------------------------------------------
void setBuzzer(bool on) { digitalWrite(BUZZER_PIN, on ? HIGH : LOW); }

void allOff() {
  digitalWrite(LED_GREEN_PIN, LOW);
  digitalWrite(LED_RED_PIN, LOW);
  setBuzzer(false);
}

// ----------------------------------------------------------------------------
// WiFi
// ----------------------------------------------------------------------------
bool connectToWifi() {
  if (WiFi.status() == WL_CONNECTED) {
    return true;
  }

  WiFi.persistent(false);
  WiFi.mode(WIFI_STA);
  WiFi.setSleep(false);  // phone hotspots often drop sleeping ESP32 stations
  WiFi.setAutoReconnect(true);
  WiFi.begin(kWiFiSsid, kWiFiPassword);

  Serial.print("[WIFI] Connecting");
  const unsigned long start = millis();
  while (WiFi.status() != WL_CONNECTED && (millis() - start) < (WIFI_TIMEOUT_S * 1000UL)) {
    delay(500);
    Serial.print('.');
  }
  Serial.println();

  if (WiFi.status() == WL_CONNECTED) {
    Serial.print("[WIFI] Connected, IP: ");
    Serial.println(WiFi.localIP());
    return true;
  }
  Serial.printf("[WIFI] Connection failed, status=%d\n", WiFi.status());
  return false;
}

// ----------------------------------------------------------------------------
// NTP time sync + ISO-8601 timestamp (UTC, "2026-08-13T03:29:25Z")
// ----------------------------------------------------------------------------
void syncTime() {
  if (WiFi.status() != WL_CONNECTED) {
    Serial.println("[NTP] Skipped: WiFi is offline");
    return;
  }
  configTime(0, 0, "pool.ntp.org", "time.nist.gov");  // UTC
  Serial.print("[NTP] Syncing");
  int attempts = 0;
  while (time(nullptr) < 100000L && attempts < 20) {
    delay(500);
    attempts++;
    Serial.print('.');
  }
  Serial.println();
}

String getIsoTimestamp() {
  time_t now = time(nullptr);
  struct tm t;
  gmtime_r(&now, &t);
  char buf[32];
  strftime(buf, sizeof buf, "%Y-%m-%dT%H:%M:%SZ", &t);
  return String(buf);
}

// ----------------------------------------------------------------------------
// HTTP helpers
// ----------------------------------------------------------------------------
String workingBase = "";  // cached base URL that responded successfully

// Find the first backend candidate that responds. Tries mDNS hostname first,
// then falls back to IPs, so it works no matter what IP the hotspot assigns.
String getApiBase() {
  if (workingBase.length() > 0) {
    return workingBase;
  }
  if (!connectToWifi()) {
    return "";
  }
  for (int i = 0; i < kServerCandidatesCount; i++) {
    HTTPClient http;
    http.begin(String(kServerCandidates[i]) + "/users");
    http.setTimeout(2500);
    const int code = http.GET();
    http.end();
    Serial.printf("[NET] Candidate %d -> HTTP %d\n", i, code);
    if (code == HTTP_CODE_OK) {
      workingBase = String(kServerCandidates[i]);
      Serial.print("[NET] Backend found: ");
      Serial.println(workingBase);
      return workingBase;
    }
  }
  return String(kServerCandidates[0]);  // last resort, try first anyway
}

String urlEncode(const String& value) {
  String out = "";
  const size_t len = value.length();
  for (size_t i = 0; i < len; i++) {
    const char c = value[i];
    if (isalnum(c) || c == '-' || c == '_' || c == '.' || c == '~') {
      out += c;
    } else {
      char buf[4];
      snprintf(buf, sizeof buf, "%%%02X", (unsigned char)c);
      out += buf;
    }
  }
  return out;
}

String httpGet(const String& url) {
  if (!connectToWifi()) {
    return "";
  }
  HTTPClient http;
  http.begin(url);
  http.setTimeout(5000);
  const int code = http.GET();
  String body = "";
  if (code == HTTP_CODE_OK) {
    body = http.getString();
  }
  Serial.printf("[HTTP] GET %d\n", code);
  http.end();
  return body;
}

// ----------------------------------------------------------------------------
// API calls
// ----------------------------------------------------------------------------
String resolveUserIdByRfid(const String& uid) {
  const String apiBase = getApiBase();
  if (apiBase.length() == 0) {
    return "";
  }
  const String body = httpGet(apiBase + "/users/rfid/" + uid);
  if (body.length() == 0) {
    return "";
  }
  JsonDocument doc;
  if (deserializeJson(doc, body)) {
    return "";
  }
  return doc["id"].as<String>();
}

bool sendAttendance(const String& uid) {
  const String userId = resolveUserIdByRfid(uid);
  if (userId.length() == 0) {
    Serial.println("[API] Card not registered");
    return false;
  }

  const String apiBase = getApiBase();
  if (apiBase.length() == 0) {
    return false;
  }
  const String url = apiBase + "/attendance?userId=" + userId +
                     "&checkInTime=" + getIsoTimestamp() +
                     "&status=IN&method=RFID";
  HTTPClient http;
  if (!connectToWifi()) {
    return false;
  }
  http.begin(url);
  http.setTimeout(5000);
  const int code = http.POST("");  // POST with query params
  http.end();
  return code == HTTP_CODE_OK;
}

bool sendHelpRequest(const String& uid) {
  const String userId = uid.length() > 0 ? resolveUserIdByRfid(uid) : "";
  const String apiBase = getApiBase();
  if (apiBase.length() == 0) {
    return false;
  }
  String url = apiBase + "/assistance?message=" + urlEncode(kHelpMessage);
  if (userId.length() > 0) {
    url += "&userId=" + userId;
  }
  HTTPClient http;
  if (!connectToWifi()) {
    return false;
  }
  http.begin(url);
  http.setTimeout(5000);
  const int code = http.POST("");  // POST with query params
  http.end();
  return code == HTTP_CODE_OK;
}

void pollEnrollmentCommand() {
  if (millis() - lastEnrollmentPollMs < ENROLLMENT_POLL_MS || WiFi.status() != WL_CONNECTED) {
    return;
  }
  lastEnrollmentPollMs = millis();

  const String apiBase = getApiBase();
  if (apiBase.length() == 0) {
    return;
  }

  HTTPClient http;
  http.begin(apiBase + "/rfid-enrollment");
  http.setTimeout(2000);
  const int code = http.GET();
  if (code == HTTP_CODE_OK) {
    JsonDocument doc;
    if (!deserializeJson(doc, http.getString())) {
      const bool waiting = doc["status"].as<String>() == "WAITING";
      if (waiting != enrollmentMode) {
        enrollmentMode = waiting;
        Serial.println(enrollmentMode
          ? "[ENROLLMENT] Ready - scan the new card"
          : "[ENROLLMENT] Cancelled or completed");
      }
    }
  }
  http.end();
}

bool submitEnrollmentUid(const String& uid) {
  const String apiBase = getApiBase();
  if (apiBase.length() == 0 || !connectToWifi()) {
    return false;
  }

  HTTPClient http;
  http.begin(apiBase + "/rfid-enrollment/scan?uid=" + urlEncode(uid));
  http.setTimeout(5000);
  const int code = http.POST("");
  http.end();
  return code == HTTP_CODE_OK;
}

// ----------------------------------------------------------------------------
// Feedback state machine (non-blocking, driven from loop())
//   SUCCESS -> Green LED 2 s + 2 quick beeps (100 ms each)
//   FAIL    -> Red   LED 2 s + 1 long  beep (1 s)
// ----------------------------------------------------------------------------
void startFeedback(FeedbackState state) {
  feedbackState = state;
  stateStartMs  = millis();
  allOff();
}

void updateFeedback() {
  if (feedbackState == FeedbackState::IDLE) {
    return;
  }

  const unsigned long elapsed = millis() - stateStartMs;

  switch (feedbackState) {
    case FeedbackState::SUCCESS: {
      digitalWrite(LED_GREEN_PIN, HIGH);
      if (elapsed < BEEP_SHORT_MS) {
        setBuzzer(true);                                // beep 1
      } else if (elapsed < BEEP_SHORT_MS + BEEP_GAP_MS) {
        setBuzzer(false);
      } else if (elapsed < 2UL * BEEP_SHORT_MS + BEEP_GAP_MS) {
        setBuzzer(true);                                // beep 2
      } else {
        setBuzzer(false);
      }
      if (elapsed >= LED_FEEDBACK_MS) {
        allOff();
        feedbackState = FeedbackState::IDLE;
      }
      break;
    }

    case FeedbackState::FAIL: {
      digitalWrite(LED_RED_PIN, HIGH);
      setBuzzer(elapsed < BEEP_LONG_MS);
      if (elapsed >= LED_FEEDBACK_MS) {
        allOff();
        feedbackState = FeedbackState::IDLE;
      }
      break;
    }

    default:
      break;
  }
}

// ----------------------------------------------------------------------------
// RFID: read a card UID as an uppercase hex string, or "" if none present
// ----------------------------------------------------------------------------
String readCardUid() {
  if (!rfid.PICC_IsNewCardPresent()) {
    return "";
  }
  if (!rfid.PICC_ReadCardSerial()) {
    return "";
  }

  String uid = "";
  for (byte i = 0; i < rfid.uid.size; i++) {
    if (rfid.uid.uidByte[i] < 0x10) {
      uid += "0";
    }
    uid += String(rfid.uid.uidByte[i], HEX);
  }
  uid.toUpperCase();
  return uid;
}

// ----------------------------------------------------------------------------
// Handle a detected card: read UID, call API, trigger feedback
// ----------------------------------------------------------------------------
void handleRfidScan() {
  if (feedbackState != FeedbackState::IDLE) {
    return;  // ignore new scans while feedback is running
  }

  static unsigned long lastScanMs = 0UL;
  if (millis() - lastScanMs < CARD_COOLDOWN_MS) {
    return;
  }
  lastScanMs = millis();

  const String uid = readCardUid();
  if (uid.length() == 0) {
    return;
  }

  rfid.PICC_HaltA();
  rfid.PCD_StopCrypto1();

  lastScannedUid = uid;
  Serial.print("[SCAN] UID=");
  Serial.println(uid);

  if (enrollmentMode) {
    if (submitEnrollmentUid(uid)) {
      enrollmentMode = false;
      Serial.println("[ENROLLMENT] UID sent to web form");
      startFeedback(FeedbackState::SUCCESS);
    } else {
      Serial.println("[ENROLLMENT] Failed to send UID");
      startFeedback(FeedbackState::FAIL);
    }
    return;
  }

  if (sendAttendance(uid)) {
    Serial.println("[SCAN] Registered card -> attendance recorded, Green LED + 2 beeps");
    startFeedback(FeedbackState::SUCCESS);
  } else {
    Serial.println("[SCAN] Unregistered card -> no record, Red LED + long beep");
    startFeedback(FeedbackState::FAIL);
  }
}

// ----------------------------------------------------------------------------
// Button with software debounce (INPUT_PULLUP: LOW = pressed)
// ----------------------------------------------------------------------------
bool isButtonPressedDebounced() {
  const bool reading = digitalRead(BUTTON_PIN);

  if (reading != lastButtonReading) {
    lastButtonChangeMs = millis();
  }
  lastButtonReading = reading;

  if ((millis() - lastButtonChangeMs) > BUTTON_DEBOUNCE_MS &&
      reading != lastStableButtonState) {
    lastStableButtonState = reading;
    return reading == LOW;  // true only on the press edge
  }
  return false;
}

void handleButton() {
  if (!isButtonPressedDebounced()) {
    return;
  }

  Serial.println("Button pressed -> Sending help request to manager");
  if (sendHelpRequest(lastScannedUid)) {
    Serial.println("[HELP] Sent successfully");
    startFeedback(FeedbackState::SUCCESS);
  } else {
    Serial.println("[HELP] Failed to send");
    startFeedback(FeedbackState::FAIL);
  }
}

// ============================================================================
// Setup / Loop
// ============================================================================
void setup() {
  Serial.begin(115200);
  delay(500);

  pinMode(BUZZER_PIN, OUTPUT);
  pinMode(LED_GREEN_PIN, OUTPUT);
  pinMode(LED_RED_PIN, OUTPUT);
  pinMode(BUTTON_PIN, INPUT_PULLUP);

  allOff();

  connectToWifi();
  syncTime();

  SPI.begin(SPI_SCK_PIN, SPI_MISO_PIN, SPI_MOSI_PIN, RFID_SS_PIN);
  rfid.PCD_Init();

  Serial.println("RFID Attendance System ready (no camera mode)");
}

void loop() {
  handleButton();
  pollEnrollmentCommand();
  handleRfidScan();
  updateFeedback();
}
