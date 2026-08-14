from deepface import DeepFace
import cv2
import serial
import time
import os
from scipy.spatial import distance
import numpy as np

# Load Haar Cascade classifiers
face_cascade = cv2.CascadeClassifier(cv2.data.haarcascades + 'haarcascade_frontalface_default.xml')
eye_cascade = cv2.CascadeClassifier(cv2.data.haarcascades + 'haarcascade_eye.xml')

try:
    ser = serial.Serial("COM5", 115200, timeout=1)
    time.sleep(2)
except Exception as e:
    print("Không thể mở cổng COM5:", e)
    raise

print("Đã kết nối với ESP32 qua COM5")

LEFT_EYE = [33,160,158,133,153,144]
RIGHT_EYE = [362,385,387,263,373,380]

blink_state = {
    "closed_frames": 0,
    "open_frames": 0,
    "blinked": False,
}

def blink_detect(frame, state, close_frames=3, open_frames=2):
    """Detect blink by tracking eye detection across consecutive frames."""
    gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
    faces = face_cascade.detectMultiScale(gray, 1.3, 5)

    if len(faces) == 0:
        state["open_frames"] = 0
        state["closed_frames"] = 0
        return False

    x, y, w, h = faces[0]
    roi_gray = gray[y:y+h, x:x+w]
    eyes = eye_cascade.detectMultiScale(roi_gray, scaleFactor=1.1, minNeighbors=5, minSize=(20, 20))

    if len(eyes) >= 2:
        state["open_frames"] += 1
        state["closed_frames"] = 0
        if state["open_frames"] >= open_frames:
            state["blinked"] = False
        return False

    state["closed_frames"] += 1
    state["open_frames"] = 0
    if state["closed_frames"] >= close_frames and not state["blinked"]:
        state["blinked"] = True
        return True

    return False

while True:
    try:
        line = ser.readline().decode(errors="ignore").strip()
        print(repr(line))
    except Exception as e:
        print("Lỗi đọc serial:", e)
        continue

    if not line:
        continue

    print("Serial:", line)

    if line.startswith("RFID:"):
        uid = line.replace("RFID:", "").strip()
        print("Đã quét UID:", uid)

        image_path = f"database/{uid}.jpg"
        if not os.path.exists(image_path):
            print("Không tìm thấy ảnh mẫu cho UID:", uid)
            ser.write(b"DENY\n")
            continue

        cap = cv2.VideoCapture(0)
        if not cap.isOpened():
            print("Không thể mở camera")
            ser.write(b"DENY\n")
            continue

        start_time = time.time()
        timeout = 10  # seconds to attempt verification
        verified = False
        print("Hãy chớp mắt để xác thực...")

        blinked = False

        while time.time() - start_time < timeout:
            ret, frame = cap.read()
            if not ret or frame is None:
                continue

            if blink_detect(frame, blink_state):
                blinked = True
                cv2.putText(frame,
                            "Blink detected",
                            (20, 40),
                            cv2.FONT_HERSHEY_SIMPLEX,
                            1,
                            (0, 255, 0),
                            2)
            else:
                cv2.putText(frame,
                            "Please blink",
                            (20, 40),
                            cv2.FONT_HERSHEY_SIMPLEX,
                            1,
                            (0, 255, 255),
                            2)

            cv2.imshow("Face Verification", frame)

            if cv2.waitKey(1) & 0xFF == ord('q'):
                print("Hủy quét bởi người dùng")
                break

            if not blinked:
                continue

            cv2.imwrite("temp.jpg", frame)
            try:
                result = DeepFace.verify(
                    img1_path="temp.jpg",
                    img2_path=image_path,
                    enforce_detection=False
                )
                if result.get("verified"):
                    verified = True
                    print("Xác thực thành công")
                    ser.write(b"OPEN\n")
                    break
                else:
                    print("Xác thực thất bại, thử lại...")
                    blinked = False
            except Exception as e:
                print("Lỗi xác thực khuôn mặt:", e)
                time.sleep(0.2)
                blinked = False
                continue

        cap.release()
        cv2.destroyAllWindows()

        if not verified:
            print("Không xác thực được trong thời gian cho phép")
            ser.write(b"DENY\n")
