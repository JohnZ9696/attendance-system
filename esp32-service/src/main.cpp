#include <Arduino.h>
#include <SPI.h>
#include <WiFi.h>
#include <WiFiUdp.h>
#include <ESPmDNS.h>
#include <HTTPClient.h>
#include <MFRC522.h>
#include <ArduinoJson.h>
#include <Wire.h>
#include <Adafruit_GFX.h>
#include <Adafruit_SSD1306.h>

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

// OLED I2C pins
constexpr uint8_t OLED_SDA_PIN = 21;
constexpr uint8_t OLED_SCL_PIN = 22;
constexpr uint8_t OLED_ADDRESS = 0x3C;
constexpr int OLED_WIDTH = 128;
constexpr int OLED_HEIGHT = 64;

Adafruit_SSD1306 oled(OLED_WIDTH, OLED_HEIGHT, &Wire, -1);
bool oledReady = false;

#include "wifi_config.h"

// ============================================================================
// Configuration
// ============================================================================
constexpr char kWiFiSsid[]     = WIFI_SSID;
constexpr char kWiFiPassword[] = WIFI_PASSWORD;
constexpr char kDeviceId[]     = "door-01";

constexpr char kMdnsHostname[] = "attendance";

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
  PROCESSING,
  RFID_INVALID,
  FACE_NOT_ENROLLED,
  CAMERA_OFFLINE,
  CAPTURE_TIMEOUT,
  LIVENESS_FAILED,
  FACE_BELOW_THRESHOLD,
  FACE_MATCH_TIMEOUT,
  MULTIPLE_FACES,
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
volatile bool rfidRequestInProgress = false;
unsigned long lastEnrollmentPollMs = 0UL;
bool rfidReady = false;

// ----------------------------------------------------------------------------
// OLED helpers
// ----------------------------------------------------------------------------
void showOled(
    const String& title,
    const String& line1 = "",
    const String& line2 = ""
) {
  if (!oledReady) return;

  oled.clearDisplay();
  oled.setTextColor(SSD1306_WHITE);

  oled.setTextSize(1);
  oled.setCursor(0, 0);
  oled.println(title);

  oled.drawLine(0, 11, 127, 11, SSD1306_WHITE);

  oled.setCursor(0, 20);
  oled.println(line1);

  oled.setCursor(0, 36);
  oled.println(line2);

  oled.display();
}

void initOled() {
  Wire.begin(OLED_SDA_PIN, OLED_SCL_PIN);

  oledReady = oled.begin(
      SSD1306_SWITCHCAPVCC,
      OLED_ADDRESS
  );

  if (!oledReady) {
    Serial.println("[OLED] Khong tim thay OLED tai 0x3C");
    return;
  }

  oled.clearDisplay();
  oled.display();
  showOled("HE THONG", "Dang khoi dong...");
}

void showFeedbackOnOled(FeedbackState state) {
  switch (state) {
    case FeedbackState::RFID_INVALID:
      showOled("THAT BAI", "THE KHONG HOP LE");
      break;

    case FeedbackState::FACE_NOT_ENROLLED:
      showOled("THAT BAI", "CHUA DANG KY MAT");
      break;

    case FeedbackState::CAMERA_OFFLINE:
      showOled("THAT BAI", "CAMERA OFFLINE");
      break;

    case FeedbackState::CAPTURE_TIMEOUT:
      showOled("THAT BAI", "CAPTURE TIMEOUT");
      break;

    case FeedbackState::LIVENESS_FAILED:
      showOled("THAT BAI", "LIVENESS FAILED");
      break;

    case FeedbackState::FACE_BELOW_THRESHOLD:
      showOled("THAT BAI", "FACE NOT MATCH");
      break;

    case FeedbackState::FACE_MATCH_TIMEOUT:
      showOled("THAT BAI", "FACE TIMEOUT");
      break;

    case FeedbackState::MULTIPLE_FACES:
      showOled("THAT BAI", "NHIEU KHUON MAT");
      break;

    case FeedbackState::ALREADY_CHECKED_IN:
      showOled("THONG BAO", "DA DIEM DANH");
      break;

    case FeedbackState::CHECK_IN_ON_TIME:
      showOled("THANH CONG", "DUNG GIO");
      break;

    case FeedbackState::CHECK_IN_LATE:
      showOled("THANH CONG", "DI TRE");
      break;

    case FeedbackState::CLOUD_WRITE_FAILED:
      showOled("LOI HE THONG", "KHONG GUI DUOC");
      break;

    case FeedbackState::INCIDENT_RECORDED:
      showOled("HO TRO", "DA GUI YEU CAU");
      break;

    default:
      break;
  }
}

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
IPAddress workingNetworkIp(0, 0, 0, 0);

constexpr uint16_t DISCOVERY_PORT = 4210;

bool readDiscoveryResponse(
    WiFiUDP& udp,
    IPAddress& outIp,
    uint16_t& outPort,
    unsigned long timeoutMs) {
  const unsigned long deadline = millis() + timeoutMs;
  while ((long)(deadline - millis()) > 0) {
    int size = udp.parsePacket();
    if (size > 0) {
      char response[64] = {};
      int read = udp.read(response, sizeof(response) - 1);
      if (read > 0 && strncmp(response, "ATTENDANCE_SERVER_V1:", 21) == 0) {
        outIp = udp.remoteIP();
        outPort = static_cast<uint16_t>(atoi(response + 21));
        return outPort > 0;
      }
    }
    delay(5);
  }
  return false;
}

void sendDiscoveryRequest(WiFiUDP& udp, const IPAddress& target) {
  const char request[] = "ATTENDANCE_DISCOVER_V1";
  udp.beginPacket(target, DISCOVERY_PORT);
  udp.write(reinterpret_cast<const uint8_t*>(request), sizeof(request) - 1);
  udp.endPacket();
}

bool discoverServer(IPAddress& outIp, uint16_t& outPort) {
  WiFiUDP udp;
  if (!udp.begin(0)) return false;

  IPAddress localIp = WiFi.localIP();
  IPAddress mask = WiFi.subnetMask();
  IPAddress broadcast(
      localIp[0] | static_cast<uint8_t>(~mask[0]),
      localIp[1] | static_cast<uint8_t>(~mask[1]),
      localIp[2] | static_cast<uint8_t>(~mask[2]),
      localIp[3] | static_cast<uint8_t>(~mask[3]));
  for (int attempt = 0; attempt < 3; attempt++) {
    sendDiscoveryRequest(udp, broadcast);
    if (readDiscoveryResponse(udp, outIp, outPort, 700)) {
      udp.stop();
      return true;
    }
  }

  // Phone hotspots may block broadcast but still allow direct client traffic.
  if (mask[0] == 255 && mask[1] == 255 && mask[2] == 255) {
    const uint8_t firstHost = (localIp[3] & mask[3]) + 1;
    const uint8_t lastHost = (localIp[3] | static_cast<uint8_t>(~mask[3])) - 1;
    Serial.println("[DISCOVERY] Broadcast bi chan, dang quet subnet...");

    for (uint16_t host = firstHost; host <= lastHost; host++) {
      if (host == localIp[3]) continue;
      sendDiscoveryRequest(udp, IPAddress(localIp[0], localIp[1], localIp[2], host));
      if (readDiscoveryResponse(udp, outIp, outPort, 40)) {
        udp.stop();
        return true;
      }
    }
  }

  udp.stop();
  return false;
}

String getApiBase() {
  if (!connectToWifi()) return "";
  if (workingNetworkIp != WiFi.localIP()) {
    workingBase = "";
    workingNetworkIp = WiFi.localIP();
  }
  if (workingBase.length() > 0) return workingBase;

  IPAddress serverIp(0, 0, 0, 0);
  uint16_t serverPort = 8080;

  Serial.println("[DISCOVERY] Dang tim Spring Boot qua UDP...");
  if (discoverServer(serverIp, serverPort)) {
    Serial.printf("[DISCOVERY] Server %s:%u\n", serverIp.toString().c_str(), serverPort);
  } else if (MDNS.begin(kMdnsHostname)) {
    serverIp = MDNS.queryHost("attendance", 1000);
    MDNS.end();
    if (serverIp == IPAddress(0, 0, 0, 0)) {
      Serial.println("[DISCOVERY] Khong tim thay server");
      return "";
    }
    Serial.printf("[MDNS] attendance.local -> %s\n", serverIp.toString().c_str());
  } else {
    Serial.println("[DISCOVERY] Khong tim thay server");
    return "";
  }

  char buf[128];
  snprintf(buf, sizeof buf, "http://%s:%u/api/v1", serverIp.toString().c_str(), serverPort);
  workingBase = String(buf);
  return workingBase;
}

// ----------------------------------------------------------------------------
// API calls
// ----------------------------------------------------------------------------

void sendHeartbeat() {
  if (rfidRequestInProgress) return;
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
  if (rfidRequestInProgress) {
    startFeedback(FeedbackState::PROCESSING);
    return;
  }

  const String apiBase = getApiBase();
  if (apiBase.length() == 0 || !connectToWifi()) {
    startFeedback(FeedbackState::CLOUD_WRITE_FAILED);
    return;
  }

  HTTPClient http;
  String url = apiBase + "/assistance";
  http.begin(url);
  http.addHeader("Content-Type", "application/json");

  JsonDocument doc;
  doc["deviceId"] = kDeviceId;
  doc["type"] = "HARDWARE_HELP_REQUEST";
  doc["source"] = "PUSH_BUTTON";
  doc["occurredAt"] = getIsoTimestamp();

  String payload;
  serializeJson(doc, payload);

  http.setTimeout(5000);
  int code = http.POST(payload);
  String responseBody = http.getString();

  Serial.printf("[HELP] POST %s -> %d\n", url.c_str(), code);
  Serial.println(responseBody);
  http.end();

  if (code == 200 || code == 201) {
    startFeedback(FeedbackState::INCIDENT_RECORDED);
  } else {
    startFeedback(FeedbackState::CLOUD_WRITE_FAILED);
  }
}

void pollEnrollmentCommand() {
  if (rfidRequestInProgress) return;
  if (millis() - lastEnrollmentPollMs < ENROLLMENT_POLL_MS || WiFi.status() != WL_CONNECTED) return;
  lastEnrollmentPollMs = millis();

  const String apiBase = getApiBase();
  if (apiBase.length() == 0) return;

  HTTPClient http;
  http.begin(apiBase + "/rfid-enrollment");
  http.setTimeout(2000);
  int code = http.GET();
  if (code == HTTP_CODE_OK) {
    JsonDocument doc;
    if (!deserializeJson(doc, http.getString())) {
      bool waiting = doc["status"].as<String>() == "WAITING";
      if (waiting != enrollmentMode) {
        enrollmentMode = waiting;
        Serial.println(enrollmentMode ? "[ENROLLMENT] Ready" : "[ENROLLMENT] Cancelled/completed");
        
        if (enrollmentMode) {
          showOled("DANG KY THE", "Moi quet the");
        } else {
          showOled("SAN SANG", "Moi quet the");
        }
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
  
  JsonDocument doc;
  doc["uid"] = uid;
  String payload;
  serializeJson(doc, payload);

  http.setTimeout(5000);
  int code = http.POST(payload);
  http.end();
  return code == HTTP_CODE_OK;
}

void sendRfidScanTask(void *pvParameters) {
  String uid = *(String*)pvParameters;
  delete (String*)pvParameters;

  const String apiBase = getApiBase();
  if (apiBase.length() == 0 || !connectToWifi()) {
    Serial.println("[RFID HTTP] WiFi or API base unavailable");
    startFeedback(FeedbackState::CLOUD_WRITE_FAILED);
    rfidRequestInProgress = false;
    vTaskDelete(NULL);
    return;
  }

  WiFiClient client;
  HTTPClient http;
  String url = apiBase + "/devices/" + kDeviceId + "/rfid-scans";
  if (!http.begin(client, url)) {
    Serial.printf("[RFID HTTP] Invalid URL: %s\n", url.c_str());
    startFeedback(FeedbackState::CLOUD_WRITE_FAILED);
    rfidRequestInProgress = false;
    vTaskDelete(NULL);
    return;
  }
  http.addHeader("Content-Type", "application/json");

  JsonDocument doc;
  doc["uid"] = uid;
  doc["deviceId"] = kDeviceId;
  doc["scannedAt"] = getIsoTimestamp();

  String payload;
  serializeJson(doc, payload);

  http.setTimeout(50000); // Allow time for face match
  Serial.printf("[RFID HTTP] POST %s\n", url.c_str());
  int code = http.POST(payload);
  String responseBody = code > 0 ? http.getString() : http.errorToString(code);
  Serial.printf("[RFID HTTP] Status: %d\n", code);
  Serial.println(responseBody);
  
  if (code > 0) {
    JsonDocument resp;
    deserializeJson(resp, responseBody);
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
  rfidRequestInProgress = false;
  vTaskDelete(NULL);
}

void sendRfidScan(const String& uid) {
  if (rfidRequestInProgress) return;
  rfidRequestInProgress = true;
  startFeedback(FeedbackState::PROCESSING);
  String* uidPtr = new String(uid);
  if (uidPtr == nullptr || xTaskCreate(sendRfidScanTask, "rfid_scan", 8192, uidPtr, 1, NULL) != pdPASS) {
    delete uidPtr;
    rfidRequestInProgress = false;
    startFeedback(FeedbackState::CLOUD_WRITE_FAILED);
    Serial.println("[RFID HTTP] Could not start request task");
  }
}

// ----------------------------------------------------------------------------
// Feedback state machine
// ----------------------------------------------------------------------------
void startFeedback(FeedbackState state) {
  feedbackState = state;
  stateStartMs  = millis();
  allOff();
  showFeedbackOnOled(state);
}

void updateFeedback() {
  if (feedbackState == FeedbackState::IDLE) return;
  const unsigned long elapsed = millis() - stateStartMs;

  bool r_led = false, g_led = false, buzz = false;
  unsigned long duration = 2000UL; // default 2s
  
  switch (feedbackState) {
    case FeedbackState::PROCESSING:
      // green LED + short beep (instant feedback)
      g_led = true;
      buzz = (elapsed < 100);
      duration = 60000UL; // Up to 60s for face match timeout
      break;

    // ===== TẤT CẢ LỖI -> ĐỎ =====
    case FeedbackState::RFID_INVALID:           // UID không có trong DB
    case FeedbackState::FACE_NOT_ENROLLED:      // Chưa enroll face
    case FeedbackState::CAMERA_OFFLINE:
    case FeedbackState::CAPTURE_TIMEOUT:
    case FeedbackState::LIVENESS_FAILED:
    case FeedbackState::FACE_MATCH_TIMEOUT:
    case FeedbackState::MULTIPLE_FACES:         // Đa khuôn mặt
      r_led = true;
      buzz = (elapsed < 100) || (elapsed >= 200 && elapsed < 300); // 2 beep ngắn
      break;

    case FeedbackState::FACE_BELOW_THRESHOLD:   // Sai khuôn mặt
      r_led = true;
      buzz = (elapsed < 1000);                  // 1 beep dài
      break;

    // ===== THÀNH CÔNG -> XANH =====
    case FeedbackState::ALREADY_CHECKED_IN:
      g_led = (elapsed % 500) < 250;            // Xanh nháy
      duration = 2000UL;
      break;

    case FeedbackState::CHECK_IN_ON_TIME:
      g_led = true;
      buzz = (elapsed < 100);                   // 1 beep ngắn
      break;

    case FeedbackState::CHECK_IN_LATE:
      g_led = true;
      buzz = (elapsed < 100) || (elapsed >= 200 && elapsed < 300); // 2 beep ngắn
      break;

    case FeedbackState::CLOUD_WRITE_FAILED:
      r_led = true;
      buzz = (elapsed < 500);                   // Beep dài 500ms
      break;

    case FeedbackState::INCIDENT_RECORDED:
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
    showOled("SAN SANG", "Moi quet the");
  }
}

// ----------------------------------------------------------------------------
// RFID
// ----------------------------------------------------------------------------
String readCardUid() {
  if (!rfidReady) return "";
  
  // Chua co the moi: im lang va tiep tuc cho.
  if (!rfid.PICC_IsNewCardPresent()) return "";

  // Co the nhung khong doc duoc UID.
  if (!rfid.PICC_ReadCardSerial()) {
    Serial.println("[RFID] Card detected but UID read failed");
    return "";
  }
  String uid = "";
  for (byte i = 0; i < rfid.uid.size; i++) {
    if (rfid.uid.uidByte[i] < 0x10) uid += "0";
    uid += String(rfid.uid.uidByte[i], HEX);
  }
  uid.toUpperCase();
  return uid;
}

void handleRfidScan() {
  static unsigned long lastScanMs = 0UL;

  if (!rfidReady) return;

  if (feedbackState == FeedbackState::PROCESSING) return;

  if (millis() - lastScanMs < CARD_COOLDOWN_MS) return;
  
  const String uid = readCardUid();
  if (uid.length() == 0) return;
  lastScanMs = millis();


  rfid.PICC_HaltA();
  rfid.PCD_StopCrypto1();

  lastScannedUid = uid;
  Serial.print("[SCAN] UID=");
  Serial.println(uid);
  Serial.println(enrollmentMode);
  
  if (enrollmentMode) {
    showOled("DANG KY THE", uid, "Dang gui...");
    if (submitEnrollmentUid(uid)) {
      enrollmentMode = false;
      startFeedback(FeedbackState::CHECK_IN_ON_TIME);
    } else {
      startFeedback(FeedbackState::CLOUD_WRITE_FAILED);
    }
    return;
  }
  
  showOled("DA DOC THE", uid, "Dang xu ly...");
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
      tone(BUZZER_PIN, 1000);
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

  initOled();

  SPI.begin(SPI_SCK_PIN, SPI_MISO_PIN, SPI_MOSI_PIN, RFID_SS_PIN);
  rfid.PCD_Init();
  delay(100);

  const byte version = rfid.PCD_ReadRegister(MFRC522::VersionReg);
  rfid.PCD_DumpVersionToSerial();

  rfidReady = version != 0x00 && version != 0xFF;
  Serial.println(rfidReady);
  if (rfidReady) {
    rfid.PCD_AntennaOn();
  } else {
    Serial.println("[RFID] ERROR: Khong giao tiep duoc voi RC522");
    Serial.println("[RFID] Kiem tra SDA=GPIO16, RST=GPIO17, SCK=18, MISO=19, MOSI=23, 3.3V va GND");
  }

  showOled("WIFI", "Dang ket noi...");
  const bool wifiOk = connectToWifi();

  if (wifiOk) {
    showOled(
        "WIFI OK",
        WiFi.localIP().toString(),
        "Dang dong bo gio"
    );
  } else {
    showOled("WIFI LOI", "Kiem tra mang");
  }

  syncTime();

  Serial.println("RFID Attendance System ready");
  showOled("SAN SANG", "Moi quet the");
}

void loop() {
  handleButton();
  pollEnrollmentCommand();
  handleRfidScan();
  updateFeedback();
  sendHeartbeat();
}
