import React, { useEffect, useState } from 'react';
import { CalendarOff, ChevronLeft, ChevronRight, Save, X } from 'lucide-react';
import { apiClient } from '../api/client';
import { Skeleton } from '../components/Skeleton';
import { useToast } from '../components/useToast.js';

const WEEKDAY_LABELS = ['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN'];
const WEEKDAY_INDEX = [1, 2, 3, 4, 5, 6, 0];

const dateKey = (date) => `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;

export default function Settings() {
  const [startTime, setStartTime] = useState('07:30');
  const [graceMinutes, setGraceMinutes] = useState(15);
  const [dayOffs, setDayOffs] = useState([]);
  const [weeklyDayOffs, setWeeklyDayOffs] = useState([0, 6]);
  const [calendarMonth, setCalendarMonth] = useState(() => new Date(new Date().getFullYear(), new Date().getMonth(), 1));
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState('');
  const toast = useToast();

  const loadSettings = async () => {
    setLoading(true);
    setMessage('');
    try {
      const settings = await apiClient.getSettings();
      setStartTime(settings.classStartTime?.slice(0, 5) || '07:30');
      setGraceMinutes(settings.lateGraceMinutes ?? 15);
      setDayOffs(Array.isArray(settings.dayOffs) ? settings.dayOffs.map((value) => value.slice(0, 10)) : []);
      setWeeklyDayOffs(Array.isArray(settings.weeklyDayOffs) ? settings.weeklyDayOffs : [0, 6]);
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
        dayOffs,
        weeklyDayOffs,
      });
      toast('Đã lưu cài đặt thành công.', 'success');
      setMessage('Đã lưu thay đổi.');
    } catch {
      toast('Không thể lưu cài đặt.', 'error');
      setMessage('Không thể lưu cài đặt.');
    } finally {
      setSaving(false);
    }
  };

  const toggleDayOff = (value) => {
    setDayOffs((current) => (current.includes(value) ? current.filter((item) => item !== value) : [...current, value].sort()));
  };

  const toggleWeeklyDayOff = (dayIndex) => {
    setWeeklyDayOffs((current) => (current.includes(dayIndex) ? current.filter((item) => item !== dayIndex) : [...current, dayIndex].sort()));
  };

  const removeDayOff = (value) => setDayOffs((current) => current.filter((item) => item !== value));

  const changeMonth = (delta) => {
    setCalendarMonth(new Date(calendarMonth.getFullYear(), calendarMonth.getMonth() + delta, 1));
  };

  const monthLabel = calendarMonth.toLocaleDateString('vi-VN', { month: 'long', year: 'numeric' });
  const todayKey = dateKey(new Date());
  const daysInMonth = new Date(calendarMonth.getFullYear(), calendarMonth.getMonth() + 1, 0).getDate();
  const leadingBlanks = (new Date(calendarMonth.getFullYear(), calendarMonth.getMonth(), 1).getDay() + 6) % 7;
  const calendarCells = [...Array(leadingBlanks).fill(null), ...Array.from({ length: daysInMonth }, (_, index) => index + 1)];

  const threshold = (() => {
    const [h, m] = startTime.split(':').map(Number);
    const total = h * 60 + m + Number(graceMinutes);
    return `${String(Math.floor(total / 60) % 24).padStart(2, '0')}:${String(total % 60).padStart(2, '0')}`;
  })();

  return (
    <div className="page-stack">
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
      {loading ? (
        <div className="flex-col gap-6" style={{ marginTop: 24 }}>
          <div className="card"><Skeleton className="skeleton-card-label" style={{ marginBottom: 20 }} /><Skeleton className="skeleton-card-value" style={{ width: '60%' }} /><Skeleton className="skeleton-card-label" style={{ marginTop: 20, width: '40%' }} /></div>
          <div className="card"><Skeleton className="skeleton-card-label" style={{ marginBottom: 20 }} /><Skeleton className="skeleton-card-value" style={{ width: '50%' }} /></div>
        </div>
      ) : (
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

          {/* Days off */}
          <div className="card">
            <h3 className="mb-4">Ngày nghỉ (không tính vắng)</h3>
            <p className="text-sm text-muted" style={{ marginBottom: 12 }}>Chọn thứ nghỉ cố định hàng tuần, và bấm vào ngày trên lịch để đánh dấu ngày nghỉ đặc biệt.</p>

            <label className="text-sm text-muted" style={{ display: 'block', marginBottom: 8 }}>Nghỉ cố định hàng tuần</label>
            <div className="weekly-day-off-toggle">
              {WEEKDAY_INDEX.map((dayIndex, index) => {
                const selected = weeklyDayOffs.includes(dayIndex);
                return (
                  <button
                    type="button"
                    key={WEEKDAY_LABELS[index]}
                    className={`weekly-day-off-chip${selected ? ' selected' : ''}`}
                    onClick={() => toggleWeeklyDayOff(dayIndex)}
                    aria-pressed={selected}
                  >
                    {WEEKDAY_LABELS[index]}
                  </button>
                );
              })}
            </div>

            <div className="day-off-calendar">
              <div className="day-off-calendar-header">
                <button className="icon-btn" type="button" onClick={() => changeMonth(-1)} aria-label="Tháng trước"><ChevronLeft size={18} /></button>
                <strong>{monthLabel}</strong>
                <button className="icon-btn" type="button" onClick={() => changeMonth(1)} aria-label="Tháng sau"><ChevronRight size={18} /></button>
              </div>
              <div className="day-off-weekdays">
                {WEEKDAY_LABELS.map((label) => <span className="day-off-weekday" key={label}>{label}</span>)}
              </div>
              <div className="day-off-grid">
                {calendarCells.map((day, index) => {
                  if (day === null) return <span className="day-off-day day-off-blank" key={`blank-${index}`} />;
                  const value = `${calendarMonth.getFullYear()}-${String(calendarMonth.getMonth() + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
                  const selected = dayOffs.includes(value);
                  const isToday = value === todayKey;
                  return (
                    <button
                      type="button"
                      className={`day-off-day${selected ? ' selected' : ''}${isToday ? ' today' : ''}`}
                      key={value}
                      onClick={() => toggleDayOff(value)}
                      aria-pressed={selected}
                      title={selected ? 'Bỏ ngày nghỉ' : 'Đánh dấu ngày nghỉ'}
                    >
                      {day}
                    </button>
                  );
                })}
              </div>
            </div>

            {dayOffs.length ? (
              <>
                <p className="text-sm text-muted" style={{ margin: '14px 0 8px' }}>Đã chọn {dayOffs.length} ngày nghỉ:</p>
                <div className="day-off-list">
                  {dayOffs.map((value) => (
                    <span className="day-off-chip" key={value}>
                      <CalendarOff size={14} />
                      {new Date(`${value}T00:00:00`).toLocaleDateString('vi-VN', { weekday: 'short', day: '2-digit', month: '2-digit', year: 'numeric' })}
                      <button type="button" className="day-off-remove" onClick={() => removeDayOff(value)} aria-label="Xóa ngày nghỉ"><X size={13} /></button>
                    </span>
                  ))}
                </div>
              </>
            ) : <p className="text-sm text-muted" style={{ marginTop: 14 }}>Chưa có ngày nghỉ nào.</p>}
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
      )}
    </div>
  );
}