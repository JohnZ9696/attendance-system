#include <Arduino.h>
#include <WiFi.h>
#include <esp_camera.h>
#include <HTTPClient.h>

// ============================================================================
// Configuration
// ============================================================================

const char* WIFI_SSID = "Minh Thu";
const char* WIFI_PASSWORD = "camsaike";
const char* INTERNAL_API_KEY = "s5HpmgoZ4Wl5A9v8pJ6qLuAyWIrAZLU_nP3W3AeaUDc";
const char* API_URL = "http://192.168.1.104:8000/internal/v1/cameras/";
const char* CAMERA_ID = "cam-01";

constexpr int FLASH_LED_PIN = 4;
constexpr unsigned long FRAME_INTERVAL_MS = 100;
constexpr unsigned long BLINK_INTERVAL_MS = 250;
constexpr unsigned long COMMAND_POLL_INTERVAL_MS = 500;

// ============================================================================
// Camera Pins (AI-Thinker)
// ============================================================================
#define PWDN_GPIO_NUM     32
#define RESET_GPIO_NUM    -1
#define XCLK_GPIO_NUM      0
#define SIOD_GPIO_NUM     26
#define SIOC_GPIO_NUM     27
#define Y9_GPIO_NUM       35
#define Y8_GPIO_NUM       34
#define Y7_GPIO_NUM       39
#define Y6_GPIO_NUM       36
#define Y5_GPIO_NUM       21
#define Y4_GPIO_NUM       19
#define Y3_GPIO_NUM       18
#define Y2_GPIO_NUM        5
#define VSYNC_GPIO_NUM    25
#define HREF_GPIO_NUM     23
#define PCLK_GPIO_NUM     22

// ============================================================================
// State
// ============================================================================
unsigned long lastFrameMs = 0;
unsigned long lastBlinkMs = 0;
unsigned long lastCommandPollMs = 0;
bool ledState = false;
bool captureRequested = false;


// ============================================================================
// WiFi Connection
// ============================================================================
void connectWiFi() {
  if (WiFi.status() == WL_CONNECTED) return;

  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  Serial.print("Connecting to WiFi");

  while (WiFi.status() != WL_CONNECTED) {
    if (millis() - lastBlinkMs > BLINK_INTERVAL_MS) {
      lastBlinkMs = millis();
      ledState = !ledState;
      digitalWrite(FLASH_LED_PIN, ledState);
    }
    delay(10);
    Serial.print(".");
  }
  Serial.println("\nWiFi connected");
  digitalWrite(FLASH_LED_PIN, LOW); // Off when connected
}

// ============================================================================
// Camera Initialization
// ============================================================================
bool initCamera() {
  camera_config_t config = {};
  config.ledc_channel = LEDC_CHANNEL_0;
  config.ledc_timer = LEDC_TIMER_0;
  config.pin_d0 = Y2_GPIO_NUM;
  config.pin_d1 = Y3_GPIO_NUM;
  config.pin_d2 = Y4_GPIO_NUM;
  config.pin_d3 = Y5_GPIO_NUM;
  config.pin_d4 = Y6_GPIO_NUM;
  config.pin_d5 = Y7_GPIO_NUM;
  config.pin_d6 = Y8_GPIO_NUM;
  config.pin_d7 = Y9_GPIO_NUM;
  config.pin_xclk = XCLK_GPIO_NUM;
  config.pin_pclk = PCLK_GPIO_NUM;
  config.pin_vsync = VSYNC_GPIO_NUM;
  config.pin_href = HREF_GPIO_NUM;
  config.pin_sccb_sda = SIOD_GPIO_NUM;
  config.pin_sccb_scl = SIOC_GPIO_NUM;
  config.pin_pwdn = PWDN_GPIO_NUM;
  config.pin_reset = RESET_GPIO_NUM;
  config.xclk_freq_hz = 20000000;
  config.pixel_format = PIXFORMAT_JPEG;

  // FRAMESIZE_VGA, JPEG quality 12, 1 frame buffer
  config.frame_size = FRAMESIZE_VGA;
  config.jpeg_quality = 15;
  config.fb_count = 1;
  config.grab_mode = CAMERA_GRAB_WHEN_EMPTY;
  config.fb_location = CAMERA_FB_IN_PSRAM;

  esp_err_t err = esp_camera_init(&config);
  if (err != ESP_OK) {
    Serial.printf("Camera init failed with error 0x%x\n", err);
    return false;
  }
  Serial.println("Camera initialized");
  return true;
}

// ============================================================================
// ISO-8601 Timestamp Approximation
// ============================================================================
String getIsoTimestamp() {
  time_t now;
  struct tm timeinfo;
  if (!getLocalTime(&timeinfo)) {
    return "";
  }
  char buf[32];
  strftime(buf, sizeof(buf), "%Y-%m-%dT%H:%M:%SZ", &timeinfo);
  return String(buf);
}

// ============================================================================
// Capture Command Polling
// ============================================================================
void pollCaptureCommand() {
  if (millis() - lastCommandPollMs < COMMAND_POLL_INTERVAL_MS) {
    return;
  }

  lastCommandPollMs = millis();

  if (WiFi.status() != WL_CONNECTED) {
    captureRequested = false;
    Serial.println("[COMMAND] WiFi offline");
    return;
  }

  HTTPClient http;
  String url = String(API_URL) + CAMERA_ID + "/capture-command";

  http.begin(url);
  http.addHeader("INTERNAL-API-KEY", INTERNAL_API_KEY);
  http.setTimeout(2000);

  int code = http.GET();
  Serial.printf("[COMMAND] HTTP=%d\n", code);

  if (code == HTTP_CODE_OK) {
    String response = http.getString();

    Serial.print("[COMMAND] Body=");
    Serial.println(response);

    response.replace(" ", "");
    response.replace("\r", "");
    response.replace("\n", "");

    captureRequested =
        response.indexOf("\"active\":true") >= 0;

    Serial.printf(
        "[COMMAND] captureRequested=%d\n",
        captureRequested
    );
  } else {
    captureRequested = false;
    Serial.printf(
        "[COMMAND] Error=%s\n",
        http.errorToString(code).c_str()
    );
  }

  http.end();
}

// ============================================================================
// Send Frame
// ============================================================================
void sendFrameRaw() {
  if (millis() - lastFrameMs < FRAME_INTERVAL_MS) {
    return;
  }

  lastFrameMs = millis();

  Serial.println("[FRAME] Getting camera frame...");

  camera_fb_t* fb = esp_camera_fb_get();

  if (!fb) {
    Serial.println("[FRAME] Capture failed");
    return;
  }

  Serial.printf(
      "[FRAME] Captured %u bytes\n",
      fb->len
  );

  HTTPClient http;
  String url =
      String(API_URL)
      + CAMERA_ID
      + "/frames";

  http.begin(url);
  http.setTimeout(10000);
  http.addHeader(
      "INTERNAL-API-KEY",
      INTERNAL_API_KEY
  );

  String boundary = "----ESP32CamBoundary";

  http.addHeader(
      "Content-Type",
      "multipart/form-data; boundary=" + boundary
  );

  String head =
      "--" + boundary + "\r\n";

  head +=
      "Content-Disposition: form-data; "
      "name=\"cameraId\"\r\n\r\n";

  head += String(CAMERA_ID) + "\r\n";

  head += "--" + boundary + "\r\n";

  head +=
      "Content-Disposition: form-data; "
      "name=\"image\"; filename=\"frame.jpg\"\r\n";

  head += "Content-Type: image/jpeg\r\n\r\n";

  String tail =
      "\r\n--" + boundary + "--\r\n";

  size_t totalLen =
      head.length()
      + fb->len
      + tail.length();

  uint8_t* postData = psramFound()
      ? (uint8_t*)ps_malloc(totalLen)
      : (uint8_t*)malloc(totalLen);

  if (!postData) {
    Serial.println("[FRAME] Malloc failed");
    esp_camera_fb_return(fb);
    http.end();
    return;
  }

  memcpy(
      postData,
      head.c_str(),
      head.length()
  );

  memcpy(
      postData + head.length(),
      fb->buf,
      fb->len
  );

  memcpy(
      postData + head.length() + fb->len,
      tail.c_str(),
      tail.length()
  );

  Serial.printf(
      "[FRAME] POST starting, total=%u bytes\n",
      totalLen
  );

  unsigned long startedMs = millis();

  int code = http.POST(
      postData,
      totalLen
  );

  Serial.printf(
      "[FRAME] POST finished: HTTP=%d, time=%lu ms\n",
      code,
      millis() - startedMs
  );

  if (code < 0) {
    Serial.printf(
        "[FRAME] Error=%s\n",
        http.errorToString(code).c_str()
    );
  }

  free(postData);
  http.end();
  esp_camera_fb_return(fb);
}

void setup() {
  Serial.begin(115200);
  pinMode(FLASH_LED_PIN, OUTPUT);
  digitalWrite(FLASH_LED_PIN, LOW);

  connectWiFi();
  configTime(0, 0, "pool.ntp.org");

  while (!initCamera()) {
    Serial.println("Retrying camera init in 1s...");
    delay(1000);
  }

  Serial.println("[CAMERA] Capture firmware V2 ready");
}

void loop() {
  if (WiFi.status() != WL_CONNECTED) {
    digitalWrite(FLASH_LED_PIN, LOW);
    WiFi.reconnect();
    delay(500);
    return;
  }

  pollCaptureCommand();
  digitalWrite(FLASH_LED_PIN, LOW);

  if (captureRequested) {
    Serial.println("[CAMERA] Capturing...");
    sendFrameRaw();
  }

  delay(20);
}