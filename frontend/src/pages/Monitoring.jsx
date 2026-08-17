import React, {
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react';
import { Camera, Wifi, CheckCircle, AlertCircle } from 'lucide-react';
import { useSSE } from '../hooks/useSSE';
import { useToast } from '../components/useToast.js';

const GATEWAY_DEVICE_ID = 'door-01';
const CAMERA_ID = 'cam-01';

export default function Monitoring() {
  const { events, connected } = useSSE('/monitor/events');

  const toast = useToast();
  const notifiedEventRef = useRef(null);
  const [cameraStatus, setCameraStatus] = useState({
    online: false,
    captureActive: false,
    hasPreview: false,
  });

  const [previewUrl, setPreviewUrl] = useState('');
  const [previewLoaded, setPreviewLoaded] = useState(false);

  const aiBaseUrl =
    import.meta.env.VITE_AI_BASE_URL ||
    'http://localhost:8000';

  useEffect(() => {
    const loadStatus = async () => {
      try {
        const response = await fetch(
          `${aiBaseUrl}/internal/v1/cameras/${CAMERA_ID}/status`
        );

        if (!response.ok) {
          throw new Error('Cannot load camera status');
        }

        setCameraStatus(await response.json());
      } catch {
        setCameraStatus({
          online: false,
          captureActive: false,
          hasPreview: false,
        });
      }
    };

    loadStatus();
    const timer = setInterval(loadStatus, 500);

    return () => clearInterval(timer);
  }, [aiBaseUrl]);

  useEffect(() => {
    if (!cameraStatus.captureActive) {
      setPreviewLoaded(false);
      setPreviewUrl('');
      return;
    }

    const refreshPreview = () => {
      setPreviewUrl(
        `${aiBaseUrl}/internal/v1/cameras/${CAMERA_ID}/preview.jpg?t=${Date.now()}`
      );
    };

    refreshPreview();

    const timer = setInterval(refreshPreview, 400);

    return () => clearInterval(timer);
  }, [cameraStatus.captureActive, aiBaseUrl]);

  useEffect(() => {
    const latest = events[0];

    if (!latest || latest.id === notifiedEventRef.current) {
      return;
    }

    notifiedEventRef.current = latest.id;

    if (latest.type === 'verification_update') {
      if (latest.data.result === 'VERIFIED') {
        toast(
          `Điểm danh thành công: ${latest.data.fullName}`,
          'success'
        );
      } else {
        toast(
          `Xác thực thất bại: ${
            latest.data.message || latest.data.result
          }`,
          'error'
        );
      }
    }

    if (latest.type === 'attendance_event') {
      const status = latest.data.status === 'LATE'
        ? `Đi muộn ${latest.data.lateMinutes || 0} phút`
        : 'Đúng giờ';

      toast(
        `Điểm danh thành công: ${latest.data.studentName} - ${status}`,
        'success'
      );
    }
  }, [events, toast]);

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
        const verified = e.data.result === 'VERIFIED';

        color = verified
          ? 'var(--status-success)'
          : 'var(--status-error)';

        label = e.data.result;

        message = verified
          ? `Điểm danh thành công - độ tương đồng ${(
              e.data.similarityPercent || 0
            ).toFixed(1)}%`
          : `Xác thực thất bại: ${
              e.data.message || e.data.result
            }`;
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
      [GATEWAY_DEVICE_ID]: { status: 'UNKNOWN', lastHeartbeat: null },
      [CAMERA_ID]: { status: 'UNKNOWN', lastHeartbeat: null }
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
        {/* Camera Live Preview */}
        <div className="card col-span-2 flex-col">
          <div className="camera-title-row">
            <h3 className="flex items-center gap-2">
              <Camera size={20} />
              Camera trực tiếp
            </h3>

            <span
              className={`camera-badge ${
                cameraStatus.online ? 'camera-online' : 'camera-offline'
              }`}
            >
              {cameraStatus.online ? 'ONLINE' : 'OFFLINE'}
            </span>
          </div>

          <div className="camera-preview">
            {cameraStatus.captureActive && previewUrl && (
              <img
                src={previewUrl}
                alt="ESP32-CAM trực tiếp"
                onLoad={() => setPreviewLoaded(true)}
                onError={() => setPreviewLoaded(false)}
              />
            )}

            {!cameraStatus.online && (
              <div className="camera-placeholder">
                ESP32-CAM đang offline
              </div>
            )}

            {cameraStatus.online &&
              !cameraStatus.captureActive && (
                <div className="camera-placeholder">
                  Camera online - đang chờ RFID
                </div>
              )}

            {cameraStatus.online &&
              cameraStatus.captureActive &&
              !previewLoaded && (
                <div className="camera-placeholder">
                  Đang chờ frame từ ESP32-CAM...
                </div>
              )}
          </div>
        </div>

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
                    <p className="text-sm text-muted">Trạng thái: {deviceStatuses[GATEWAY_DEVICE_ID].status}</p>
                  </div>
                </div>
                {deviceStatuses[GATEWAY_DEVICE_ID].status === 'ONLINE' ?
                  <CheckCircle size={20} style={{ color: 'var(--status-success)' }} /> :
                  <AlertCircle size={20} style={{ color: 'var(--status-warning)' }} />
                }
              </div>

              <div className="flex items-center justify-between p-3 rounded-md" style={{ background: 'var(--bg-base)', border: '1px solid var(--border-color)' }}>
                <div className="flex items-center gap-3">
                  <Camera size={20} className="text-muted" />
                  <div>
                    <p style={{ fontWeight: 500 }}>ESP32-CAM</p>
                    <p className="text-sm text-muted">Trạng thái: {cameraStatus.online ? 'ONLINE' : 'OFFLINE'}</p>
                    <p className="text-sm text-muted">
                      Hoạt động:{' '}
                      {cameraStatus.captureActive
                        ? 'ĐANG XÁC THỰC'
                        : 'ĐANG CHỜ RFID'}
                    </p>
                  </div>
                </div>
                {cameraStatus.online ?
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