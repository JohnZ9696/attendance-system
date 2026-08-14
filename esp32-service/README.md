# ESP32 Service

PlatformIO firmware workspace for the attendance hardware. The initial target
connects an ESP32 to Wi-Fi and provides a place for the RFID RC522 and
ESP32-CAM integrations.

## Setup

1. Install PlatformIO Core or the PlatformIO IDE extension.
2. Update the Wi-Fi credentials in `src/main.cpp` for local testing.
3. Build and upload:

```bash
pio run
pio run --target upload
pio device monitor
```

The device-side HTTP clients should call the Spring Boot API gateway. The AI
service is reached through the gateway rather than directly from the device.
