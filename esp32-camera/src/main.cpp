#include <Arduino.h>
#include <HTTPClient.h>
#include <WiFi.h>
#include <esp_camera.h>

#include "secrets.h"

// AI-Thinker ESP32-CAM pin map.
#define PWDN_GPIO_NUM 32
#define RESET_GPIO_NUM -1
#define XCLK_GPIO_NUM 0
#define SIOD_GPIO_NUM 26
#define SIOC_GPIO_NUM 27
#define Y9_GPIO_NUM 35
#define Y8_GPIO_NUM 34
#define Y7_GPIO_NUM 39
#define Y6_GPIO_NUM 36
#define Y5_GPIO_NUM 21
#define Y4_GPIO_NUM 19
#define Y3_GPIO_NUM 18
#define Y2_GPIO_NUM 5
#define VSYNC_GPIO_NUM 25
#define HREF_GPIO_NUM 23
#define PCLK_GPIO_NUM 22

constexpr char CAMERA_ID[] = "cam-01";
constexpr unsigned long COMMAND_POLL_INTERVAL_MS = 500;
constexpr unsigned long FRAME_INTERVAL_MS = 300;
constexpr unsigned long WIFI_RETRY_INTERVAL_MS = 5000;

unsigned long lastCommandPollMs = 0;
unsigned long lastFrameMs = 0;
unsigned long lastWiFiRetryMs = 0;
bool captureRequested = false;

String cameraUrl(const char* endpoint) {
  return String(AI_SERVICE_BASE_URL) + "/internal/v1/cameras/" + CAMERA_ID + endpoint;
}

void setCaptureRequested(bool active) {
  if (captureRequested == active) {
    return;
  }

  captureRequested = active;
  Serial.println(active ? "[CAMERA] Capture started" : "[CAMERA] Capture stopped");
}

void connectWiFi() {
  if (WiFi.status() == WL_CONNECTED) {
    return;
  }

  WiFi.mode(WIFI_STA);
  WiFi.persistent(false);
  WiFi.setAutoReconnect(true);
  WiFi.setSleep(false);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);

  Serial.print("[WIFI] Connecting");
  const unsigned long startedMs = millis();
  while (WiFi.status() != WL_CONNECTED && millis() - startedMs < 15000) {
    Serial.print('.');
    delay(250);
  }

  if (WiFi.status() == WL_CONNECTED) {
    Serial.print("\n[WIFI] Connected: ");
    Serial.println(WiFi.localIP());
  } else {
    Serial.println("\n[WIFI] Connection timeout");
  }
}

bool initCamera() {
  const bool hasPsram = psramFound();
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
  config.frame_size = hasPsram ? FRAMESIZE_VGA : FRAMESIZE_QVGA;
  config.jpeg_quality = hasPsram ? 12 : 18;
  config.fb_count = 1;
  config.fb_location = hasPsram ? CAMERA_FB_IN_PSRAM : CAMERA_FB_IN_DRAM;
  config.grab_mode = hasPsram ? CAMERA_GRAB_LATEST : CAMERA_GRAB_WHEN_EMPTY;

  const esp_err_t result = esp_camera_init(&config);
  if (result != ESP_OK) {
    Serial.printf("[CAMERA] Init failed: 0x%x\n", result);
    return false;
  }

  Serial.printf("[CAMERA] Ready (PSRAM: %s)\n", hasPsram ? "yes" : "no");
  return true;
}

void pollCaptureCommand() {
  WiFiClient client;
  HTTPClient http;

  if (!http.begin(client, cameraUrl("/capture-command"))) {
    Serial.println("[COMMAND] Invalid URL");
    setCaptureRequested(false);
    return;
  }

  http.setConnectTimeout(1500);
  http.setTimeout(3000);
  http.addHeader("INTERNAL-API-KEY", INTERNAL_API_KEY);

  const int statusCode = http.GET();
  if (statusCode == HTTP_CODE_OK) {
    String response = http.getString();
    response.replace(" ", "");
    response.replace("\r", "");
    response.replace("\n", "");
    setCaptureRequested(response.indexOf("\"active\":true") >= 0);
  } else {
    Serial.printf("[COMMAND] Request failed: %d\n", statusCode);
    setCaptureRequested(false);
  }

  http.end();
}

void sendFrame() {
  camera_fb_t* frame = esp_camera_fb_get();
  if (frame == nullptr) {
    Serial.println("[FRAME] Capture failed");
    return;
  }

  const String boundary = "----ESP32CamBoundary";
  const String bodyStart =
      "--" + boundary + "\r\n"
      "Content-Disposition: form-data; name=\"image\"; filename=\"frame.jpg\"\r\n"
      "Content-Type: image/jpeg\r\n\r\n";
  const String bodyEnd = "\r\n--" + boundary + "--\r\n";
  const size_t bodyLength = bodyStart.length() + frame->len + bodyEnd.length();

  uint8_t* body = static_cast<uint8_t*>(malloc(bodyLength));
  if (body == nullptr) {
    Serial.println("[FRAME] Not enough memory to upload frame");
    esp_camera_fb_return(frame);
    return;
  }

  memcpy(body, bodyStart.c_str(), bodyStart.length());
  memcpy(body + bodyStart.length(), frame->buf, frame->len);
  memcpy(body + bodyStart.length() + frame->len, bodyEnd.c_str(), bodyEnd.length());

  WiFiClient client;
  HTTPClient http;
  int statusCode = -1;

  if (http.begin(client, cameraUrl("/frames"))) {
    http.setConnectTimeout(2000);
    http.setTimeout(8000);
    http.addHeader("INTERNAL-API-KEY", INTERNAL_API_KEY);
    http.addHeader("Content-Type", "multipart/form-data; boundary=" + boundary);
    statusCode = http.POST(body, bodyLength);
    http.end();
  }

  free(body);
  esp_camera_fb_return(frame);

  if (statusCode != HTTP_CODE_OK) {
    Serial.printf("[FRAME] Upload failed: %d\n", statusCode);
  }
}

void setup() {
  Serial.begin(115200);
  connectWiFi();

  if (!initCamera()) {
    Serial.println("[CAMERA] Restarting in 3 seconds");
    delay(3000);
    ESP.restart();
  }

  lastCommandPollMs = millis() - COMMAND_POLL_INTERVAL_MS;
  Serial.println("[CAMERA] Ready");
}

void loop() {
  if (WiFi.status() != WL_CONNECTED) {
    setCaptureRequested(false);
    if (millis() - lastWiFiRetryMs >= WIFI_RETRY_INTERVAL_MS) {
      lastWiFiRetryMs = millis();
      connectWiFi();
    }
    delay(20);
    return;
  }

  if (millis() - lastCommandPollMs >= COMMAND_POLL_INTERVAL_MS) {
    lastCommandPollMs = millis();
    pollCaptureCommand();
  }

  if (captureRequested && millis() - lastFrameMs >= FRAME_INTERVAL_MS) {
    lastFrameMs = millis();
    sendFrame();
  }

  delay(5);
}
