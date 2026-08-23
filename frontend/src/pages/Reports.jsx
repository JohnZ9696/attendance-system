import React, { useState, useEffect } from 'react';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer, PieChart, Pie, Cell, AreaChart, Area } from 'recharts';
import { apiClient } from '../api/client';
import { ErrorBanner } from '../components/ui';

const COLORS = ['#10b981', '#f59e0b', '#ef4444'];

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

  const handleDownload = async () => {
    try {
      const blob = await apiClient.downloadReport();
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `attendance_report_${new Date().toISOString().split('T')[0]}.xlsx`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      alert("Failed to download report");
    }
  };

  if (loading) return <div className="p-8 text-center text-[var(--text-muted)]">Đang tải dữ liệu...</div>;

  const pieData = stats ? [
    { name: 'Đúng giờ', value: stats.onTimeToday },
    { name: 'Đi muộn', value: stats.lateToday },
    { name: 'Vắng mặt', value: Math.max(0, stats.totalStudents - stats.presentToday) }
  ] : [];

  const trendData = stats?.trend || [];

  return (
    <div className="flex-col gap-8 pb-10">
      <div className="flex items-center justify-between mb-4">
        <div>
          <h2 className="text-3xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-indigo-500 to-purple-600">Metric Dashboard</h2>
          <p className="text-[var(--text-muted)] mt-1">Tổng quan dữ liệu điểm danh và phân tích xu hướng</p>
        </div>
        <button 
          onClick={handleDownload}
          className="px-6 py-2 bg-gradient-to-r from-blue-600 to-indigo-600 text-white rounded-lg shadow-lg hover:shadow-xl transition-all font-medium flex items-center gap-2"
        >
          <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path><polyline points="7 10 12 15 17 10"></polyline><line x1="12" y1="15" x2="12" y2="3"></line></svg>
          Tải Excel Report
        </button>
      </div>

      <ErrorBanner message={error} onRetry={loadData} />

      {stats && (
        <>
          {/* Top KPI Cards with Glassmorphism */}
          <div className="grid grid-cols-4 gap-6">
            <div className="p-6 rounded-2xl border border-white/10 shadow-[0_8px_32px_0_rgba(31,38,135,0.07)] backdrop-blur-md bg-white/5 relative overflow-hidden">
              <div className="absolute top-0 right-0 p-4 opacity-10">
                 <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path><circle cx="9" cy="7" r="4"></circle><path d="M23 21v-2a4 4 0 0 0-3-3.87"></path><path d="M16 3.13a4 4 0 0 1 0 7.75"></path></svg>
              </div>
              <p className="text-[var(--text-muted)] font-medium mb-1 relative z-10">Tổng sinh viên</p>
              <h3 className="text-4xl font-bold text-[var(--text-primary)] relative z-10">{stats.totalStudents}</h3>
            </div>
            
            <div className="p-6 rounded-2xl border border-white/10 shadow-[0_8px_32px_0_rgba(31,38,135,0.07)] backdrop-blur-md bg-white/5 relative overflow-hidden">
              <div className="absolute top-0 right-0 p-4 opacity-10 text-emerald-500">
                 <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path><polyline points="22 4 12 14.01 9 11.01"></polyline></svg>
              </div>
              <p className="text-[var(--text-muted)] font-medium mb-1 relative z-10">Có mặt hôm nay</p>
              <h3 className="text-4xl font-bold text-emerald-500 relative z-10">{stats.presentToday}</h3>
            </div>
            
            <div className="p-6 rounded-2xl border border-white/10 shadow-[0_8px_32px_0_rgba(31,38,135,0.07)] backdrop-blur-md bg-white/5 relative overflow-hidden">
              <div className="absolute top-0 right-0 p-4 opacity-10 text-amber-500">
                 <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><circle cx="12" cy="12" r="10"></circle><polyline points="12 6 12 12 16 14"></polyline></svg>
              </div>
              <p className="text-[var(--text-muted)] font-medium mb-1 relative z-10">Đi muộn</p>
              <h3 className="text-4xl font-bold text-amber-500 relative z-10">{stats.lateToday}</h3>
            </div>

            <div className="p-6 rounded-2xl border border-white/10 shadow-[0_8px_32px_0_rgba(31,38,135,0.07)] backdrop-blur-md bg-white/5 relative overflow-hidden">
              <div className="absolute top-0 right-0 p-4 opacity-10 text-rose-500">
                 <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path><line x1="12" y1="9" x2="12" y2="13"></line><line x1="12" y1="17" x2="12.01" y2="17"></line></svg>
              </div>
              <p className="text-[var(--text-muted)] font-medium mb-1 relative z-10">Vắng mặt</p>
              <h3 className="text-4xl font-bold text-rose-500 relative z-10">{Math.max(0, stats.totalStudents - stats.presentToday)}</h3>
            </div>
          </div>

          <div className="grid grid-cols-3 gap-6">
            {/* Trend Chart */}
            <div className="col-span-2 p-6 rounded-2xl border border-[var(--border-color)] bg-[var(--bg-surface)] shadow-sm">
              <h3 className="text-lg font-bold mb-6 flex items-center gap-2">
                 <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"></polyline></svg>
                 Xu hướng 7 ngày
              </h3>
              <div style={{ height: '320px' }}>
                <ResponsiveContainer width="100%" height="100%">
                  <AreaChart data={trendData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                    <defs>
                      <linearGradient id="colorOnTime" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="5%" stopColor="#10b981" stopOpacity={0.3}/>
                        <stop offset="95%" stopColor="#10b981" stopOpacity={0}/>
                      </linearGradient>
                      <linearGradient id="colorLate" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="5%" stopColor="#f59e0b" stopOpacity={0.3}/>
                        <stop offset="95%" stopColor="#f59e0b" stopOpacity={0}/>
                      </linearGradient>
                    </defs>
                    <CartesianGrid strokeDasharray="3 3" stroke="var(--border-color)" vertical={false} />
                    <XAxis dataKey="date" stroke="var(--text-muted)" fontSize={12} tickMargin={10} />
                    <YAxis stroke="var(--text-muted)" fontSize={12} />
                    <Tooltip 
                      contentStyle={{ backgroundColor: 'var(--bg-base)', borderColor: 'var(--border-color)', borderRadius: '12px', boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)' }}
                    />
                    <Legend verticalAlign="top" height={36}/>
                    <Area type="monotone" dataKey="onTime" name="Đúng giờ" stroke="#10b981" strokeWidth={3} fillOpacity={1} fill="url(#colorOnTime)" />
                    <Area type="monotone" dataKey="late" name="Đi muộn" stroke="#f59e0b" strokeWidth={3} fillOpacity={1} fill="url(#colorLate)" />
                  </AreaChart>
                </ResponsiveContainer>
              </div>
            </div>

            {/* Pie Chart */}
            <div className="p-6 rounded-2xl border border-[var(--border-color)] bg-[var(--bg-surface)] shadow-sm flex flex-col">
              <h3 className="text-lg font-bold mb-2 flex items-center gap-2">
                 <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M21.21 15.89A10 10 0 1 1 8 2.83"></path><path d="M22 12A10 10 0 0 0 12 2v10z"></path></svg>
                 Tỷ lệ hôm nay
              </h3>
              <div style={{ flex: 1, minHeight: '280px' }} className="flex items-center justify-center relative">
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Pie
                      data={pieData}
                      cx="50%"
                      cy="50%"
                      innerRadius={70}
                      outerRadius={95}
                      paddingAngle={6}
                      dataKey="value"
                      stroke="none"
                      cornerRadius={6}
                    >
                      {pieData.map((entry, index) => (
                        <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                      ))}
                    </Pie>
                    <Tooltip 
                      contentStyle={{ backgroundColor: 'var(--bg-base)', borderColor: 'var(--border-color)', borderRadius: '12px', boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)' }}
                      itemStyle={{ color: 'var(--text-primary)', fontWeight: 500 }}
                    />
                    <Legend verticalAlign="bottom" height={36} iconType="circle"/>
                  </PieChart>
                </ResponsiveContainer>
                {/* Center text for donut chart */}
                <div className="absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 text-center pointer-events-none mt-[-10px]">
                   <p className="text-sm text-[var(--text-muted)]">Hiện diện</p>
                   <p className="text-2xl font-bold">{stats.totalStudents > 0 ? Math.round((stats.presentToday / stats.totalStudents) * 100) : 0}%</p>
                </div>
              </div>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
