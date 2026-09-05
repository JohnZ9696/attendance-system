#include <Arduino.h>
#include <HTTPClient.h>
#include <WiFi.h>
#include <WiFiUdp.h>
#include <esp_camera.h>
#include <WebServer.h>
#include <Preferences.h>

#include "../secrets.h"
#ifndef WIFI_SSID
#include "secrets.example.h"
#endif

WebServer server(80);
Preferences preferences;

String ssid = "";
String password = "";

const char* html_page = 
"<!DOCTYPE html><html><head><meta name='viewport' content='width=device-width, initial-scale=1'>"
"<style>body{font-family:Arial; margin:40px auto; max-width:400px; text-align:center;}"
"form{background:#f3f3f3; padding:20px; border-radius:8px;}"
"input{width:90%; padding:10px; margin:10px 0; border:1px solid #ccc; border-radius:4px;}"
"input[type=submit]{background:#4CAF50; color:white; cursor:pointer; font-weight:bold;}</style></head>"
"<body><h2>ESP32 Wi-Fi Config</h2>"
"<form action='/save' method='POST'>"
"<input type='text' name='ssid' placeholder='WiFi Name (SSID)' required><br>"
"<input type='password' name='password' placeholder='Password'><br>"
"<input type='submit' value='Save & Connect'>"
"</form></body></html>";

void handleRoot() {
  server.send(200, "text/html", html_page);
}

void handleSave() {
  if (server.hasArg("ssid")) {
    ssid = server.arg("ssid");
    password = server.arg("password");

    // Save to flash memory (NVS)
    preferences.begin("wifi-config", false);
    preferences.putString("ssid", ssid);
    preferences.putString("password", password);
    preferences.end();

    server.send(200, "text/html", "<h3>Settings saved! Reconnecting...</h3>");
    delay(2000);
    
    // Attempt to connect to the new network
    WiFi.begin(ssid.c_str(), password.c_str());
  } else {
    server.send(400, "text/plain", "Bad Request");
  }
}



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
constexpr uint16_t AI_DISCOVERY_PORT = 4211;
constexpr unsigned long COMMAND_POLL_INTERVAL_MS = 500;
constexpr unsigned long FRAME_INTERVAL_MS = 300;        // khi đang xác thực (≈3.3 FPS)
constexpr unsigned long PREVIEW_FRAME_INTERVAL_MS = 2000; // preview mode (0.5 FPS)
constexpr unsigned long WIFI_RETRY_INTERVAL_MS = 5000;

unsigned long lastCommandPollMs = 0;
unsigned long lastFrameMs = 0;
unsigned long lastWiFiRetryMs = 0;
bool captureRequested = false;
String aiServiceBaseUrl = AI_SERVICE_BASE_URL;
uint8_t transportFailureCount = 0;

String cameraUrl(const char* endpoint) {
  return aiServiceBaseUrl + "/internal/v1/cameras/" + CAMERA_ID + endpoint;
}

bool readDiscoveryResponse(
    WiFiUDP& udp,
    IPAddress& serverIp,
    uint16_t& serverPort,
    unsigned long timeoutMs) {
  constexpr char RESPONSE_PREFIX[] = "ATTENDANCE_AI_SERVER_V1:";
  const unsigned long deadline = millis() + timeoutMs;

  while (static_cast<long>(deadline - millis()) > 0) {
    if (udp.parsePacket() > 0) {
      char response[64] = {};
      const int bytesRead = udp.read(response, sizeof(response) - 1);
      if (bytesRead > 0 && strncmp(response, RESPONSE_PREFIX, strlen(RESPONSE_PREFIX)) == 0) {
        const long port = strtol(response + strlen(RESPONSE_PREFIX), nullptr, 10);
        if (port > 0 && port <= 65535) {
          serverIp = udp.remoteIP();
          serverPort = static_cast<uint16_t>(port);
          return true;
        }
      }
    }
    delay(5);
  }

  return false;
}

void sendDiscoveryRequest(WiFiUDP& udp, const IPAddress& target) {
  constexpr char REQUEST[] = "ATTENDANCE_AI_DISCOVER_V1";
  udp.beginPacket(target, AI_DISCOVERY_PORT);
  udp.write(reinterpret_cast<const uint8_t*>(REQUEST), sizeof(REQUEST) - 1);
  udp.endPacket();
}

bool discoverAiService() {
  WiFiUDP udp;
  if (!udp.begin(0)) {
    return false;
  }

  const IPAddress localIp = WiFi.localIP();
  const IPAddress mask = WiFi.subnetMask();
  const IPAddress broadcast(
      localIp[0] | static_cast<uint8_t>(~mask[0]),
      localIp[1] | static_cast<uint8_t>(~mask[1]),
      localIp[2] | static_cast<uint8_t>(~mask[2]),
      localIp[3] | static_cast<uint8_t>(~mask[3]));
  IPAddress serverIp;
  uint16_t serverPort = 0;

  Serial.println("[DISCOVERY] Looking for FastAPI...");
  for (uint8_t attempt = 0; attempt < 3; attempt++) {
    sendDiscoveryRequest(udp, broadcast);
    if (readDiscoveryResponse(udp, serverIp, serverPort, 500)) {
      break;
    }
  }

  // Phone hotspots commonly block broadcast between connected clients.
  if (serverPort == 0 && mask == IPAddress(255, 255, 255, 0)) {
    Serial.println("[DISCOVERY] Broadcast failed, scanning subnet...");
    for (uint16_t host = 1; host <= 254 && serverPort == 0; host++) {
      if (host == localIp[3]) {
        continue;
      }
      sendDiscoveryRequest(udp, IPAddress(localIp[0], localIp[1], localIp[2], host));
      readDiscoveryResponse(udp, serverIp, serverPort, 8);
    }
  }

  udp.stop();
  if (serverPort == 0) {
    Serial.printf("[DISCOVERY] FastAPI not found, fallback: %s\n", aiServiceBaseUrl.c_str());
    return false;
  }

  aiServiceBaseUrl = String("http://") + serverIp.toString() + ":" + String(serverPort);
  Serial.printf("[DISCOVERY] FastAPI: %s\n", aiServiceBaseUrl.c_str());
  return true;
}

void handleHttpResult(int statusCode) {
  if (statusCode >= 0) {
    transportFailureCount = 0;
    return;
  }

  transportFailureCount++;
  if (transportFailureCount >= 3) {
    transportFailureCount = 0;
    discoverAiService();
  }
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

  WiFi.persistent(false);
  WiFi.setAutoReconnect(true);
  WiFi.setSleep(false);

  preferences.begin("wifi-config", true);
  ssid = preferences.getString("ssid", "");
  password = preferences.getString("password", "");
  preferences.end();

  if (ssid != "") {
    Serial.print("[WIFI] Connecting to ");
    Serial.println(ssid);
    WiFi.begin(ssid.c_str(), password.c_str());
  } else {
    Serial.println("[WIFI] No saved Wi-Fi credentials found");
  }

  const unsigned long startedMs = millis();
  while (WiFi.status() != WL_CONNECTED && millis() - startedMs < 15000) {
    Serial.print('.');
    delay(250);
  }

  if (WiFi.status() == WL_CONNECTED) {
    Serial.print("\n[WIFI] Connected: ");
    Serial.println(WiFi.localIP());
    discoverAiService();
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
  handleHttpResult(statusCode);
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
  handleHttpResult(statusCode);

  if (statusCode != HTTP_CODE_OK) {
    Serial.printf("[FRAME] Upload failed: %d\n", statusCode);
  }
}

void setup() {
  Serial.begin(115200);

  WiFi.mode(WIFI_AP_STA);
  IPAddress local_IP(192, 168, 4, 1);
  IPAddress gateway(192, 168, 4, 1);
  IPAddress subnet(255, 255, 255, 0);
  WiFi.softAPConfig(local_IP, gateway, subnet);
  WiFi.softAP("ESP32-Cam-Config", "12345678"); 
  Serial.print("Access Point IP: ");
  Serial.println(WiFi.softAPIP());

  server.on("/", handleRoot);
  server.on("/save", HTTP_POST, handleSave);
  server.begin();
  Serial.println("Web server started.");

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
  server.handleClient();

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

  // Luôn gửi frame để preview trên web có hình
  unsigned long interval = captureRequested ? FRAME_INTERVAL_MS : PREVIEW_FRAME_INTERVAL_MS;
  if (millis() - lastFrameMs >= interval) {
    lastFrameMs = millis();
    sendFrame();
  }

  delay(5);
}
