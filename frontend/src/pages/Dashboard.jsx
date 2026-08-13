import React from 'react';
import { Users, UserCheck, UserX, Activity } from 'lucide-react';
import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';

const data = [
  { time: '07:00', present: 10, late: 0 },
  { time: '07:30', present: 45, late: 5 },
  { time: '08:00', present: 120, late: 15 },
  { time: '08:30', present: 140, late: 25 },
  { time: '09:00', present: 145, late: 28 },
  { time: '09:30', present: 148, late: 30 },
];

export default function Dashboard() {
  return (
    <div className="flex-col gap-6">
      <div className="flex items-center justify-between mb-2">
        <div>
          <h2>Tổng quan hệ thống</h2>
          <p className="text-muted">Dữ liệu điểm danh hôm nay - 12/08/2026</p>
        </div>
      </div>

      {/* Stats Cards */}
      <div className="grid-stats">
        <div className="card stat-card flex items-center gap-4">
          <div className="stat-icon" style={{ background: 'rgba(59, 130, 246, 0.1)', color: 'var(--accent-primary)' }}>
            <Users size={28} />
          </div>
          <div>
            <p className="text-muted text-sm">Tổng số đăng ký</p>
            <h3 className="stat-value">156</h3>
          </div>
        </div>
        
        <div className="card stat-card flex items-center gap-4">
          <div className="stat-icon" style={{ background: 'rgba(16, 185, 129, 0.1)', color: 'var(--status-success)' }}>
            <UserCheck size={28} />
          </div>
          <div>
            <p className="text-muted text-sm">Đã điểm danh</p>
            <h3 className="stat-value">148</h3>
          </div>
        </div>
        
        <div className="card stat-card flex items-center gap-4">
          <div className="stat-icon" style={{ background: 'rgba(245, 158, 11, 0.1)', color: 'var(--status-warning)' }}>
            <UserX size={28} />
          </div>
          <div>
            <p className="text-muted text-sm">Đi muộn</p>
            <h3 className="stat-value">30</h3>
          </div>
        </div>

        <div className="card stat-card flex items-center gap-4">
          <div className="stat-icon" style={{ background: 'rgba(239, 68, 68, 0.1)', color: 'var(--status-error)' }}>
            <Activity size={28} />
          </div>
          <div>
            <p className="text-muted text-sm">Vắng mặt</p>
            <h3 className="stat-value">8</h3>
          </div>
        </div>
      </div>

      {/* Charts & Activity */}
      <div className="grid-content">
        <div className="card col-span-2">
          <div className="flex items-center justify-between mb-6">
            <h3>Biểu đồ điểm danh hôm nay</h3>
            <span className="badge badge-success">Cập nhật trực tiếp</span>
          </div>
          <div style={{ height: '300px' }}>
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={data} margin={{ top: 10, right: 30, left: 0, bottom: 0 }}>
                <defs>
                  <linearGradient id="colorPresent" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="var(--status-success)" stopOpacity={0.3}/>
                    <stop offset="95%" stopColor="var(--status-success)" stopOpacity={0}/>
                  </linearGradient>
                  <linearGradient id="colorLate" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="var(--status-warning)" stopOpacity={0.3}/>
                    <stop offset="95%" stopColor="var(--status-warning)" stopOpacity={0}/>
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="var(--border-color)" vertical={false} />
                <XAxis dataKey="time" stroke="var(--text-muted)" tick={{fill: 'var(--text-muted)'}} />
                <YAxis stroke="var(--text-muted)" tick={{fill: 'var(--text-muted)'}} />
                <Tooltip 
                  contentStyle={{ backgroundColor: 'var(--bg-surface)', borderColor: 'var(--border-color)', borderRadius: '8px' }}
                  itemStyle={{ color: 'var(--text-primary)' }}
                />
                <Area type="monotone" dataKey="present" name="Có mặt" stroke="var(--status-success)" fillOpacity={1} fill="url(#colorPresent)" />
                <Area type="monotone" dataKey="late" name="Đi muộn" stroke="var(--status-warning)" fillOpacity={1} fill="url(#colorLate)" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        <div className="card">
          <h3 className="mb-4">Hoạt động gần đây</h3>
          <div className="flex-col gap-4">
            {[1, 2, 3, 4, 5].map((i) => (
              <div key={i} className="flex items-center gap-4 activity-item pb-4" style={{ borderBottom: i !== 5 ? '1px solid var(--border-color)' : 'none' }}>
                <div className="avatar" style={{ width: '40px', height: '40px' }}>
                  <img src={`https://i.pravatar.cc/150?img=${i+10}`} alt="user" style={{ borderRadius: '50%', width: '100%', height: '100%' }} />
                </div>
                <div className="flex-1">
                  <p style={{ fontWeight: 500 }}>Nguyễn Văn {String.fromCharCode(64 + i)}</p>
                  <p className="text-muted text-sm">2412700{i}</p>
                </div>
                <div className="text-right">
                  <span className={`badge ${i % 3 === 0 ? 'badge-warning' : 'badge-success'}`}>
                    {i % 3 === 0 ? 'Đi muộn' : 'Đúng giờ'}
                  </span>
                  <p className="text-muted text-sm mt-1">07:{30 + i} AM</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
