import React from 'react';
import { Camera, Wifi, CheckCircle, AlertCircle } from 'lucide-react';

export default function Monitoring() {
  return (
    <div className="flex-col gap-6">
      <div className="flex items-center justify-between mb-2">
        <div>
          <h2>Giám sát trực tiếp</h2>
          <p className="text-muted">Xem camera trực tiếp và trạng thái thiết bị ESP32</p>
        </div>
      </div>

      <div className="grid-content">
        {/* Camera Feed */}
        <div className="card col-span-2">
          <div className="flex items-center justify-between mb-4">
            <h3 className="flex items-center gap-2">
              <Camera size={20} />
              Luồng Camera (ESP32-CAM)
            </h3>
            <span className="badge badge-success flex items-center gap-2">
              <span className="notification-dot" style={{ position: 'relative', top: 0, right: 0 }}></span>
              Trực tiếp
            </span>
          </div>
          <div 
            className="flex items-center justify-center bg-black rounded-md overflow-hidden relative" 
            style={{ height: '400px', border: '1px solid var(--border-color)' }}
          >
            {/* Placeholder for Camera Feed */}
            <div className="flex-col items-center gap-4" style={{ color: 'var(--text-muted)' }}>
              <Camera size={48} />
              <p>Đang chờ kết nối từ ESP32-CAM...</p>
            </div>
            
            {/* Face Detection Overlay Simulation */}
            <div 
              style={{
                position: 'absolute',
                top: '20%',
                left: '30%',
                width: '150px',
                height: '150px',
                border: '2px dashed var(--accent-primary)',
                borderRadius: '8px',
                display: 'none' // Set to block when simulating detection
              }}
            >
              <span style={{ position: 'absolute', top: '-25px', left: 0, background: 'var(--accent-primary)', color: 'white', padding: '2px 8px', borderRadius: '4px', fontSize: '12px' }}>
                Đang nhận diện...
              </span>
            </div>
          </div>
        </div>

        {/* Device Status */}
        <div className="flex-col gap-6">
          <div className="card">
            <h3 className="mb-4">Trạng thái thiết bị</h3>
            <div className="flex-col gap-4">
              <div className="flex items-center justify-between p-3 rounded-md" style={{ background: 'var(--bg-base)', border: '1px solid var(--border-color)' }}>
                <div className="flex items-center gap-3">
                  <Wifi size={20} className="text-muted" />
                  <div>
                    <p style={{ fontWeight: 500 }}>ESP32 Gateway</p>
                    <p className="text-sm text-muted">IP: 192.168.1.100</p>
                  </div>
                </div>
                <CheckCircle size={20} style={{ color: 'var(--status-success)' }} />
              </div>

              <div className="flex items-center justify-between p-3 rounded-md" style={{ background: 'var(--bg-base)', border: '1px solid var(--border-color)' }}>
                <div className="flex items-center gap-3">
                  <Camera size={20} className="text-muted" />
                  <div>
                    <p style={{ fontWeight: 500 }}>ESP32-CAM</p>
                    <p className="text-sm text-muted">IP: 192.168.1.101</p>
                  </div>
                </div>
                <CheckCircle size={20} style={{ color: 'var(--status-success)' }} />
              </div>

              <div className="flex items-center justify-between p-3 rounded-md" style={{ background: 'rgba(239, 68, 68, 0.05)', border: '1px solid rgba(239, 68, 68, 0.2)' }}>
                <div className="flex items-center gap-3">
                  <AlertCircle size={20} className="text-muted" />
                  <div>
                    <p style={{ fontWeight: 500 }}>RFID Reader RC522</p>
                    <p className="text-sm text-muted">Mất kết nối 2 phút trước</p>
                  </div>
                </div>
                <AlertCircle size={20} style={{ color: 'var(--status-error)' }} />
              </div>
            </div>
          </div>

          <div className="card">
            <h3 className="mb-4">Log thời gian thực</h3>
            <div className="flex-col gap-2 p-3 rounded-md" style={{ background: 'var(--bg-base)', border: '1px solid var(--border-color)', height: '200px', overflowY: 'auto', fontFamily: 'monospace', fontSize: '12px' }}>
              <p><span style={{ color: 'var(--text-muted)' }}>[08:24:10]</span> <span style={{ color: 'var(--status-info)' }}>INFO:</span> Web client connected via Supabase Realtime.</p>
              <p><span style={{ color: 'var(--text-muted)' }}>[08:25:01]</span> <span style={{ color: 'var(--status-success)' }}>SUCCESS:</span> Face matched - User: 24127345 (98.5% confidence)</p>
              <p><span style={{ color: 'var(--text-muted)' }}>[08:25:02]</span> <span style={{ color: 'var(--status-success)' }}>SUCCESS:</span> RFID scanned - UID: A1:B2:C3:D4</p>
              <p><span style={{ color: 'var(--text-muted)' }}>[08:25:02]</span> <span style={{ color: 'var(--status-success)' }}>SUCCESS:</span> Attendance recorded for 24127345.</p>
              <p><span style={{ color: 'var(--text-muted)' }}>[08:27:15]</span> <span style={{ color: 'var(--status-error)' }}>ERROR:</span> RFID Reader timeout.</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
