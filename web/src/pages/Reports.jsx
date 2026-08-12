import React from 'react';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer, PieChart, Pie, Cell } from 'recharts';

const weeklyData = [
  { name: 'Thứ 2', present: 140, late: 12, absent: 4 },
  { name: 'Thứ 3', present: 145, late: 8, absent: 3 },
  { name: 'Thứ 4', present: 138, late: 15, absent: 3 },
  { name: 'Thứ 5', present: 142, late: 10, absent: 4 },
  { name: 'Thứ 6', present: 130, late: 20, absent: 6 },
];

const pieData = [
  { name: 'Đúng giờ', value: 148 },
  { name: 'Đi muộn', value: 30 },
  { name: 'Vắng mặt', value: 8 },
];

const COLORS = ['var(--status-success)', 'var(--status-warning)', 'var(--status-error)'];

export default function Reports() {
  return (
    <div className="flex-col gap-6">
      <div className="flex items-center justify-between mb-2">
        <div>
          <h2>Báo cáo và thống kê</h2>
          <p className="text-muted">Thống kê dữ liệu điểm danh và xuất báo cáo</p>
        </div>
      </div>

      <div className="grid-content">
        <div className="card col-span-2">
          <h3 className="mb-6">Thống kê điểm danh trong tuần</h3>
          <div style={{ height: '350px' }}>
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={weeklyData} margin={{ top: 20, right: 30, left: 0, bottom: 5 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="var(--border-color)" vertical={false} />
                <XAxis dataKey="name" stroke="var(--text-muted)" />
                <YAxis stroke="var(--text-muted)" />
                <Tooltip 
                  contentStyle={{ backgroundColor: 'var(--bg-surface)', borderColor: 'var(--border-color)', borderRadius: '8px' }}
                />
                <Legend />
                <Bar dataKey="present" name="Đúng giờ" stackId="a" fill="var(--status-success)" radius={[0, 0, 4, 4]} />
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
      </div>
    </div>
  );
}
