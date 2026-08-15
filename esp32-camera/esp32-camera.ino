#include <Arduino.h>
#include <WiFi.h>
#include <esp_camera.h>
#include <HTTPClient.h>

// ============================================================================
// Configuration
// ============================================================================
const char* WIFI_SSID = "Public APCS 4.2";
const char* WIFI_PASSWORD = "PublicApcs";

const char* API_URL = "http://10.122.5.62:8000/internal/v1/cameras/";
const char* CAMERA_ID = "cam-01";

constexpr int FLASH_LED_PIN = 4;
constexpr unsigned long FRAME_INTERVAL_MS = 500;
constexpr unsigned long BLINK_INTERVAL_MS = 250;

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
bool ledState = false;

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
  digitalWrite(FLASH_LED_PIN, HIGH); // Steady when connected
}

// ============================================================================
// Camera Initialization
// ============================================================================
bool initCamera() {
  camera_config_t config;
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
  config.jpeg_quality = 12;
  config.fb_count = 1;

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
  // Normally sync with NTP. Here returning empty to let server set it, 
  // or a placeholder if required. For multipart it's a form field.
  // We'll sync time so we can provide a valid timestamp.
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
// Capture and Send Frame
// ============================================================================
void captureAndSend() {
  if (millis() - lastFrameMs < FRAME_INTERVAL_MS) {
    return;
  }
  lastFrameMs = millis();

  if (WiFi.status() != WL_CONNECTED) {
    Serial.println("WiFi not connected, skipping frame");
    return;
  }

  camera_fb_t* fb = esp_camera_fb_get();
  if (!fb) {
    Serial.println("Camera capture failed");
    return;
  }

  HTTPClient http;
  String url = String(API_URL) + String(CAMERA_ID) + "/frames";
  http.begin(url);

  String boundary = "----ESP32CamBoundary";
  http.addHeader("Content-Type", "multipart/form-data; boundary=" + boundary);

  String timestamp = getIsoTimestamp();

  String head = "--" + boundary + "\r\n";
  head += "Content-Disposition: form-data; name=\"cameraId\"\r\n\r\n";
  head += String(CAMERA_ID) + "\r\n";
  
  if (timestamp.length() > 0) {
    head += "--" + boundary + "\r\n";
    head += "Content-Disposition: form-data; name=\"timestamp\"\r\n\r\n";
    head += timestamp + "\r\n";
  }

  head += "--" + boundary + "\r\n";
  head += "Content-Disposition: form-data; name=\"frame\"; filename=\"frame.jpg\"\r\n";
  head += "Content-Type: image/jpeg\r\n\r\n";

  String tail = "\r\n--" + boundary + "--\r\n";

  uint32_t totalLen = head.length() + fb->len + tail.length();

  WiFiClient *stream = http.getStreamPtr();
  http.sendRequest("POST", stream, totalLen);

  stream->print(head);
  stream->write(fb->buf, fb->len);
  stream->print(tail);

  int httpCode = http.GET(); // This gets the response code for the request sent
  // Note: For POST with stream, sendRequest already sets it up, but HTTPClient needs to get the response.
  // Actually, wait, HTTPClient doesn't have a simple way to stream upload with `POST` returning code directly in standard usage unless we do it correctly. 
  // Wait, correct usage of HTTPClient stream: 
  // http.sendRequest("POST", stream, totalLen) will not work if stream is null. stream ptr is for receiving.
  // For sending, we can allocate a buffer or send piece by piece.
  // Let's just use `http.sendRequest("POST", (uint8_t *)payload, payload_len)` or custom WiFiClient.

  // Let's rewrite the POST using a custom buffer or String if it's small, or use raw WiFiClient.
  // A VGA JPEG is ~10-20KB, which fits in ESP32 RAM. Let's just build a single buffer to be safe, 
  // but wait, standard way is to use WiFiClient directly to avoid huge String allocations.
  
  http.end();
  esp_camera_fb_return(fb);
}

void sendFrameRaw() {
  if (millis() - lastFrameMs < FRAME_INTERVAL_MS) {
    return;
  }
  lastFrameMs = millis();

  if (WiFi.status() != WL_CONNECTED) {
    Serial.println("WiFi offline");
    return;
  }

  camera_fb_t* fb = esp_camera_fb_get();
  if (!fb) {
    Serial.println("Capture failed");
    return;
  }

  HTTPClient http;
  String url = String(API_URL) + CAMERA_ID + "/frames";
  http.begin(url);
  
  String boundary = "----ESP32CamBoundary";
  http.addHeader("Content-Type", "multipart/form-data; boundary=" + boundary);
  
  String head = "--" + boundary + "\r\n";
  head += "Content-Disposition: form-data; name=\"cameraId\"\r\n\r\n";
  head += String(CAMERA_ID) + "\r\n";
  
  String timestamp = getIsoTimestamp();
  if (timestamp.length() > 0) {
    head += "--" + boundary + "\r\n";
    head += "Content-Disposition: form-data; name=\"timestamp\"\r\n\r\n";
    head += timestamp + "\r\n";
  }

  head += "--" + boundary + "\r\n";
  head += "Content-Disposition: form-data; name=\"file\"; filename=\"frame.jpg\"\r\n";
  head += "Content-Type: image/jpeg\r\n\r\n";
  
  String tail = "\r\n--" + boundary + "--\r\n";

  size_t totalLen = head.length() + fb->len + tail.length();
  
  // To avoid huge allocation, we can just use the underlying WiFiClient
  // But wait, HTTPClient doesn't support streaming POST easily in all versions.
  // Let's allocate a buffer if we have RAM. VGA JPEG is small (~15-30KB).
  uint8_t *post_data = (uint8_t *)malloc(totalLen);
  if (!post_data) {
      Serial.println("Malloc failed");
      esp_camera_fb_return(fb);
      http.end();
      return;
  }
  
  memcpy(post_data, head.c_str(), head.length());
  memcpy(post_data + head.length(), fb->buf, fb->len);
  memcpy(post_data + head.length() + fb->len, tail.c_str(), tail.length());
  
  int code = http.POST(post_data, totalLen);
  if (code > 0) {
    Serial.printf("Frame sent, HTTP %d\n", code);
  } else {
    Serial.printf("Frame send failed: %s\n", http.errorToString(code).c_str());
  }
  
  free(post_data);
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
}

void loop() {
  if (WiFi.status() != WL_CONNECTED) {
    if (millis() - lastBlinkMs > BLINK_INTERVAL_MS) {
      lastBlinkMs = millis();
      ledState = !ledState;
      digitalWrite(FLASH_LED_PIN, ledState);
    }
    WiFi.reconnect();
    return;
  } else {
    digitalWrite(FLASH_LED_PIN, HIGH);
  }

  sendFrameRaw();
}
