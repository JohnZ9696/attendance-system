import { useEffect, useState } from 'react';
import { Activity, Clock3, RefreshCw, UserCheck, Users } from 'lucide-react';
import { Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { apiClient } from '../api/client';
import { Button, ErrorBanner, PageHeader, Panel } from '../components/ui';
import { SkeletonCard, SkeletonChart } from '../components/Skeleton';

const timeFormatter = new Intl.DateTimeFormat('vi-VN', { hour: '2-digit', minute: '2-digit' });
const dateFormatter = new Intl.DateTimeFormat('vi-VN', { dateStyle: 'full' });

function relativeTime(value) {
  const minutes = Math.floor((Date.now() - value.getTime()) / 60000);
  if (minutes < 1) return 'vừa xong';
  if (minutes < 60) return `${minutes} phút trước`;
  return timeFormatter.format(value);
}

function buildTimeline(records) {
  const hours = new Map();
  records.forEach((record) => {
    const date = new Date(record.checkInTime);
    const hour = `${String(date.getHours()).padStart(2, '0')}:00`;
    const current = hours.get(hour) || { time: hour, onTime: 0, late: 0 };
    if (record.status === 'LATE') current.late += 1;
    else current.onTime += 1;
    hours.set(hour, current);
  });
  return [...hours.values()].sort((a, b) => a.time.localeCompare(b.time));
}

export default function Dashboard() {
  const [data, setData] = useState({ users: [], records: [] });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadData = async (silent = false) => {
    if (!silent) setLoading(true);
    try {
      const [users, records] = await Promise.all([apiClient.getUsers(), apiClient.getTodayAttendance()]);
      setData({ users, records });
      setError('');
    } catch {
      if (!silent) setError('Không thể tải dữ liệu tổng quan. Kiểm tra kết nối backend.');
    } finally {
      if (!silent) setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
    const intervalId = window.setInterval(() => loadData(true), 3000);
    return () => window.clearInterval(intervalId);
  }, []);

  const attendedIds = new Set(data.records.map((record) => record.user?.id));
  const lateCount = data.records.filter((record) => record.status === 'LATE').length;
  const recent = [...data.records]
    .sort((a, b) => new Date(b.checkInTime) - new Date(a.checkInTime))
    .slice(0, 6);
  const timeline = buildTimeline(data.records);
  const stats = [
    { label: 'Người dùng', value: data.users.length, icon: Users, tone: 'blue' },
    { label: 'Đã điểm danh', value: attendedIds.size, icon: UserCheck, tone: 'green' },
    { label: 'Đi muộn', value: lateCount, icon: Clock3, tone: 'amber' },
    { label: 'Chưa ghi nhận', value: Math.max(data.users.length - attendedIds.size, 0), icon: Activity, tone: 'red' },
  ];

  return (
    <div className="page-stack">
      <PageHeader
        title="Tổng quan hôm nay"
        description={dateFormatter.format(new Date())}
        actions={<Button onClick={() => loadData()} disabled={loading}><RefreshCw size={16} /> Làm mới</Button>}
      />
      <ErrorBanner message={error} onRetry={loadData} />

      <div className="stats-grid">
        {loading ? (
          <>
            <SkeletonCard />
            <SkeletonCard />
            <SkeletonCard />
            <SkeletonCard />
          </>
        ) : stats.map(({ label, value, icon: Icon, tone }) => (
          <Panel className="stat-panel" key={label}>
            <span className={`stat-icon tone-${tone}`}><Icon size={20} /></span>
            <div><p>{label}</p><strong>{value}</strong></div>
          </Panel>
        ))}
      </div>

      <div className="dashboard-grid">
        <Panel className="chart-panel">
          <div className="panel-heading"><div><h2>Nhịp điểm danh</h2><p>Số lượt ghi nhận theo giờ</p></div></div>
          <div className="chart-frame">
            {loading ? <SkeletonChart /> : timeline.length ? (
              <ResponsiveContainer width="100%" height="100%">
                <AreaChart data={timeline} margin={{ top: 8, right: 8, left: -20, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="var(--border-color)" />
                  <XAxis dataKey="time" tickLine={false} axisLine={false} />
                  <YAxis allowDecimals={false} tickLine={false} axisLine={false} />
                  <Tooltip />
                  <Area type="monotone" dataKey="onTime" name="Đúng giờ" stroke="#18866b" fill="#d8eee8" strokeWidth={2} />
                  <Area type="monotone" dataKey="late" name="Đi muộn" stroke="#c27a14" fill="#fae8c6" strokeWidth={2} />
                </AreaChart>
              </ResponsiveContainer>
            ) : <div className="empty-state">Chưa có lượt điểm danh hôm nay.</div>}
          </div>
        </Panel>

        <Panel>
          <div className="panel-heading"><div><h2>Gần đây</h2><p>{recent.length} lượt mới nhất</p></div></div>
          <div className="activity-list">
            {recent.map((record) => (
              <div className="activity-row" key={record.id}>
                <span className="initials">{record.user?.name?.slice(0, 1) || '?'}</span>
                <div className="activity-person"><strong>{record.user?.name || 'Không xác định'}</strong><span>{record.user?.mssv || 'Chưa có MSSV'}</span></div>
                <div className="activity-meta"><span className={`badge ${record.status === 'LATE' ? 'badge-warning' : 'badge-success'}`}>{record.status === 'LATE' ? 'Đi muộn' : 'Đúng giờ'}</span><time>{relativeTime(new Date(record.checkInTime))}</time></div>
              </div>
            ))}
            {!recent.length && <div className="empty-state">Chưa có hoạt động.</div>}
          </div>
        </Panel>
      </div>
    </div>
  );
}
