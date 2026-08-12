import React, { useState } from 'react';
import { Plus, Search, Edit2, Trash2, Fingerprint, Camera } from 'lucide-react';

export default function Users() {
  const [users] = useState([
    { id: 1, mssv: '24127345', name: 'Nguyễn Minh Đức', rfid: 'A1:B2:C3:D4', faceRegistered: true },
    { id: 2, mssv: '24127346', name: 'Văn Phú Đức', rfid: 'E5:F6:G7:H8', faceRegistered: true },
    { id: 3, mssv: '24127475', name: 'Lương Thiện Nhân', rfid: 'I9:J0:K1:L2', faceRegistered: true },
    { id: 4, mssv: '24127001', name: 'Trần Văn A', rfid: 'Chưa đăng ký', faceRegistered: false },
  ]);

  return (
    <div className="flex-col gap-6">
      <div className="flex items-center justify-between mb-2">
        <div>
          <h2>Quản lý người dùng</h2>
          <p className="text-muted">Thêm, sửa, xóa và quản lý dữ liệu RFID/Khuôn mặt</p>
        </div>
        <button className="btn btn-primary">
          <Plus size={18} />
          Thêm người dùng mới
        </button>
      </div>

      <div className="card">
        <div className="flex items-center justify-between mb-6">
          <div className="relative" style={{ width: '300px' }}>
            <Search size={18} className="text-muted" style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)' }} />
            <input type="text" className="input" placeholder="Tìm kiếm theo tên hoặc MSSV..." style={{ paddingLeft: '40px' }} />
          </div>
          <button className="btn btn-secondary">Lọc dữ liệu</button>
        </div>

        <div className="table-container">
          <table>
            <thead>
              <tr>
                <th>MSSV</th>
                <th>Họ và tên</th>
                <th>Mã RFID</th>
                <th>Dữ liệu khuôn mặt</th>
                <th>Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {users.map(user => (
                <tr key={user.id}>
                  <td style={{ fontWeight: 500 }}>{user.mssv}</td>
                  <td>{user.name}</td>
                  <td>
                    {user.rfid === 'Chưa đăng ký' ? (
                      <span className="text-muted">{user.rfid}</span>
                    ) : (
                      <span className="badge badge-info flex items-center gap-2 w-max">
                        <Fingerprint size={14} />
                        {user.rfid}
                      </span>
                    )}
                  </td>
                  <td>
                    {user.faceRegistered ? (
                      <span className="badge badge-success flex items-center gap-2 w-max">
                        <Camera size={14} />
                        Đã đăng ký
                      </span>
                    ) : (
                      <span className="badge badge-error">Chưa đăng ký</span>
                    )}
                  </td>
                  <td>
                    <div className="flex gap-2">
                      <button className="icon-btn" title="Chỉnh sửa"><Edit2 size={18} /></button>
                      <button className="icon-btn" style={{ color: 'var(--status-error)' }} title="Xóa"><Trash2 size={18} /></button>
                    </div>
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
