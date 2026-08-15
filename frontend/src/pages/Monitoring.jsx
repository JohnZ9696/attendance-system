import React, { useMemo } from 'react';
import { Camera, Wifi, CheckCircle, AlertCircle } from 'lucide-react';
import { useSSE } from '../hooks/useSSE';

export default function Monitoring() {
  const { events, connected } = useSSE('/monitor/events');

  const logs = useMemo(() => {
    return events.map((e, idx) => {
      let color = 'var(--status-info)';
      let label = 'INFO';
      let message = '';

      if (e.type === 'attendance_event') {
        color = 'var(--status-success)';
        label = 'SUCCESS';
        message = `Điểm danh: ${e.data.studentName} (${e.data.studentId}) - ${e.data.status === 'LATE' ? `Muộn ${e.data.lateMinutes}p` : 'Đúng giờ'}`;
      } else if (e.type === 'verification_update') {
        color = e.data.result === 'SUCCESS' ? 'var(--status-success)' : 'var(--status-warning)';
        label = e.data.result;
        message = `Xác thực khuôn mặt: ${e.data.result} (${(e.data.similarityPercent || 0).toFixed(1)}%)`;
      } else if (e.type === 'device_status') {
        color = e.data.status === 'ONLINE' ? 'var(--status-success)' : 'var(--status-error)';
        label = e.data.status;
        message = `Thiết bị ${e.data.deviceId} - Trạng thái: ${e.data.status}`;
      } else if (e.type === 'incident') {
        color = 'var(--status-error)';
        label = 'INCIDENT';
        message = `Sự cố: ${e.data.message} (${e.data.source})`;
      } else {
        message = JSON.stringify(e.data);
      }

      return (
        <p key={idx}>
          <span style={{ color: 'var(--text-muted)' }}>[{e.timestamp.toLocaleTimeString('vi-VN')}]</span>{' '}
          <span style={{ color }}>{label}:</span> {message}
        </p>
      );
    });
  }, [events]);

  const deviceStatuses = useMemo(() => {
    const devices = {
      'ESP32_GATEWAY': { status: 'UNKNOWN', lastHeartbeat: null },
      'ESP32_CAM': { status: 'UNKNOWN', lastHeartbeat: null }
    };
    events.filter(e => e.type === 'device_status').forEach(e => {
      if (devices[e.data.deviceId]) {
        devices[e.data.deviceId].status = e.data.status;
        devices[e.data.deviceId].lastHeartbeat = e.data.lastHeartbeat;
      }
    });
    return devices;
  }, [events]);

  return (
    <div className="flex-col gap-6">
      <div className="flex items-center justify-between mb-2">
        <div>
          <h2>Giám sát trực tiếp</h2>
          <p className="text-muted">Xem luồng log trực tiếp và trạng thái thiết bị</p>
        </div>
      </div>

      <div className="grid-content">
        {/* Verification / Log View */}
        <div className="card col-span-2 flex-col">
          <div className="flex items-center justify-between mb-4">
            <h3 className="flex items-center gap-2">
              <Camera size={20} />
              Luồng sự kiện (SSE)
            </h3>
            <span className={`badge ${connected ? 'badge-success' : 'badge-warning'} flex items-center gap-2`}>
              {connected && <span className="notification-dot" style={{ position: 'relative', top: 0, right: 0 }}></span>}
              {connected ? 'Trực tiếp' : 'Đang kết nối...'}
            </span>
          </div>
          
          <div className="flex-col gap-2 p-3 rounded-md" style={{ background: 'var(--bg-base)', border: '1px solid var(--border-color)', height: '400px', overflowY: 'auto', fontFamily: 'monospace', fontSize: '13px', lineHeight: '1.5' }}>
            {logs.length > 0 ? logs : <p style={{ color: 'var(--text-muted)' }}>Đang chờ sự kiện...</p>}
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
                    <p className="text-sm text-muted">Trạng thái: {deviceStatuses['ESP32_GATEWAY'].status}</p>
                  </div>
                </div>
                {deviceStatuses['ESP32_GATEWAY'].status === 'ONLINE' ? 
                  <CheckCircle size={20} style={{ color: 'var(--status-success)' }} /> :
                  <AlertCircle size={20} style={{ color: 'var(--status-warning)' }} />
                }
              </div>

              <div className="flex items-center justify-between p-3 rounded-md" style={{ background: 'var(--bg-base)', border: '1px solid var(--border-color)' }}>
                <div className="flex items-center gap-3">
                  <Camera size={20} className="text-muted" />
                  <div>
                    <p style={{ fontWeight: 500 }}>ESP32-CAM</p>
                    <p className="text-sm text-muted">Trạng thái: {deviceStatuses['ESP32_CAM'].status}</p>
                  </div>
                </div>
                {deviceStatuses['ESP32_CAM'].status === 'ONLINE' ? 
                  <CheckCircle size={20} style={{ color: 'var(--status-success)' }} /> :
                  <AlertCircle size={20} style={{ color: 'var(--status-warning)' }} />
                }
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
