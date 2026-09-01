# Debug LED Feedback - Đồ Án Điểm Danh

## Tổng quan thay đổi đèn báo

Điều chỉnh logic đèn LED (xanh/đỏ) + buzzer trên **ESP32 Gateway** để phản ánh đúng trạng thái điểm danh từ Spring Boot.

---

## Mapping trạng thái → Đèn báo

| Trường hợp | Error Code từ Spring | LED | Buzzer | Ý nghĩa |
|---|---|---|---|---|
| **UID không tồn tại trong Supabase** | `STUDENT_NOT_FOUND_OR_INACTIVE` | 🔴 Đỏ | 1 beep ngắn | Thẻ chưa đăng ký hoặc đã vô hiệu hóa |
| **UID chưa đăng ký khuôn mặt** | `FACE_NOT_ENROLLED` | 🔴 Đỏ | 1 beep ngắn | Sinh viên có thẻ nhưng chưa enroll face |
| **Nhận diện đa khuôn mặt** | `MULTIPLE_FACES` | 🔴 Đỏ | 2 beep ngắn | Camera thấy >1 khuôn mặt trong khung hình |
| **Liveness thất bại (không nháy mắt)** | `LIVENESS_FAILED` / `OPEN_CLOSED_OPEN_BLINK_NOT_COMPLETED` | 🔴 Đỏ | 2 beep ngắn | Không hoàn thành chuỗi mở-nhắm-mở mắt |
| **Khuôn mặt không khớp (dưới ngưỡng quản trị, tối thiểu 30%)** | `FACE_BELOW_THRESHOLD` / `SIMILARITY_BELOW_THRESHOLD` | 🔴 Đỏ | 1 beep dài | Sai người / khuôn mặt không giống đăng ký |
| **Hết thời gian chụp / không thấy mặt** | `CAPTURE_TIMEOUT` / `NO_FACE_IN_CAPTURE_WINDOW` | 🔴 Đỏ | 2 beep ngắn | Camera không nhận diện được mặt trong thời gian cho phép |
| **Camera offline** | `CAMERA_OFFLINE` / `NO_FRESH_CAMERA_FRAME` | 🔴 Đỏ | 2 beep ngắn | ESP32-CAM không gửi frame |
| **Hết thời gian so khớp** | `FACE_MATCH_TIMEOUT` / `FACE_MODEL_TIMEOUT` | 🔴 Đỏ | 2 beep ngắn | Quá lâu không tìm thấy khuôn mặt hợp lệ |
| **Đã điểm danh hôm nay** | `ALREADY_CHECKED_IN` | 🟢 Xanh nháy | 1 beep ngắn | Sinh viên đã check-in thành công trước đó |
| **Điểm danh đúng giờ** | `VERIFIED` + status `ON_TIME` | 🟢 Xanh | 1 beep ngắn | Thành công, trước 07:30 |
| **Điểm danh trễ** | `VERIFIED` + status `LATE` | 🟢 Xanh | 2 beep ngắn | Thành công, sau 07:30 |
| **Lỗi hệ thống / không kết nối được FastAPI** | `FASTAPI_UNAVAILABLE_OR_TIMEOUT` / `CLOUD_WRITE_FAILED` | 🔴 Đỏ | 1 beep dài (500ms) | Lỗi server/mạng |

---

## Chi tiết implementation

### 1. ESP32 Gateway - `esp32-service/src/main.cpp`

```cpp
enum class FeedbackState : uint8_t {
    IDLE,
    PROCESSING,           // 🟢 Xanh nhấp nháy - đang chờ FastAPI
    RFID_INVALID,         // 🔴 Đỏ - UID không hợp lệ
    CAMERA_OFFLINE,       // 🔴 Đỏ - Camera không online
    CAPTURE_TIMEOUT,      // 🔴 Đỏ - Hết thời gian chụp
    LIVENESS_FAILED,      // 🔴 Đỏ - Không nháy mắt
    FACE_BELOW_THRESHOLD, // 🔴 Đỏ - Sai khuôn mặt
    FACE_MATCH_TIMEOUT,   // 🔴 Đỏ - Hết thời gian so khớp
    MULTIPLE_FACES,       // 🔴 Đỏ - Nhiều khuôn mặt (thêm mới)
    ALREADY_CHECKED_IN,   // 🟢 Xanh nháy - Đã điểm danh
    CHECK_IN_ON_TIME,     // 🟢 Xanh - Đúng giờ
    CHECK_IN_LATE,        // 🟢 Xanh - Đi trễ
    CLOUD_WRITE_FAILED,   // 🔴 Đỏ - Lỗi server
    INCIDENT_RECORDED     // 🟢 Xanh - Gửi hỗ trợ thành công
};
```

**Hàm `updateFeedback()` - logic đèn:**

```cpp
void updateFeedback() {
    if (feedbackState == FeedbackState::IDLE) return;
    const unsigned long elapsed = millis() - stateStartMs;

    bool r_led = false, g_led = false, buzz = false;
    unsigned long duration = 2000UL;

    switch (feedbackState) {
        case FeedbackState::PROCESSING:
            g_led = true;                    // Xanh sáng liên tục
            buzz = (elapsed < 100);          // Beep ngắn lúc bắt đầu
            duration = 60000UL;              // Chờ tối đa 60s
            break;

        // ===== TẤT CẢ LỖI -> ĐỎ =====
        case FeedbackState::RFID_INVALID:           // UID không có trong DB
        case FeedbackState::CAMERA_OFFLINE:
        case FeedbackState::CAPTURE_TIMEOUT:
        case FeedbackState::LIVENESS_FAILED:
        case FeedbackState::FACE_MATCH_TIMEOUT:
        case FeedbackState::MULTIPLE_FACES:         // MỚI: đa khuôn mặt
            r_led = true;
            buzz = (elapsed < 100) || (elapsed >= 200 && elapsed < 300); // 2 beep ngắn
            break;

        case FeedbackState::FACE_BELOW_THRESHOLD:   // Sai khuôn mặt
            r_led = true;
            buzz = (elapsed < 1000);                // 1 beep dài
            break;

        // ===== THÀNH CÔNG -> XANH =====
        case FeedbackState::ALREADY_CHECKED_IN:
            g_led = (elapsed % 500) < 250;          // Xanh nháy
            duration = 2000UL;
            break;

        case FeedbackState::CHECK_IN_ON_TIME:
            g_led = true;
            buzz = (elapsed < 100);                 // 1 beep ngắn
            break;

        case FeedbackState::CHECK_IN_LATE:
            g_led = true;
            buzz = (elapsed < 100) || (elapsed >= 200 && elapsed < 300); // 2 beep ngắn
            break;

        case FeedbackState::CLOUD_WRITE_FAILED:
            r_led = true;
            buzz = (elapsed < 500);                 // Beep dài 500ms
            break;

        case FeedbackState::INCIDENT_RECORDED:
            g_led = true;
            buzz = (elapsed < 100);
            duration = 1000UL;
            break;
    }

    digitalWrite(LED_RED_PIN, r_led ? HIGH : LOW);
    digitalWrite(LED_GREEN_PIN, g_led ? HIGH : LOW);
    setBuzzer(buzz);

    if (elapsed >= duration) {
        allOff();
        feedbackState = FeedbackState::IDLE;
        showOled("SAN SANG", "Moi quet the");
    }
}
```

---

### 2. Spring Boot - Error Codes trả về

**`DeviceController.java` + `CheckInFlowService.java`:**

| Error Code | Khi nào xảy ra | HTTP Status |
|---|---|---|
| `STUDENT_NOT_FOUND_OR_INACTIVE` | UID không tìm thấy hoặc `is_active=false` | 404 |
| `FACE_NOT_ENROLLED` | `student.face_embedding` null/empty | 422 |
| `MULTIPLE_FACES` | FastAPI detect >1 face trong frame | 422 |
| `LIVENESS_FAILED` | Không hoàn thành blink sequence | 422 |
| `FACE_BELOW_THRESHOLD` | Similarity thấp hơn ngưỡng quản trị (30-100%) | 422 |
| `CAPTURE_TIMEOUT` | Không thấy face trong `captureTimeoutMs` | 422 |
| `CAMERA_OFFLINE` | ESP32-CAM không gửi frame | 422 |
| `FACE_MATCH_TIMEOUT` | Hết `matchTimeoutMs` chưa match được | 422 |
| `ALREADY_CHECKED_IN` | Unique constraint `(student_id, attendance_date)` | 200 (với errorCode) |
| `VERIFIED` + `ON_TIME`/`LATE` | Thành công | 200 |

---

### 3. FastAPI - Trả về kết quả CV

**`verification_manager.py`:**

```python
# MULTIPLE_FACES
if face_result.error == "MULTIPLE_FACES":
    return _response(request, VerificationResultEnum.ERROR,
        similarity=best_similarity, liveness=True, reason="MULTIPLE_FACES")

# LIVENESS_FAILED
if blink_frame is None:
    if saw_face:
        return _response(request, VerificationResultEnum.LIVENESS_FAILED,
            reason="OPEN_CLOSED_OPEN_BLINK_NOT_COMPLETED")
    return _response(request, VerificationResultEnum.CAPTURE_TIMEOUT,
        reason="NO_FACE_IN_CAPTURE_WINDOW")

# FACE_BELOW_THRESHOLD (ngưỡng do Spring Boot truyền từ cấu hình quản trị)
if best_similarity < request.similarityThresholdPercent:
    return _response(request, VerificationResultEnum.FACE_BELOW_THRESHOLD,
        similarity=best_similarity, liveness=True, reason="SIMILARITY_BELOW_THRESHOLD")
```

Spring Boot kiểm tra lại `similarityPercent`, ngưỡng của phiên và `livenessPassed`
trước khi quyết định ghi điểm danh. FastAPI không ghi dữ liệu điểm danh.

---

### 4. OLED hiển thị kèm theo

| Trạng thái | OLED Line 1 | OLED Line 2 |
|---|---|---|
| RFID_INVALID | THAT BAI | THE KHONG HOP LE |
| FACE_NOT_ENROLLED | THAT BAI | CHUA DANG KY MAT |
| MULTIPLE_FACES | THAT BAI | NHIEU KHUON MAT |
| LIVENESS_FAILED | THAT BAI | LIVENESS FAILED |
| FACE_BELOW_THRESHOLD | THAT BAI | FACE NOT MATCH |
| CAPTURE_TIMEOUT | THAT BAI | CAPTURE TIMEOUT |
| CAMERA_OFFLINE | THAT BAI | CAMERA OFFLINE |
| FACE_MATCH_TIMEOUT | THAT BAI | FACE TIMEOUT |
| ALREADY_CHECKED_IN | THONG BAO | DA DIEM DANH |
| CHECK_IN_ON_TIME | THANH CONG | DUNG GIO |
| CHECK_IN_LATE | THANH CONG | DI TRE |
| CLOUD_WRITE_FAILED | LOI HE THONG | KHONG GUI DUOC |

---

## Checklist test

- [ ] Quét thẻ không có trong Supabase → 🔴 Đỏ + beep ngắn + OLED "THE KHONG HOP LE"
- [ ] Quét thẻ có UID nhưng chưa enroll face → 🔴 Đỏ + beep ngắn + OLED "CHUA DANG KY MAT"
- [ ] 2 người đứng trước camera cùng lúc → 🔴 Đỏ + 2 beep + OLED "NHIEU KHUON MAT"
- [ ] Quét thẻ, không nháy mắt → 🔴 Đỏ + 2 beep + OLED "LIVENESS FAILED"
- [ ] Người lạ quét thẻ người khác → 🔴 Đỏ + beep dài + OLED "FACE NOT MATCH"
- [ ] Che camera/quay đi → 🔴 Đỏ + 2 beep + OLED "CAPTURE TIMEOUT" hoặc "CAMERA OFFLINE"
- [ ] Quét thẻ đã điểm danh hôm nay → 🟢 Xanh nháy + beep + OLED "DA DIEM DANH"
- [ ] Sinh viên đúng, nháy mắt chuẩn, trước 07:30 → 🟢 Xanh + beep + OLED "DUNG GIO"
- [ ] Sinh viên đúng, nháy mắt chuẩn, sau 07:30 → 🟢 Xanh + 2 beep + OLED "DI TRE"

---

## File liên quan cần cập nhật

1. `esp32-service/src/main.cpp` - FeedbackState enum, updateFeedback(), showFeedbackOnOled()
2. `backend/src/main/java/com/iot/attendance/service/CheckInFlowService.java` - handleRfidScan(), complete()
3. `backend/src/main/java/com/iot/attendance/controller/DeviceController.java` - handleScan()
4. `ai-service/app/services/verification_manager.py` - run_verification() error handling
5. `ai-service/app/services/face_recognition.py` - extract_face_embedding() trả về MULTIPLE_FACES
