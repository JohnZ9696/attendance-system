import React, { useState, useEffect } from 'react';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer, PieChart, Pie, Cell } from 'recharts';
import { apiClient } from '../api/client';
import { ErrorBanner } from '../components/ui';

const COLORS = ['var(--status-success)', 'var(--status-warning)', 'var(--status-error)'];

export default function Reports() {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadData = async () => {
    setLoading(true);
    try {
      const data = await apiClient.getAttendanceStats();
      setStats(data);
    } catch {
      setError('Không thể tải báo cáo. Vui lòng thử lại.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const pieData = stats ? [
    { name: 'Đúng giờ', value: stats.today.onTime },
    { name: 'Đi muộn', value: stats.today.late },
    { name: 'Vắng mặt', value: stats.today.absent }
  ] : [];

  const weeklyData = stats?.weekly || [];

  return (
    <div className="flex-col gap-6">
      <div className="flex items-center justify-between mb-2">
        <div>
          <h2>Báo cáo và thống kê</h2>
          <p className="text-muted">Thống kê dữ liệu điểm danh và tổng hợp</p>
        </div>
      </div>

      <ErrorBanner message={error} onRetry={loadData} />

      {!loading && stats && (
        <div className="grid-content">
          <div className="card col-span-2">
            <h3 className="mb-6">Thống kê điểm danh trong tuần</h3>
            <div style={{ height: '350px' }}>
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={weeklyData} margin={{ top: 20, right: 30, left: 0, bottom: 5 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="var(--border-color)" vertical={false} />
                  <XAxis dataKey="date" stroke="var(--text-muted)" />
                  <YAxis stroke="var(--text-muted)" />
                  <Tooltip 
                    contentStyle={{ backgroundColor: 'var(--bg-surface)', borderColor: 'var(--border-color)', borderRadius: '8px' }}
                  />
                  <Legend />
                  <Bar dataKey="onTime" name="Đúng giờ" stackId="a" fill="var(--status-success)" radius={[0, 0, 4, 4]} />
                  <Bar dataKey="late" name="Đi muộn" stackId="a" fill="var(--status-warning)" />
                  <Bar dataKey="absent" name="Vắng mặt" stackId="a" fill="var(--status-error)" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>

          <div className="card flex-col">
            <h3 className="mb-6">Tỷ lệ chuyên cần (Hôm nay)</h3>
            <div style={{ flex: 1, minHeight: '250px' }}>
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={pieData}
                    cx="50%"
                    cy="50%"
                    innerRadius={60}
                    outerRadius={100}
                    paddingAngle={5}
                    dataKey="value"
                    stroke="none"
                  >
                    {pieData.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                    ))}
                  </Pie>
                  <Tooltip 
                    contentStyle={{ backgroundColor: 'var(--bg-surface)', borderColor: 'var(--border-color)', borderRadius: '8px' }}
                    itemStyle={{ color: 'var(--text-primary)' }}
                  />
                  <Legend verticalAlign="bottom" height={36}/>
                </PieChart>
              </ResponsiveContainer>
            </div>
          </div>
          
          <div className="card col-span-3">
             <h3 className="mb-4">Phân tích chuyên sâu</h3>
             <div className="stats-grid" style={{ gridTemplateColumns: 'repeat(3, 1fr)' }}>
                <div className="p-4 rounded-md" style={{ background: 'var(--bg-base)', border: '1px solid var(--border-color)' }}>
                   <p className="text-muted mb-1">Tổng số phút muộn tuần này</p>
                   <p style={{ fontSize: '24px', fontWeight: 600 }}>{stats.weeklyLateMinutes || 0} phút</p>
                </div>
                <div className="p-4 rounded-md" style={{ background: 'var(--bg-base)', border: '1px solid var(--border-color)' }}>
                   <p className="text-muted mb-1">Trung bình phút muộn / lượt</p>
                   <p style={{ fontSize: '24px', fontWeight: 600 }}>{stats.avgLateMinutes || 0} phút</p>
                </div>
                <div className="p-4 rounded-md" style={{ background: 'var(--bg-base)', border: '1px solid var(--border-color)' }}>
                   <p className="text-muted mb-1">Cảnh báo vi phạm (3 lần muộn)</p>
                   <p style={{ fontSize: '24px', fontWeight: 600, color: 'var(--status-error)' }}>{stats.violationCount || 0} học sinh</p>
                </div>
             </div>
          </div>
        </div>
      )}
    </div>
  );
}
