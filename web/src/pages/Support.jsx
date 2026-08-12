import React from 'react';
import { Send, AlertTriangle } from 'lucide-react';

export default function Support() {
  return (
    <div className="flex-col gap-6">
      <div className="flex items-center justify-between mb-2">
        <div>
          <h2>Hỗ trợ / Báo lỗi</h2>
          <p className="text-muted">Gửi yêu cầu hỗ trợ khi hệ thống hoặc thiết bị gặp sự cố</p>
        </div>
      </div>

      <div className="grid-content">
        <div className="card col-span-2">
          <h3 className="mb-4 flex items-center gap-2">
            <AlertTriangle size={20} className="text-muted" />
            Mô tả sự cố
          </h3>
          <form className="flex-col gap-4">
            <div className="form-group flex-col gap-2">
              <label className="text-sm text-muted">Loại sự cố</label>
              <select className="input">
                <option value="hardware">Phần cứng (Camera, ESP32, RFID, Loa/Đèn)</option>
                <option value="software">Phần mềm (Lỗi Web, Không tải được dữ liệu)</option>
                <option value="network">Mạng (Mất kết nối Wifi, Server lỗi)</option>
                <option value="other">Khác</option>
              </select>
            </div>
            
            <div className="form-group flex-col gap-2">
              <label className="text-sm text-muted">Mô tả chi tiết</label>
              <textarea 
                className="input" 
                rows="5" 
                placeholder="Vui lòng mô tả rõ sự cố bạn đang gặp phải (ví dụ: Không đọc được thẻ RFID, Camera bị mờ...)"
                style={{ resize: 'vertical' }}
              ></textarea>
            </div>

            <div className="form-group flex-col gap-2">
              <label className="text-sm text-muted">Thiết bị bị ảnh hưởng (nếu có)</label>
              <input type="text" className="input" placeholder="Mã thiết bị hoặc vị trí (ví dụ: Cửa A)" />
            </div>

            <div className="flex justify-end mt-4">
              <button type="button" className="btn btn-primary">
                <Send size={18} />
                Gửi báo cáo lỗi
              </button>
            </div>
          </form>
        </div>

        <div className="card">
          <h3 className="mb-4">Lịch sử báo cáo</h3>
          <div className="flex-col gap-4 text-sm">
            <div className="p-3 rounded-md" style={{ border: '1px solid var(--border-color)' }}>
              <div className="flex justify-between mb-2">
                <span className="badge badge-warning">Đang xử lý</span>
                <span className="text-muted">11/08/2026</span>
              </div>
              <p style={{ fontWeight: 500 }}>Lỗi không nhận thẻ RFID</p>
              <p className="text-muted mt-1">Cửa phòng thực hành C không phản hồi khi quẹt thẻ.</p>
            </div>
            
            <div className="p-3 rounded-md" style={{ border: '1px solid var(--border-color)', opacity: 0.7 }}>
              <div className="flex justify-between mb-2">
                <span className="badge badge-success">Đã giải quyết</span>
                <span className="text-muted">10/08/2026</span>
              </div>
              <p style={{ fontWeight: 500 }}>Mất kết nối ESP32-CAM</p>
              <p className="text-muted mt-1">Camera bị ngắt kết nối liên tục.</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
