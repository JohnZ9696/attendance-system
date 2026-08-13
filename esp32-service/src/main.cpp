#include <Arduino.h>
#include <WiFi.h>

namespace {
constexpr char kWifiSsid[] = "YOUR_WIFI_SSID";
constexpr char kWifiPassword[] = "YOUR_WIFI_PASSWORD";

void connectToWifi() {
  WiFi.mode(WIFI_STA);
  WiFi.begin(kWifiSsid, kWifiPassword);

  Serial.print("Connecting to Wi-Fi");
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print('.');
  }
  Serial.println();
  Serial.print("ESP32 IP: ");
  Serial.println(WiFi.localIP());
}
}  // namespace

void setup() {
  Serial.begin(115200);
  delay(500);
  connectToWifi();
  Serial.println("ESP32 attendance service ready");
}

void loop() {
  // RFID reader and ESP32-CAM integrations will publish attendance events here.
  delay(1000);
}
