import { useEffect, useState } from 'react';
import { AlertCircle, CheckCircle2, Clock3, Download, RefreshCw, Users, XCircle } from 'lucide-react';
import { Area, AreaChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { apiClient } from '../api/client';
import { Button, ErrorBanner, PageHeader, Panel } from '../components/ui';
import { SkeletonCard, SkeletonChart } from '../components/Skeleton';

const dateFormatter = new Intl.DateTimeFormat('vi-VN', { day: '2-digit', month: '2-digit' });

function formatTrend(data = []) {
  return data.map((item) => ({ ...item, label: dateFormatter.format(new Date(`${item.date}T00:00:00`)) }));
}

export default function Reports() {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [downloading, setDownloading] = useState(false);
  const [error, setError] = useState('');

  const loadData = async (silent = false) => {
    if (silent) setRefreshing(true);
    else setLoading(true);
    try {
      setStats(await apiClient.getAttendanceStats());
      setError('');
    } catch {
      setError('Không thể tải báo cáo. Kiểm tra kết nối backend.');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => { loadData(); }, []);

  const handleDownload = async () => {
    setDownloading(true);
    try {
      const blob = await apiClient.downloadReport();
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `attendance_report_${new Date().toISOString().slice(0, 10)}.xlsx`;
      link.click();
      URL.revokeObjectURL(url);
    } catch {
      setError('Không thể tải file Excel. Vui lòng thử lại.');
    } finally {
      setDownloading(false);
    }
  };

  const present = stats?.presentToday ?? 0;
  const total = stats?.totalStudents ?? 0;
  const attendanceRate = total ? Math.round((present / total) * 100) : 0;
  const absent = Math.max(total - present, 0);
  const trend = formatTrend(stats?.trend);
  const cards = [
    { label: 'Tổng sinh viên', value: total, icon: Users, tone: 'blue' },
    { label: 'Có mặt hôm nay', value: present, icon: CheckCircle2, tone: 'green' },
    { label: 'Đi muộn', value: stats?.lateToday ?? 0, icon: Clock3, tone: 'amber' },
    { label: 'Chưa ghi nhận', value: absent, icon: XCircle, tone: 'red' },
  ];

  return (
    <div className="page-stack reports-page">
      <PageHeader
        title="Báo cáo & thống kê"
        description="Theo dõi tình hình điểm danh theo ngày và xu hướng 7 ngày gần nhất"
        actions={(
          <>
            <Button onClick={() => loadData(true)} disabled={loading || refreshing}>
              <RefreshCw size={16} className={refreshing ? 'spin' : ''} /> Làm mới
            </Button>
            <Button variant="primary" onClick={handleDownload} disabled={downloading || loading}>
              <Download size={16} /> {downloading ? 'Đang tải...' : 'Xuất Excel'}
            </Button>
          </>
        )}
      />
      <ErrorBanner message={error} onRetry={loadData} />

      {loading ? (
        <><div className="stats-grid"><SkeletonCard /><SkeletonCard /><SkeletonCard /><SkeletonCard /></div><Panel><SkeletonChart /></Panel></>
      ) : stats && (
        <>
          <div className="stats-grid">
            {cards.map(({ label, value, icon: Icon, tone }) => (
              <Panel className="stat-panel" key={label}>
                <span className={`stat-icon tone-${tone}`}><Icon size={19} /></span>
                <div><p>{label}</p><strong>{value}</strong></div>
              </Panel>
            ))}
          </div>

          <div className="reports-grid">
            <Panel className="reports-chart-panel">
              <div className="panel-heading">
                <div><h2>Xu hướng điểm danh</h2><p>Số lượt đúng giờ và đi muộn trong 7 ngày</p></div>
                <span className="report-period">7 ngày</span>
              </div>
              <div className="report-chart-frame">
                {trend.length ? (
                  <ResponsiveContainer width="100%" height="100%">
                    <AreaChart data={trend} margin={{ top: 8, right: 8, left: -20, bottom: 0 }}>
                      <defs>
                        <linearGradient id="reportOnTime" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stopColor="#14745f" stopOpacity=".22" /><stop offset="100%" stopColor="#14745f" stopOpacity="0" /></linearGradient>
                        <linearGradient id="reportLate" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stopColor="#a9670e" stopOpacity=".18" /><stop offset="100%" stopColor="#a9670e" stopOpacity="0" /></linearGradient>
                      </defs>
                      <CartesianGrid stroke="var(--border-color)" strokeDasharray="3 3" vertical={false} />
                      <XAxis dataKey="label" tickLine={false} axisLine={false} />
                      <YAxis allowDecimals={false} tickLine={false} axisLine={false} />
                      <Tooltip contentStyle={{ border: '1px solid var(--border-color)', borderRadius: 6, background: '#fff' }} />
                      <Legend verticalAlign="top" height={34} />
                      <Area type="monotone" dataKey="onTime" name="Đúng giờ" stroke="#14745f" fill="url(#reportOnTime)" strokeWidth={2} />
                      <Area type="monotone" dataKey="late" name="Đi muộn" stroke="#a9670e" fill="url(#reportLate)" strokeWidth={2} />
                    </AreaChart>
                  </ResponsiveContainer>
                ) : <div className="empty-state">Chưa có dữ liệu xu hướng.</div>}
              </div>
            </Panel>

            <Panel className="attendance-rate-panel">
              <div className="panel-heading"><div><h2>Tỷ lệ hiện diện</h2><p>Tổng quan trong ngày</p></div></div>
              <div className="rate-value"><strong>{attendanceRate}%</strong><span>có mặt</span></div>
              <div className="rate-track"><span style={{ width: `${attendanceRate}%` }} /></div>
              <div className="rate-breakdown">
                <div><span><i className="dot dot-green" />Có mặt</span><strong>{present}</strong></div>
                <div><span><i className="dot dot-red" />Chưa ghi nhận</span><strong>{absent}</strong></div>
              </div>
              <div className="rate-note"><AlertCircle size={15} /> Dữ liệu tính theo ngày hiện tại</div>
            </Panel>
          </div>
        </>
      )}
    </div>
  );
}
