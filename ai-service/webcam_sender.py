import time
import requests
import cv2

API_URL = "http://127.0.0.1:8000/internal/v1/cameras/cam-01/frames"
INTERNAL_API_KEY = "s5HpmgoZ4Wl5A9v8pJ6qLuAyWIrAZLU_nP3W3AeaUDc"

camera = cv2.VideoCapture(0)

if not camera.isOpened():
    raise RuntimeError("Không mở được webcam. Thử đổi VideoCapture(1).")

while True:
    ok, frame = camera.read()

    if not ok:
        print("Không đọc được webcam")
        break

    ok, jpeg = cv2.imencode(".jpg", frame)

    if not ok:
        continue

    try:
        response = requests.post(
            API_URL,
            headers={"INTERNAL-API-KEY": INTERNAL_API_KEY},
            data={"cameraId": "cam-01"},
            files={"image": ("frame.jpg", jpeg.tobytes(), "image/jpeg")},
            timeout=5,
        )
        print("Frame:", response.status_code)
    except requests.RequestException as error:
        print("Send failed:", error)

    cv2.imshow("Webcam sender - press Q to stop", frame)

    if cv2.waitKey(1) & 0xFF == ord("q"):
        break

    time.sleep(0.15)  # khoảng 6–7 frame/giây

camera.release()
cv2.destroyAllWindows()