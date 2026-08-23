#include <Arduino.h>
#include <SPI.h>
#include <WiFi.h>
#include <HTTPClient.h>
#include <MFRC522.h>
#include <ArduinoJson.h>
#include <fstream>

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

#include "wifi_config.h"

// ============================================================================
// Configuration
// ============================================================================
constexpr char kWiFiSsid[]     = WIFI_SSID;
constexpr char kWiFiPassword[] = WIFI_PASSWORD;
constexpr char kDeviceId[]     = "door-01";

constexpr char kServerCandidates[][40] = {
  "http://192.168.1.184:8080/api/v1",
};
constexpr int kServerCandidatesCount = sizeof(kServerCandidates) / sizeof(kServerCandidates[0]);

// ============================================================================
// Timing / Behavior constants
// ============================================================================
constexpr unsigned long BUTTON_DEBOUNCE_MS = 50UL;    // software debounce window
constexpr unsigned long BUTTON_COOLDOWN_MS = 2000UL;  // min gap between help button presses
constexpr unsigned long CARD_COOLDOWN_MS   = 500UL;   // min gap between reads
constexpr unsigned long ENROLLMENT_POLL_MS = 1000UL;  // backend command polling
constexpr unsigned long HEARTBEAT_INTERVAL_MS = 60000UL; // Heartbeat interval
constexpr int           WIFI_TIMEOUT_S     = 15;      // max WiFi connect wait

// ============================================================================
// Global objects & state
// ============================================================================
MFRC522 rfid(RFID_SS_PIN, RFID_RST_PIN);

enum class FeedbackState : uint8_t { 
  IDLE, 
  RFID_INVALID,
  CAMERA_OFFLINE,
  CAPTURE_TIMEOUT,
  LIVENESS_FAILED,
  FACE_BELOW_THRESHOLD,
  FACE_MATCH_TIMEOUT,
  ALREADY_CHECKED_IN,
  CHECK_IN_ON_TIME,
  CHECK_IN_LATE,
  CLOUD_WRITE_FAILED,
  INCIDENT_RECORDED
};
void startFeedback(FeedbackState state);
FeedbackState feedbackState = FeedbackState::IDLE;
unsigned long stateStartMs = 0UL;

bool lastStableButtonState = HIGH;
bool lastButtonReading     = HIGH;
unsigned long lastButtonChangeMs = 0UL;
unsigned long lastButtonPressMs  = 0UL;
unsigned long lastHeartbeatMs    = 0UL;

String lastScannedUid;
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
// WiFi & NTP
// ----------------------------------------------------------------------------
bool connectToWifi() {
  if (WiFi.status() == WL_CONNECTED) return true;

  WiFi.persistent(false);
  WiFi.mode(WIFI_STA);
  WiFi.setSleep(false);
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
  return false;
}

void syncTime() {
  if (WiFi.status() != WL_CONNECTED) return;
  configTime(0, 0, "pool.ntp.org", "time.nist.gov");
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
String workingBase = "";

String getApiBase() {
  if (workingBase.length() > 0) return workingBase;
  if (!connectToWifi()) return "";
  
  // In a real scenario, we might test candidates here.
  // For simplicity, we just use the first.
  workingBase = String(kServerCandidates[0]);
  return workingBase;
}

// ----------------------------------------------------------------------------
// API calls
// ----------------------------------------------------------------------------

void sendHeartbeat() {
  if (millis() - lastHeartbeatMs < HEARTBEAT_INTERVAL_MS) return;
  lastHeartbeatMs = millis();
  
  const String apiBase = getApiBase();
  if (apiBase.length() == 0 || !connectToWifi()) return;

  HTTPClient http;
  String url = apiBase + "/devices/" + kDeviceId + "/heartbeat";
  http.begin(url);
  http.setTimeout(3000);
  int code = http.POST("");
  http.end();
}

void sendHelpRequest() {
  const String apiBase = getApiBase();
  if (apiBase.length() == 0 || !connectToWifi()) {
    startFeedback(FeedbackState::CLOUD_WRITE_FAILED);
    return;
  }

  HTTPClient http;
  String url = apiBase + "/devices/" + kDeviceId + "/incidents";
  http.begin(url);
  http.addHeader("Content-Type", "application/json");

  StaticJsonDocument<200> doc;
  doc["deviceId"] = kDeviceId;
  doc["type"] = "HARDWARE_HELP_REQUEST";
  doc["source"] = "PUSH_BUTTON";
  doc["occurredAt"] = getIsoTimestamp();

  String payload;
  serializeJson(doc, payload);

  http.setTimeout(5000);
  int code = http.POST(payload);
  http.end();

  if (code == 200 || code == 201) {
    startFeedback(FeedbackState::INCIDENT_RECORDED);
  } else {
    startFeedback(FeedbackState::CLOUD_WRITE_FAILED);
  }
}

void pollEnrollmentCommand() {
  if (millis() - lastEnrollmentPollMs < ENROLLMENT_POLL_MS || WiFi.status() != WL_CONNECTED) return;
  lastEnrollmentPollMs = millis();

  const String apiBase = getApiBase();
  if (apiBase.length() == 0) return;

  HTTPClient http;
  http.begin(apiBase + "/rfid-enrollment");
  http.setTimeout(2000);
  int code = http.GET();
  if (code == HTTP_CODE_OK) {
    StaticJsonDocument<200> doc;
    if (!deserializeJson(doc, http.getString())) {
      bool waiting = doc["status"].as<String>() == "WAITING";
      if (waiting != enrollmentMode) {
        enrollmentMode = waiting;
        Serial.println(enrollmentMode ? "[ENROLLMENT] Ready" : "[ENROLLMENT] Cancelled/completed");
      }
    }
  }
  http.end();
}

bool submitEnrollmentUid(const String& uid) {
  const String apiBase = getApiBase();
  if (apiBase.length() == 0 || !connectToWifi()) return false;

  HTTPClient http;
  http.begin(apiBase + "/rfid-enrollment/scan");
  http.addHeader("Content-Type", "application/json");
  
  StaticJsonDocument<200> doc;
  doc["uid"] = uid;
  String payload;
  serializeJson(doc, payload);

  http.setTimeout(5000);
  int code = http.POST(payload);
  http.end();
  return code == HTTP_CODE_OK;
}

void sendRfidScan(const String& uid) {
  const String apiBase = getApiBase();
  if (apiBase.length() == 0 || !connectToWifi()) {
    startFeedback(FeedbackState::CLOUD_WRITE_FAILED);
    return;
  }

  HTTPClient http;
  String url = apiBase + "/devices/" + kDeviceId + "/rfid-scans";
  http.begin(url);
  http.addHeader("Content-Type", "application/json");

  StaticJsonDocument<200> doc;
  doc["uid"] = uid;
  doc["deviceId"] = kDeviceId;
  doc["scannedAt"] = getIsoTimestamp();

  String payload;
  serializeJson(doc, payload);

  http.setTimeout(50000); // Allow time for face match
  int code = http.POST(payload);
  
  if (code > 0) {
    StaticJsonDocument<200> resp;
    deserializeJson(resp, http.getString());
    String errorCode = resp["errorCode"].as<String>();
    
    if (code == 200 || code == 201) {
      String status = resp["status"].as<String>();
      if (status == "ON_TIME") startFeedback(FeedbackState::CHECK_IN_ON_TIME);
      else if (status == "LATE") startFeedback(FeedbackState::CHECK_IN_LATE);
      else startFeedback(FeedbackState::CHECK_IN_ON_TIME);
    } else {
      if (errorCode == "RFID_INVALID") startFeedback(FeedbackState::RFID_INVALID);
      else if (errorCode == "CAMERA_OFFLINE") startFeedback(FeedbackState::CAMERA_OFFLINE);
      else if (errorCode == "CAPTURE_TIMEOUT") startFeedback(FeedbackState::CAPTURE_TIMEOUT);
      else if (errorCode == "LIVENESS_FAILED") startFeedback(FeedbackState::LIVENESS_FAILED);
      else if (errorCode == "FACE_BELOW_THRESHOLD") startFeedback(FeedbackState::FACE_BELOW_THRESHOLD);
      else if (errorCode == "FACE_MATCH_TIMEOUT") startFeedback(FeedbackState::FACE_MATCH_TIMEOUT);
      else if (errorCode == "ALREADY_CHECKED_IN") startFeedback(FeedbackState::ALREADY_CHECKED_IN);
      else startFeedback(FeedbackState::CLOUD_WRITE_FAILED);
    }
  } else {
    startFeedback(FeedbackState::CLOUD_WRITE_FAILED);
  }
  http.end();
}

// ----------------------------------------------------------------------------
// Feedback state machine
// ----------------------------------------------------------------------------
void startFeedback(FeedbackState state) {
  feedbackState = state;
  stateStartMs  = millis();
  allOff();
}

void updateFeedback() {
  if (feedbackState == FeedbackState::IDLE) return;
  const unsigned long elapsed = millis() - stateStartMs;

  bool r_led = false, g_led = false, buzz = false;
  unsigned long duration = 2000UL; // default 2s

  switch (feedbackState) {
    case FeedbackState::RFID_INVALID:
      // red LED + short beep (100ms)
      r_led = true;
      buzz = (elapsed < 100);
      break;
    case FeedbackState::CAMERA_OFFLINE:
    case FeedbackState::CAPTURE_TIMEOUT:
    case FeedbackState::LIVENESS_FAILED:
    case FeedbackState::FACE_MATCH_TIMEOUT:
      // red LED + 2 beeps (100ms on, 100ms off, 100ms on)
      r_led = true;
      buzz = (elapsed < 100) || (elapsed >= 200 && elapsed < 300);
      break;
    case FeedbackState::FACE_BELOW_THRESHOLD:
      // red LED + long beep (1000ms)
      r_led = true;
      buzz = (elapsed < 1000);
      break;
    case FeedbackState::ALREADY_CHECKED_IN:
      // green blink / special pattern
      g_led = (elapsed % 500) < 250;
      duration = 2000UL;
      break;
    case FeedbackState::CHECK_IN_ON_TIME:
      // green LED + short beep
      g_led = true;
      buzz = (elapsed < 100);
      break;
    case FeedbackState::CHECK_IN_LATE:
      // green LED + 2 short beeps
      g_led = true;
      buzz = (elapsed < 100) || (elapsed >= 200 && elapsed < 300);
      break;
    case FeedbackState::CLOUD_WRITE_FAILED:
      // red LED + error beep
      r_led = true;
      buzz = (elapsed < 500); // 500ms beep
      break;
    case FeedbackState::INCIDENT_RECORDED:
      // confirm LED
      g_led = true;
      buzz = (elapsed < 100);
      duration = 1000UL;
      break;
    default:
      break;
  }

  digitalWrite(LED_RED_PIN, r_led ? HIGH : LOW);
  digitalWrite(LED_GREEN_PIN, g_led ? HIGH : LOW);
  setBuzzer(buzz);

  if (elapsed >= duration) {
    allOff();
    feedbackState = FeedbackState::IDLE;
  }
}

// ----------------------------------------------------------------------------
// RFID
// ----------------------------------------------------------------------------
String readCardUid() {
  if (!rfid.PICC_IsNewCardPresent() || !rfid.PICC_ReadCardSerial()) return "";
  String uid = "";
  for (byte i = 0; i < rfid.uid.size; i++) {
    if (rfid.uid.uidByte[i] < 0x10) uid += "0";
    uid += String(rfid.uid.uidByte[i], HEX);
  }
  uid.toUpperCase();
  return uid;
}

void handleRfidScan() {
  if (feedbackState != FeedbackState::IDLE) return;
  static unsigned long lastScanMs = 0UL;
  if (millis() - lastScanMs < CARD_COOLDOWN_MS) return;

  const String uid = readCardUid();
  if (uid.length() == 0) return;
  lastScanMs = millis();

  rfid.PICC_HaltA();
  rfid.PCD_StopCrypto1();

  lastScannedUid = uid;
  Serial.print("[SCAN] UID=");
  Serial.println(uid);

  if (enrollmentMode) {
    if (submitEnrollmentUid(uid)) {
      enrollmentMode = false;
      startFeedback(FeedbackState::CHECK_IN_ON_TIME);
    } else {
      startFeedback(FeedbackState::CLOUD_WRITE_FAILED);
    }
    return;
  }

  sendRfidScan(uid);
}

// ----------------------------------------------------------------------------
// Button
// ----------------------------------------------------------------------------
void handleButton() {
  const bool reading = digitalRead(BUTTON_PIN);
  if (reading != lastButtonReading) {
    lastButtonChangeMs = millis();
  }
  lastButtonReading = reading;

  if ((millis() - lastButtonChangeMs) > BUTTON_DEBOUNCE_MS && reading != lastStableButtonState) {
    lastStableButtonState = reading;
    if (reading == LOW) { // Pressed
      if (millis() - lastButtonPressMs >= BUTTON_COOLDOWN_MS) {
        lastButtonPressMs = millis();
        Serial.println("[BUTTON] Help requested");
        sendHelpRequest();
      }
    }
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
  Serial.println("RFID Attendance System ready");
}

void loop() {
  handleButton();
  pollEnrollmentCommand();
  handleRfidScan();
  updateFeedback();
  sendHeartbeat();
}
