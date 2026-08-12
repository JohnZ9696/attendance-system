import React, { useEffect, useState } from 'react';
import {
  Plus,
  Search,
  Edit2,
  Trash2,
  Fingerprint,
  Camera
} from 'lucide-react';

export default function Users() {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [keyword, setKeyword] = useState('');
  const [showAddForm, setShowAddForm] = useState(false);

  const [newUser, setNewUser] = useState({
    studentCode: '',
    fullName: '',
    rfidUid: '',
    email: '',
    parentPhone: '',
    faceImage: null
  });

  const handleAddUser = async (e) => {
  e.preventDefault();

  try {
    const response = await fetch('http://localhost:8080/api/students', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(newUser)
    });

    if (!response.ok) {
      throw new Error('Thêm sinh viên thất bại');
    }

    const createdUser = await response.json();

    // Cập nhật bảng ngay lập tức
    setUsers((prev) => [...prev, createdUser]);

    // Đóng form
    setShowAddForm(false);

    // Reset form
    setNewUser({
      studentCode: '',
      fullName: '',
      rfidUid: '',
      email: '',
      parentPhone: '',
      faceImage: null
    });

    alert('Thêm người dùng thành công!');
  } catch (error) {
    console.error(error);
    alert('Không thể thêm người dùng');
  }
};

  useEffect(() => {
    fetch('http://localhost:8080/api/students')
      .then((res) => {
        if (!res.ok) {
          throw new Error('Không thể lấy danh sách học sinh');
        }
        return res.json();
      })
      .then((data) => {
        setUsers(data);
      })
      .catch((error) => {
        console.error('Lỗi lấy dữ liệu:', error);
      })
      .finally(() => {
        setLoading(false);
      });
  }, []);

  const filteredUsers = users.filter((user) => {
    const search = keyword.toLowerCase();

    return (
      user.studentCode?.toLowerCase().includes(search) ||
      user.fullName?.toLowerCase().includes(search)
    );
  });

  const handleDelete = async (id) => {
    const confirmDelete = window.confirm(
      'Bạn có chắc muốn xóa người dùng này không?'
    );


    if (!confirmDelete) return;

    try {
      const response = await fetch(
        `http://localhost:8080/api/students/${id}`,
        {
          method: 'DELETE'
        }
      );

      if (!response.ok) {
        throw new Error('Xóa thất bại');
      }

      setUsers((prevUsers) =>
        prevUsers.filter((user) => user.id !== id)
      );
    } catch (error) {
      console.error(error);
      alert('Không thể xóa người dùng');
    }
  };

  return (
    <div className="flex-col gap-6">

      <div className="flex items-center justify-between mb-2">
        <div>
          <h2>Quản lý người dùng</h2>
          <p className="text-muted">
            Thêm, sửa, xóa và quản lý dữ liệu RFID/Khuôn mặt
          </p>
        </div>

        <button
          className="btn btn-primary"
          onClick={() => setShowAddForm(true)}
        >
          <Plus size={18} />
          Thêm người dùng mới
        </button>
      </div>

      {showAddForm && (
        <div className="card">
          <h3 style={{ marginBottom: '20px' }}>
            Thêm người dùng mới
          </h3>

          <form
            onSubmit={handleAddUser}
            className="flex-col gap-4"
          >
            <input
              className="input"
              placeholder="MSSV"
              required
              value={newUser.studentCode}
              onChange={(e) =>
                setNewUser({
                  ...newUser,
                  studentCode: e.target.value
                })
              }
            />

            <input
              className="input"
              placeholder="Họ và tên"
              required
              value={newUser.fullName}
              onChange={(e) =>
                setNewUser({
                  ...newUser,
                  fullName: e.target.value
                })
              }
            />

            <input
              className="input"
              placeholder="Mã RFID"
              required
              value={newUser.rfidUid}
              onChange={(e) =>
                setNewUser({
                  ...newUser,
                  rfidUid: e.target.value
                })
              }
            />

            <input
              className="input"
              type="email"
              placeholder="Email"
              value={newUser.email}
              onChange={(e) =>
                setNewUser({
                  ...newUser,
                  email: e.target.value
                })
              }
            />

            <input
              className="input"
              placeholder="SĐT phụ huynh"
              value={newUser.parentPhone}
              onChange={(e) =>
                setNewUser({
                  ...newUser,
                  parentPhone: e.target.value
                })
              }
            />

            <div className="flex gap-2">
              <button
                type="submit"
                className="btn btn-primary"
              >
                Thêm
              </button>

              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => setShowAddForm(false)}
              >
                Hủy
              </button>
            </div>
          </form>
        </div>
      )}

      <div className="card">
        <div className="flex items-center justify-between mb-6">
          <div className="relative" style={{ width: '300px' }}>
            <Search
              size={18}
              className="text-muted"
              style={{
                position: 'absolute',
                left: '12px',
                top: '50%',
                transform: 'translateY(-50%)'
              }}
            />

            <input
              type="text"
              className="input"
              placeholder="Tìm kiếm theo tên hoặc MSSV..."
              style={{ paddingLeft: '40px' }}
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
            />
          </div>

          <button className="btn btn-secondary">
            Lọc dữ liệu
          </button>
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
              {loading ? (
                <tr>
                  <td colSpan="5">
                    Đang tải dữ liệu...
                  </td>
                </tr>
              ) : filteredUsers.length === 0 ? (
                <tr>
                  <td colSpan="5">
                    Không có dữ liệu
                  </td>
                </tr>
              ) : (
                filteredUsers.map((user) => (
                  <tr key={user.id}>
                    <td style={{ fontWeight: 500 }}>
                      {user.studentCode}
                    </td>

                    <td>{user.fullName}</td>

                    <td>
                      {!user.rfidUid ? (
                        <span className="text-muted">
                          Chưa đăng ký
                        </span>
                      ) : (
                        <span className="badge badge-info flex items-center gap-2 w-max">
                          <Fingerprint size={14} />
                          {user.rfidUid}
                        </span>
                      )}
                    </td>

                    <td>
                      {user.faceImage ? (
                        <span className="badge badge-success flex items-center gap-2 w-max">
                          <Camera size={14} />
                          Đã đăng ký
                        </span>
                      ) : (
                        <span className="badge badge-error">
                          Chưa đăng ký
                        </span>
                      )}
                    </td>

                    <td>
                      <div className="flex gap-2">
                        <button
                          className="icon-btn"
                          title="Chỉnh sửa"
                        >
                          <Edit2 size={18} />
                        </button>

                        <button
                          className="icon-btn"
                          style={{
                            color: 'var(--status-error)'
                          }}
                          title="Xóa"
                          onClick={() => handleDelete(user.id)}
                        >
                          <Trash2 size={18} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

    </div>
  );
}