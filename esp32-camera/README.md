# ESP32-CAM Service

The camera sends preview and verification frames directly to FastAPI.

## Configuration

Copy `src/secrets.example.h` to `secrets.h` in this directory and configure
Wi-Fi, the internal API key, and a fallback FastAPI URL. The fallback URL is
used only when UDP discovery is unavailable.

## FastAPI Discovery

FastAPI listens on UDP port `4211`. After joining Wi-Fi, the camera broadcasts
`ATTENDANCE_AI_DISCOVER_V1` and expects `ATTENDANCE_AI_SERVER_V1:<http-port>`.
If a hotspot blocks broadcast, the camera scans a `/24` subnet with unicast UDP.

Allow inbound UDP `4211` in the FastAPI host firewall and start FastAPI before
resetting the camera.
