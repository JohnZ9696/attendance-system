import React from 'react';
import { Search, Filter, Download } from 'lucide-react';

export default function History() {
  const records = [
    { id: 1, mssv: '24127345', name: 'Nguyễn Minh Đức', date: '12/08/2026', time: '07:15:30', status: 'Đúng giờ', method: 'Face + RFID' },
    { id: 2, mssv: '24127346', name: 'Văn Phú Đức', date: '12/08/2026', time: '07:25:10', status: 'Đúng giờ', method: 'Face + RFID' },
    { id: 3, mssv: '24127475', name: 'Lương Thiện Nhân', date: '12/08/2026', time: '07:45:00', status: 'Đi muộn', method: 'Face + RFID' },
    { id: 4, mssv: '24127001', name: 'Trần Văn A', date: '12/08/2026', time: '--:--:--', status: 'Vắng mặt', method: '-' },
  ];

  return (
    <div className="flex-col gap-6">
      <div className="flex items-center justify-between mb-2">
        <div>
          <h2>Lịch sử điểm danh</h2>
          <p className="text-muted">Tra cứu lịch sử theo ngày, tháng, năm</p>
        </div>
        <button className="btn btn-primary" style={{ backgroundColor: 'var(--status-success)' }}>
          <Download size={18} />
          Xuất báo cáo Excel
        </button>
      </div>

      <div className="card">
        <div className="flex items-center gap-4 mb-6">
          <div className="relative flex-1">
            <Search size={18} className="text-muted" style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)' }} />
            <input type="text" className="input" placeholder="Tìm kiếm theo tên hoặc MSSV..." style={{ paddingLeft: '40px' }} />
          </div>
          <div className="relative" style={{ width: '200px' }}>
            <input type="date" className="input" defaultValue="2026-08-12" />
          </div>
          <button className="btn btn-secondary">
            <Filter size={18} />
            Bộ lọc nâng cao
          </button>
        </div>

        <div className="table-container">
          <table>
            <thead>
              <tr>
                <th>Ngày</th>
                <th>Thời gian</th>
                <th>MSSV</th>
                <th>Họ và tên</th>
                <th>Phương thức</th>
                <th>Trạng thái</th>
              </tr>
            </thead>
            <tbody>
              {records.map(record => (
                <tr key={record.id}>
                  <td>{record.date}</td>
                  <td style={{ fontFamily: 'monospace', fontSize: '14px' }}>{record.time}</td>
                  <td style={{ fontWeight: 500 }}>{record.mssv}</td>
                  <td>{record.name}</td>
                  <td className="text-muted">{record.method}</td>
                  <td>
                    <span className={`badge ${
                      record.status === 'Đúng giờ' ? 'badge-success' : 
                      record.status === 'Đi muộn' ? 'badge-warning' : 'badge-error'
                    }`}>
                      {record.status}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
