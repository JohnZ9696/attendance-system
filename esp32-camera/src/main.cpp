#include <Arduino.h>
#include <HTTPClient.h>
#include <WiFi.h>
#include <esp_camera.h>

// wifi_config.h phải khai báo:
// extern const char* WIFI_SSID;
// extern const char* WIFI_PASSWORD;
// extern const char* INTERNAL_API_KEY;
// extern const char* API_URL;
//
// Ví dụ API_URL:
// http://192.168.1.104:8000/internal/v1/cameras/



const char* INTERNAL_API_KEY = "s5HpmgoZ4Wl5A9v8pJ6qLuAyWIrAZLU_nP3W3AeaUDc";
const char* API_URL = "http://192.168.1.213:8000/internal/v1/cameras/";
const char* CAMERA_ID = "cam-01";


constexpr int FLASH_LED_PIN = 4;

// Mỗi POST thường mất 200-300 ms; 250 ms giúp luồng ổn định
// và vẫn đủ frame cho chuỗi mở mắt -> nhắm mắt -> mở mắt.
constexpr unsigned long FRAME_INTERVAL_MS = 250;
constexpr unsigned long COMMAND_IDLE_POLL_MS = 500;
constexpr unsigned long COMMAND_ACTIVE_POLL_MS = 1000;
constexpr unsigned long WIFI_RETRY_INTERVAL_MS = 5000;

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

struct ParsedHttpUrl {
  String host;
  uint16_t port = 80;
  String path;
  bool valid = false;
};

ParsedHttpUrl apiBase;

unsigned long lastFrameMs = 0;
unsigned long lastCommandPollMs = 0;
unsigned long lastWifiRetryMs = 0;
unsigned long lastFrameErrorLogMs = 0;

bool captureRequested = false;
bool frameSuccessPrinted = false;
uint8_t consecutiveCommandFailures = 0;

ParsedHttpUrl parseHttpUrl(const char* rawUrl) {
  ParsedHttpUrl result;
  String url(rawUrl == nullptr ? "" : rawUrl);
  url.trim();

  const String prefix = "http://";
  if (!url.startsWith(prefix)) {
    return result;
  }

  int pathStart = url.indexOf('/', prefix.length());
  String authority = pathStart >= 0
      ? url.substring(prefix.length(), pathStart)
      : url.substring(prefix.length());

  result.path = pathStart >= 0 ? url.substring(pathStart) : "/";
  if (!result.path.endsWith("/")) {
    result.path += "/";
  }

  int colon = authority.lastIndexOf(':');
  if (colon >= 0) {
    result.host = authority.substring(0, colon);
    long parsedPort = authority.substring(colon + 1).toInt();
    if (parsedPort <= 0 || parsedPort > 65535) {
      return result;
    }
    result.port = static_cast<uint16_t>(parsedPort);
  } else {
    result.host = authority;
  }

  result.valid = result.host.length() > 0;
  return result;
}

void connectWiFi() {
  if (WiFi.status() == WL_CONNECTED) {
    return;
  }

  WiFi.mode(WIFI_STA);
  WiFi.persistent(false);
  WiFi.setAutoReconnect(true);
  WiFi.setSleep(false);  // Giảm độ trễ gửi frame.
  WiFi.begin("Minh Thu", "camsake");

  Serial.print("[WIFI] Connecting");
  unsigned long startedMs = millis();

  while (WiFi.status() != WL_CONNECTED && millis() - startedMs < 20000) {
    Serial.print('.');
    delay(250);
  }

  if (WiFi.status() == WL_CONNECTED) {
    Serial.print("\n[WIFI] Connected, IP: ");
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

  if (hasPsram) {
    config.frame_size = FRAMESIZE_VGA;
    config.jpeg_quality = 12;
    config.fb_count = 1;
    config.grab_mode = CAMERA_GRAB_LATEST;
    config.fb_location = CAMERA_FB_IN_PSRAM;
  } else {
    // Chế độ dự phòng để tránh frame buffer malloc failed.
    config.frame_size = FRAMESIZE_QVGA;
    config.jpeg_quality = 18;
    config.fb_count = 1;
    config.grab_mode = CAMERA_GRAB_WHEN_EMPTY;
    config.fb_location = CAMERA_FB_IN_DRAM;
  }

  esp_err_t error = esp_camera_init(&config);
  if (error != ESP_OK) {
    Serial.printf("[CAMERA] Init failed: 0x%x\n", error);
    return false;
  }

  sensor_t* sensor = esp_camera_sensor_get();
  if (sensor != nullptr) {
    sensor->set_framesize(sensor, hasPsram ? FRAMESIZE_VGA : FRAMESIZE_QVGA);
  }

  Serial.printf(
      "[CAMERA] Initialized, PSRAM=%s, buffers=%d\n",
      hasPsram ? "yes" : "no",
      hasPsram ? 2 : 1
  );
  return true;
}

void setCaptureRequested(bool active) {
  if (active == captureRequested) {
    return;
  }

  captureRequested = active;
  frameSuccessPrinted = false;

  Serial.println(active
      ? "[CAMERA] Capture started"
      : "[CAMERA] Capture stopped");
}

void pollCaptureCommand() {
  unsigned long interval = captureRequested
      ? COMMAND_ACTIVE_POLL_MS
      : COMMAND_IDLE_POLL_MS;

  if (millis() - lastCommandPollMs < interval) {
    return;
  }
  lastCommandPollMs = millis();

  if (WiFi.status() != WL_CONNECTED) {
    return;
  }

  HTTPClient http;
  WiFiClient client;
  String url = String(API_URL) + CAMERA_ID + "/capture-command";

  if (!http.begin(client, url)) {
    Serial.println("[COMMAND] Invalid URL");
    return;
  }

  http.setConnectTimeout(1500);
  http.setTimeout(2500);
  http.addHeader("INTERNAL-API-KEY", INTERNAL_API_KEY);

  int code = http.GET();

  if (code == HTTP_CODE_OK) {
    String response = http.getString();
    response.replace(" ", "");
    response.replace("\r", "");
    response.replace("\n", "");

    consecutiveCommandFailures = 0;
    setCaptureRequested(response.indexOf("\"active\":true") >= 0);
  } else {
    consecutiveCommandFailures++;

    // Không tắt phiên chỉ vì một gói polling bị rớt.
    if (consecutiveCommandFailures >= 3) {
      setCaptureRequested(false);
    }

    if (consecutiveCommandFailures == 1 || consecutiveCommandFailures % 5 == 0) {
      if (code < 0) {
        Serial.printf("[COMMAND] Failed: %s\n", http.errorToString(code).c_str());
      } else {
        Serial.printf("[COMMAND] HTTP error: %d\n", code);
      }
    }
  }

  http.end();
}

bool writeAll(WiFiClient& client, const uint8_t* data, size_t length) {
  size_t written = 0;
  unsigned long startedMs = millis();

  while (written < length && client.connected()) {
    size_t count = client.write(data + written, length - written);
    if (count > 0) {
      written += count;
      startedMs = millis();
    } else {
      if (millis() - startedMs > 5000) {
        break;
      }
      delay(1);
    }
  }

  return written == length;
}

bool writeAll(WiFiClient& client, const String& value) {
  return writeAll(
      client,
      reinterpret_cast<const uint8_t*>(value.c_str()),
      value.length()
  );
}

int sendFrameDirect(camera_fb_t* frame) {
  if (!apiBase.valid) {
    return -1000;
  }

  WiFiClient client;
  client.setTimeout(5000);

  if (!client.connect(apiBase.host.c_str(), apiBase.port)) {
    return -1;
  }

  const String boundary = "----ESP32CamBoundary";
  String multipartHead;
  multipartHead.reserve(240);
  multipartHead += "--" + boundary + "\r\n";
  multipartHead += "Content-Disposition: form-data; name=\"cameraId\"\r\n\r\n";
  multipartHead += String(CAMERA_ID) + "\r\n";
  multipartHead += "--" + boundary + "\r\n";
  multipartHead += "Content-Disposition: form-data; name=\"image\"; filename=\"frame.jpg\"\r\n";
  multipartHead += "Content-Type: image/jpeg\r\n\r\n";

  const String multipartTail = "\r\n--" + boundary + "--\r\n";
  const size_t contentLength = multipartHead.length() + frame->len + multipartTail.length();

  String requestHead;
  requestHead.reserve(360);
  requestHead += "POST " + apiBase.path + CAMERA_ID + "/frames HTTP/1.1\r\n";
  requestHead += "Host: " + apiBase.host + ":" + String(apiBase.port) + "\r\n";
  requestHead += "INTERNAL-API-KEY: " + String(INTERNAL_API_KEY) + "\r\n";
  requestHead += "Content-Type: multipart/form-data; boundary=" + boundary + "\r\n";
  requestHead += "Content-Length: " + String(contentLength) + "\r\n";
  requestHead += "Connection: close\r\n\r\n";

  bool sent = writeAll(client, requestHead)
      && writeAll(client, multipartHead)
      && writeAll(client, frame->buf, frame->len)
      && writeAll(client, multipartTail);

  if (!sent) {
    client.stop();
    return -2;
  }

  unsigned long responseStartedMs = millis();
  while (!client.available() && client.connected()) {
    if (millis() - responseStartedMs > 5000) {
      client.stop();
      return -3;
    }
    delay(1);
  }

  String statusLine = client.readStringUntil('\n');
  client.stop();

  int firstSpace = statusLine.indexOf(' ');
  int secondSpace = firstSpace >= 0 ? statusLine.indexOf(' ', firstSpace + 1) : -1;
  if (firstSpace < 0) {
    return -4;
  }

  String codeText = secondSpace >= 0
      ? statusLine.substring(firstSpace + 1, secondSpace)
      : statusLine.substring(firstSpace + 1);
  return codeText.toInt();
}

void sendFrame() {
  if (!captureRequested || WiFi.status() != WL_CONNECTED) {
    return;
  }

  if (millis() - lastFrameMs < FRAME_INTERVAL_MS) {
    return;
  }
  lastFrameMs = millis();

  camera_fb_t* frame = esp_camera_fb_get();
  if (frame == nullptr) {
    if (millis() - lastFrameErrorLogMs > 3000) {
      Serial.println("[FRAME] Camera capture failed");
      lastFrameErrorLogMs = millis();
    }
    return;
  }

  const String boundary = "----ESP32CamBoundary";

  String head = "--" + boundary + "\r\n";
  head +=
      "Content-Disposition: form-data; "
      "name=\"cameraId\"\r\n\r\n";
  head += String(CAMERA_ID) + "\r\n";
  head += "--" + boundary + "\r\n";
  head +=
      "Content-Disposition: form-data; "
      "name=\"image\"; filename=\"frame.jpg\"\r\n";
  head += "Content-Type: image/jpeg\r\n\r\n";

  const String tail = "\r\n--" + boundary + "--\r\n";
  const size_t totalLength =
      head.length() + frame->len + tail.length();

 uint8_t* postData =
    static_cast<uint8_t*>(malloc(totalLength));

  if (postData == nullptr) {
    Serial.println("[FRAME] Memory allocation failed");
    esp_camera_fb_return(frame);
    return;
  }

  memcpy(postData, head.c_str(), head.length());
  memcpy(
      postData + head.length(),
      frame->buf,
      frame->len
  );
  memcpy(
      postData + head.length() + frame->len,
      tail.c_str(),
      tail.length()
  );

  WiFiClient client;
  HTTPClient http;
  String url = String(API_URL) + CAMERA_ID + "/frames";
  int code = -1;

  if (http.begin(client, url)) {
    http.setConnectTimeout(2000);
    http.setTimeout(8000);
    http.setReuse(false);
    http.addHeader("INTERNAL-API-KEY", INTERNAL_API_KEY);
    http.addHeader(
        "Content-Type",
        "multipart/form-data; boundary=" + boundary
    );

    code = http.POST(postData, totalLength);
    http.end();
  }

  free(postData);
  esp_camera_fb_return(frame);

  if (code == 200) {
    static unsigned long successfulFrames = 0;
    successfulFrames++;

    if (!frameSuccessPrinted) {
      Serial.println("[FRAME] Connected - HTTP 200");
      frameSuccessPrinted = true;
    }

    // Xác nhận camera vẫn gửi liên tục nhưng không in HTTP 200 mỗi frame.
    if (successfulFrames % 10 == 0) {
      Serial.printf("[FRAME] Sent %lu frames\n", successfulFrames);
    }
    return;
  }

  frameSuccessPrinted = false;
  if (millis() - lastFrameErrorLogMs > 3000) {
    Serial.printf("[FRAME] Send failed, code=%d\n", code);
    lastFrameErrorLogMs = millis();
  }
}

void setup() {
  Serial.begin(115200);
  pinMode(FLASH_LED_PIN, OUTPUT);
  digitalWrite(FLASH_LED_PIN, LOW);

  apiBase = parseHttpUrl(API_URL);
  if (!apiBase.valid) {
    Serial.println("[CONFIG] API_URL must start with http://");
  }

  connectWiFi();

  if (!initCamera()) {
    // Không gọi esp_camera_init liên tục vì sẽ gây lỗi ISR/malloc.
    Serial.println("[CAMERA] Restarting in 3 seconds...");
    delay(3000);
    ESP.restart();
  }

  Serial.println("[CAMERA] Capture firmware V3 ready");
}

void loop() {
  if (WiFi.status() != WL_CONNECTED) {
    setCaptureRequested(false);

    if (millis() - lastWifiRetryMs >= WIFI_RETRY_INTERVAL_MS) {
      lastWifiRetryMs = millis();
      Serial.println("[WIFI] Reconnecting...");
      WiFi.disconnect();
      WiFi.begin("Minh Thu", "camsaike");
    }

    delay(20);
    return;
  }

  pollCaptureCommand();
  sendFrame();
  delay(5);
}