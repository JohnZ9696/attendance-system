import React, { useEffect, useState } from 'react';
import { Save } from 'lucide-react';
import { apiClient } from '../api/client';

export default function Settings() {
  const [startTime, setStartTime] = useState('07:30');
  const [graceMinutes, setGraceMinutes] = useState(15);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState('');

  const loadSettings = async () => {
    setLoading(true);
    setMessage('');
    try {
      const settings = await apiClient.getSettings();
      setStartTime(settings.classStartTime?.slice(0, 5) || '07:30');
      setGraceMinutes(settings.lateGraceMinutes ?? 15);
    } catch {
      setMessage('Không thể tải cài đặt. Kiểm tra kết nối backend.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadSettings();
  }, []);

  const handleSave = async () => {
    setSaving(true);
    setMessage('');
    try {
      await apiClient.updateSettings({
        classStartTime: startTime,
        lateGraceMinutes: Number(graceMinutes),
      });
      setMessage('Đã lưu cài đặt thời gian điểm danh.');
    } catch {
      setMessage('Không thể lưu cài đặt. Kiểm tra kết nối backend.');
    } finally {
      setSaving(false);
    }
  };

  const threshold = (() => {
    const [h, m] = startTime.split(':').map(Number);
    const total = h * 60 + m + Number(graceMinutes);
    return `${String(Math.floor(total / 60) % 24).padStart(2, '0')}:${String(total % 60).padStart(2, '0')}`;
  })();

  return (
    <div className="flex-col gap-6">
      <div className="flex items-center justify-between mb-2">
        <div>
          <h2>Cài đặt hệ thống</h2>
          <p className="text-muted">Cấu hình thời gian, cảnh báo và tích hợp thông báo</p>
        </div>
        <button className="btn btn-primary" onClick={handleSave} disabled={saving || loading}>
          <Save size={18} />
          Lưu thay đổi
        </button>
      </div>

      {message && <div className="text-sm" style={{ color: 'var(--status-success)' }}>{message}</div>}

      <div className="grid-content">
        <div className="flex-col gap-6" style={{ gridColumn: 'span 2' }}>

          {/* Time Settings */}
          <div className="card">
            <h3 className="mb-4">Cài đặt thời gian điểm danh</h3>
            <div className="grid" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1.5rem' }}>
              <div className="form-group flex-col gap-2">
                <label className="text-sm text-muted">Giờ bắt đầu học (Vào lớp)</label>
                <input
                  type="time"
                  className="input"
                  value={startTime}
                  disabled={loading}
                  onChange={(event) => setStartTime(event.target.value)}
                />
              </div>
              <div className="form-group flex-col gap-2">
                <label className="text-sm text-muted">Thời gian trễ cho phép (phút)</label>
                <input
                  type="number"
                  className="input"
                  min="0"
                  max="180"
                  value={graceMinutes}
                  disabled={loading}
                  onChange={(event) => setGraceMinutes(event.target.value)}
                />
                <span className="text-sm text-muted">Sau {threshold} sẽ tính là đi muộn.</span>
              </div>
            </div>
          </div>

          {/* Email / SMS Config */}
          <div className="card">
            <h3 className="mb-4">Cấu hình thông báo (Email / SMS)</h3>
            <div className="grid" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1.5rem' }}>
              <div className="form-group flex-col gap-2">
                <label className="text-sm text-muted">SMTP Server (Email)</label>
                <input type="text" className="input" defaultValue="smtp.gmail.com" />
              </div>
              <div className="form-group flex-col gap-2">
                <label className="text-sm text-muted">Email gửi thông báo</label>
                <input type="email" className="input" defaultValue="admin@school.edu.vn" />
              </div>
              <div className="form-group flex-col gap-2">
                <label className="text-sm text-muted">SMS API Key (Twilio/VietGuys)</label>
                <input type="password" className="input" defaultValue="********" />
              </div>
              <div className="form-group flex-col gap-2">
                <label className="text-sm text-muted">Kích hoạt thông báo phụ huynh khi đi muộn</label>
                <select className="input">
                  <option value="yes">Có (Email & SMS)</option>
                  <option value="email_only">Chỉ Email</option>
                  <option value="no">Không</option>
                </select>
              </div>
            </div>
          </div>

        </div>

        {/* System Info */}
        <div className="card h-fit">
          <h3 className="mb-4">Thông tin phần cứng</h3>
          <ul className="flex-col gap-3">
            <li className="flex justify-between items-center pb-2" style={{ borderBottom: '1px solid var(--border-color)' }}>
              <span className="text-muted">ESP32 Firmware:</span>
              <span style={{ fontWeight: 500 }}>v1.2.4</span>
            </li>
            <li className="flex justify-between items-center pb-2" style={{ borderBottom: '1px solid var(--border-color)' }}>
              <span className="text-muted">Database:</span>
              <span style={{ fontWeight: 500, color: 'var(--status-success)' }}>Supabase (Connected)</span>
            </li>
            <li className="flex justify-between items-center pb-2" style={{ borderBottom: '1px solid var(--border-color)' }}>
              <span className="text-muted">Liveness Detecion:</span>
              <span className="badge badge-success">Bật</span>
            </li>
          </ul>
        </div>
      </div>
    </div>
  );
}