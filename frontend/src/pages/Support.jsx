import React, { useEffect, useState } from 'react';
import { Send, AlertTriangle, CheckCircle, Clock } from 'lucide-react';
import { apiClient } from '../api/client';
import { ErrorBanner } from '../components/ui';
import { useToast } from '../components/useToast';

const statusBadge = {
  OPEN: 'badge-error',
  ACKNOWLEDGED: 'badge-warning',
  RESOLVED: 'badge-success'
};

const statusLabels = {
  OPEN: 'Chờ xử lý',
  ACKNOWLEDGED: 'Đã tiếp nhận',
  RESOLVED: 'Đã giải quyết'
};

export default function Support() {
  const [incidents, setIncidents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const toast = useToast();

  const isLeadProctor = true;

  const loadIncidents = async () => {
    setLoading(true);
    try {
      const data = await apiClient.getAssistanceRequests();
      setIncidents(data);
      setError('');
    } catch {
      setError('Không thể tải lịch sử sự cố.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadIncidents();
  }, []);

  const updateStatus = async (id, newStatus) => {
    if (!isLeadProctor) return;
    try {
      await apiClient.updateAssistanceStatus(id, newStatus);
      toast('Đã cập nhật trạng thái sự cố', 'success');
      loadIncidents();
    } catch {
      toast('Lỗi khi cập nhật trạng thái', 'error');
    }
  };

  return (
    <div className="flex-col gap-6">
      <div className="flex items-center justify-between mb-2">
        <div>
          <h2>Hỗ trợ / Báo lỗi</h2>
          <p className="text-muted">Quản lý các yêu cầu hỗ trợ và sự cố thiết bị</p>
        </div>
      </div>

      <ErrorBanner message={error} onRetry={loadIncidents} />

      <div className="grid-content">
        <div className="card col-span-3">
          <h3 className="mb-4">Danh sách sự cố / Yêu cầu hỗ trợ</h3>
          {loading ? (
            <p className="text-muted">Đang tải dữ liệu...</p>
          ) : (
            <div className="flex-col gap-4 text-sm">
              {incidents.length === 0 ? (
                <p className="text-muted">Không có sự cố nào.</p>
              ) : incidents.map(incident => (
                <div key={incident.id} className="p-4 rounded-md" style={{ border: '1px solid var(--border-color)', background: 'var(--bg-base)' }}>
                  <div className="flex justify-between mb-3 items-center">
                    <span className={`badge ${statusBadge[incident.status] || 'badge-neutral'}`}>
                      {statusLabels[incident.status] || incident.status}
                    </span>
                    <span className="text-muted">{new Date(incident.createdAt).toLocaleString('vi-VN')}</span>
                  </div>
                  <p style={{ fontWeight: 500, fontSize: '15px' }}>{incident.message}</p>
                  <p className="text-muted mt-2">Nguồn: {incident.source} {incident.userId ? `- Học sinh ID: ${incident.userId}` : ''}</p>
                  
                  {isLeadProctor && incident.status !== 'RESOLVED' && (
                    <div className="flex gap-2 mt-4 pt-4" style={{ borderTop: '1px solid var(--border-color)' }}>
                      {incident.status === 'OPEN' && (
                        <button className="btn btn-sm" onClick={() => updateStatus(incident.id, 'ACKNOWLEDGED')}>
                          <Clock size={14} /> Tiếp nhận
                        </button>
                      )}
                      <button className="btn btn-sm btn-primary" onClick={() => updateStatus(incident.id, 'RESOLVED')}>
                        <CheckCircle size={14} /> Đánh dấu đã giải quyết
                      </button>
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
