import { useEffect, useState } from 'react';
import { Camera, Edit2, Fingerprint, Plus, ScanLine, Search, Trash2, X } from 'lucide-react';
import { apiClient } from '../api/client';
import { Button, ErrorBanner, PageHeader, Panel } from '../components/ui';

const emptyForm = { name: '', mssv: '', rfidUid: '', faceEmbedding: '' };
const statusLabels = { IN: 'Vào', OUT: 'Ra', LATE: 'Đi muộn' };
const statusBadge = { IN: 'badge-success', OUT: 'badge-info', LATE: 'badge-warning' };

export default function Users() {
  const [users, setUsers] = useState([]);
  const [keyword, setKeyword] = useState('');
  const [form, setForm] = useState(emptyForm);
  const [editingId, setEditingId] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [enrollment, setEnrollment] = useState(null);
  const [selectedUser, setSelectedUser] = useState(null);
  const [userLogs, setUserLogs] = useState([]);
  const [userLogsLoading, setUserLogsLoading] = useState(false);
  const [userLogsError, setUserLogsError] = useState('');
  const [absenceRange, setAbsenceRange] = useState('month');
  const [dayOffs, setDayOffs] = useState([]);
  const [weeklyDayOffs, setWeeklyDayOffs] = useState([0, 6]);

  const loadUsers = () => {
    setLoading(true);
    setError('');
    apiClient.getUsers()
      .then(setUsers)
      .catch(() => setError('Không thể tải danh sách người dùng.'))
      .finally(() => setLoading(false));
  };

  useEffect(loadUsers, []);

  useEffect(() => {
    if (enrollment !== 'waiting') return undefined;
    let cancelled = false;

    const poll = async () => {
      try {
        const state = await apiClient.getRfidEnrollment();
        if (!cancelled && state.status === 'SCANNED' && state.uid) {
          setForm({ ...emptyForm, rfidUid: state.uid });
          setEnrollment('form');
        }
      } catch {
        if (!cancelled) setError('Mất kết nối khi chờ ESP32 quét thẻ.');
      }
    };

    const intervalId = window.setInterval(poll, 1000);
    poll();
    return () => {
      cancelled = true;
      window.clearInterval(intervalId);
    };
  }, [enrollment]);

  useEffect(() => {
    if (!selectedUser) return undefined;
    let cancelled = false;
    setUserLogsLoading(true);
    setUserLogsError('');
    Promise.all([apiClient.getUserAttendance(selectedUser.id), apiClient.getSettings()])
      .then(([logs, settings]) => {
        if (cancelled) return;
        setUserLogs(logs);
        setDayOffs(Array.isArray(settings.dayOffs) ? settings.dayOffs.map((value) => value.slice(0, 10)) : []);
        setWeeklyDayOffs(Array.isArray(settings.weeklyDayOffs) ? settings.weeklyDayOffs : [0, 6]);
      })
      .catch(() => { if (!cancelled) setUserLogsError('Không thể tải lịch sử điểm danh.'); })
      .finally(() => { if (!cancelled) setUserLogsLoading(false); });
    return () => { cancelled = true; };
  }, [selectedUser]);

  const openDetail = (user) => {
    setSelectedUser(user);
    setUserLogs([]);
  };

  const closeDetail = () => {
    setSelectedUser(null);
    setUserLogs([]);
  };

  const openCreate = () => {
    setEditingId(null);
    setForm(emptyForm);
    setShowForm(true);
  };

  const startEnrollment = async () => {
    setError('');
    setEditingId(null);
    setForm(emptyForm);
    try {
      await apiClient.startRfidEnrollment();
      setEnrollment('waiting');
    } catch {
      setError('Không thể bắt đầu đăng ký thẻ. Kiểm tra backend và ESP32.');
    }
  };

  const closeEnrollment = async () => {
    setEnrollment(null);
    setForm(emptyForm);
    try {
      await apiClient.cancelRfidEnrollment();
    } catch {
      // Closing the local dialog should not be blocked by a network failure.
    }
  };

  const openEdit = (user) => {
    setEditingId(user.id);
    setForm({ name: user.name || '', mssv: user.mssv || '', rfidUid: user.rfidUid || '', faceEmbedding: user.faceEmbedding || '' });
    setShowForm(true);
  };

  const closeForm = () => {
    setShowForm(false);
    setEditingId(null);
    setForm(emptyForm);
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setSaving(true);
    setError('');
    try {
      const saved = editingId ? await apiClient.updateUser(editingId, form) : await apiClient.createUser(form);
      setUsers((current) => editingId ? current.map((user) => user.id === editingId ? saved : user) : [saved, ...current]);
      closeForm();
      setEnrollment(null);
    } catch {
      setError('Không thể lưu người dùng. Kiểm tra MSSV và mã RFID không bị trùng.');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (user) => {
    if (!window.confirm(`Xóa ${user.name}? Thao tác này không thể hoàn tác.`)) return;
    try {
      await apiClient.deleteUser(user.id);
      setUsers((current) => current.filter((item) => item.id !== user.id));
      if (selectedUser?.id === user.id) closeDetail();
    } catch {
      setError('Không thể xóa người dùng này. Kiểm tra kết nối backend và thử lại.');
    }
  };

  const search = keyword.trim().toLowerCase();
  const filtered = users.filter((user) => !search || user.name?.toLowerCase().includes(search) || user.mssv?.toLowerCase().includes(search) || user.rfidUid?.toLowerCase().includes(search));

  const sortedLogs = [...userLogs].sort((a, b) => new Date(b.checkInTime) - new Date(a.checkInTime));
  const presentDays = new Set(userLogs.map((log) => new Date(log.checkInTime).toLocaleDateString('en-CA')));
  const today = new Date();
  let rangeStart;
  if (absenceRange === '7') rangeStart = new Date(today.getFullYear(), today.getMonth(), today.getDate() - 6);
  else if (absenceRange === '30') rangeStart = new Date(today.getFullYear(), today.getMonth(), today.getDate() - 29);
  else rangeStart = new Date(today.getFullYear(), today.getMonth(), 1);
  if (selectedUser?.createdAt) {
    const created = new Date(selectedUser.createdAt);
    if (created > rangeStart) rangeStart = new Date(created.getFullYear(), created.getMonth(), created.getDate());
  }
  const absentDays = [];
  for (let day = new Date(rangeStart); day <= today; day.setDate(day.getDate() + 1)) {
    const dow = day.getDay();
    if (weeklyDayOffs.includes(dow)) continue;
    const key = day.toLocaleDateString('en-CA');
    if (dayOffs.includes(key)) continue;
    if (!presentDays.has(key)) absentDays.push(new Date(day));
  }
  const lateCount = userLogs.filter((log) => log.status === 'LATE').length;

  return (
    <div className="page-stack">
      <PageHeader title="Người dùng" description="Quản lý hồ sơ, mã RFID và trạng thái dữ liệu khuôn mặt" actions={<><Button onClick={startEnrollment}><ScanLine size={16} /> Đăng ký thẻ</Button><Button variant="primary" onClick={openCreate}><Plus size={16} /> Thêm người dùng</Button></>} />
      <ErrorBanner message={error} onRetry={loadUsers} />

      {enrollment && (
        <div className="enrollment-backdrop" role="presentation">
          <section className="enrollment-dialog" role="dialog" aria-modal="true" aria-labelledby="enrollment-title">
            <div className="panel-heading">
              <div><h2 id="enrollment-title">{enrollment === 'waiting' ? 'Quét thẻ RFID' : 'Thông tin người dùng'}</h2><p>{enrollment === 'waiting' ? 'Đưa thẻ mới lại gần đầu đọc ESP32' : 'UID đã nhận từ thiết bị. Hoàn tất hồ sơ để đăng ký.'}</p></div>
              <button className="icon-btn" onClick={closeEnrollment} aria-label="Đóng"><X size={18} /></button>
            </div>
            {enrollment === 'waiting' ? (
              <div className="enrollment-waiting"><span className="scan-rings"><Fingerprint size={34} /></span><strong>ESP32 đang chờ quét thẻ...</strong><p>Không quét thẻ đã thuộc về người dùng khác.</p></div>
            ) : (
              <form className="form-grid" onSubmit={handleSubmit}>
                <label><span>Họ và tên *</span><input className="input" required autoFocus value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} /></label>
                <label><span>MSSV *</span><input className="input" required value={form.mssv} onChange={(event) => setForm({ ...form, mssv: event.target.value })} /></label>
                <label className="form-wide"><span>Mã RFID từ ESP32</span><input className="input" readOnly value={form.rfidUid} /></label>
                <div className="form-actions form-wide"><Button type="button" onClick={closeEnrollment}>Hủy</Button><Button variant="primary" type="submit" disabled={saving}>{saving ? 'Đang lưu...' : 'Tạo người dùng'}</Button></div>
              </form>
            )}
          </section>
        </div>
      )}

      {showForm && (
        <Panel className="form-panel">
          <div className="panel-heading"><div><h2>{editingId ? 'Chỉnh sửa hồ sơ' : 'Thêm người dùng'}</h2><p>Các trường có dấu * là bắt buộc</p></div><button className="icon-btn" onClick={closeForm} aria-label="Đóng"><X size={18} /></button></div>
          <form className="form-grid" onSubmit={handleSubmit}>
            <label><span>Họ và tên *</span><input className="input" required value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} /></label>
            <label><span>MSSV *</span><input className="input" required value={form.mssv} onChange={(event) => setForm({ ...form, mssv: event.target.value })} /></label>
            <label><span>Mã RFID *</span><input className="input" required value={form.rfidUid} onChange={(event) => setForm({ ...form, rfidUid: event.target.value })} /></label>
            <label className="form-wide"><span>Tham chiếu dữ liệu khuôn mặt</span><input className="input" value={form.faceEmbedding} onChange={(event) => setForm({ ...form, faceEmbedding: event.target.value })} placeholder="Để trống nếu chưa đăng ký" /></label>
            <div className="form-actions form-wide"><Button type="button" onClick={closeForm}>Hủy</Button><Button variant="primary" type="submit" disabled={saving}>{saving ? 'Đang lưu...' : editingId ? 'Lưu thay đổi' : 'Tạo người dùng'}</Button></div>
          </form>
        </Panel>
      )}

      {selectedUser && (
        <div className="enrollment-backdrop" role="presentation">
          <section className="enrollment-dialog" role="dialog" aria-modal="true" aria-labelledby="detail-title" style={{ width: 'min(940px, 100%)' }}>
            <div className="panel-heading">
              <div>
                <h2 id="detail-title">{selectedUser.name}</h2>
                <p>MSSV {selectedUser.mssv}{selectedUser.rfidUid ? ` · RFID ${selectedUser.rfidUid}` : ''}</p>
              </div>
              <button className="icon-btn" onClick={closeDetail} aria-label="Đóng"><X size={18} /></button>
            </div>

            {userLogsError && <div className="text-muted" style={{ color: 'var(--status-error)', marginBottom: 12 }}>{userLogsError}</div>}

            <div className="filter-bar">
              <label className="text-sm text-muted">Khoảng thời gian tính vắng</label>
              <select className="input compact-input" value={absenceRange} onChange={(event) => setAbsenceRange(event.target.value)}>
                <option value="month">Tháng này</option>
                <option value="7">7 ngày qua</option>
                <option value="30">30 ngày qua</option>
              </select>
            </div>

            <div className="stat-chip-row">
              <div className="stat-chip"><strong>{presentDays.size}</strong><span>Đã điểm danh (ngày)</span></div>
              <div className="stat-chip"><strong>{lateCount}</strong><span>Đi muộn</span></div>
              <div className="stat-chip"><strong>{absentDays.length}</strong><span>Vắng (ngày học)</span></div>
            </div>

            <div className="detail-columns">
              <div className="detail-col">
                <h3>Lịch sử điểm danh</h3>
                <div className="table-container">
                  <table>
                    <thead><tr><th>Thời gian</th><th>Trạng thái</th><th>Phương thức</th></tr></thead>
                    <tbody>
                      {sortedLogs.map((log) => (
                        <tr key={log.id}>
                          <td>{new Date(log.checkInTime).toLocaleString('vi-VN')}</td>
                          <td><span className={`badge ${statusBadge[log.status] || 'badge-success'}`}>{statusLabels[log.status] || log.status}</span></td>
                          <td>{log.method || '-'}</td>
                        </tr>
                      ))}
                      {!userLogsLoading && !sortedLogs.length && <tr><td colSpan="3"><div className="empty-state">Chưa có lượt điểm danh.</div></td></tr>}
                      {userLogsLoading && <tr><td colSpan="3"><div className="empty-state">Đang tải...</div></td></tr>}
                    </tbody>
                  </table>
                </div>
              </div>
              <div className="detail-col">
                <h3>Ngày vắng</h3>
                {absentDays.length ? (
                  <ul className="absence-list">
                    {absentDays.map((day) => <li key={day.toISOString()}>{day.toLocaleDateString('vi-VN', { weekday: 'long', day: '2-digit', month: '2-digit', year: 'numeric' })}</li>)}
                  </ul>
                ) : <div className="empty-state">Không có ngày vắng trong khoảng này.</div>}
              </div>
            </div>
          </section>
        </div>
      )}

      <Panel>
        <div className="filter-bar">
          <label className="search-field"><Search size={17} /><input value={keyword} onChange={(event) => setKeyword(event.target.value)} placeholder="Tìm theo tên, MSSV hoặc RFID" /></label>
          <span className="result-count">{filtered.length}/{users.length} người dùng</span>
        </div>
        <div className="table-container">
          <table>
            <thead><tr><th>Người dùng</th><th>MSSV</th><th>RFID</th><th>Khuôn mặt</th><th></th></tr></thead>
            <tbody>
              {filtered.map((user) => (
                <tr key={user.id} className="row-clickable" onClick={() => openDetail(user)}>
                  <td><div className="person-cell"><span className="initials">{user.name?.slice(0, 1) || '?'}</span><strong>{user.name}</strong></div></td>
                  <td className="cell-code">{user.mssv}</td>
                  <td>{user.rfidUid ? <span className="badge badge-info"><Fingerprint size={13} /> {user.rfidUid}</span> : <span className="text-muted">Chưa đăng ký</span>}</td>
                  <td>{user.faceEmbedding ? <span className="badge badge-success"><Camera size={13} /> Đã có</span> : <span className="badge badge-neutral">Chưa có</span>}</td>
                  <td><div className="row-actions"><button className="icon-btn" onClick={(event) => { event.stopPropagation(); openEdit(user); }} title="Chỉnh sửa"><Edit2 size={17} /></button><button className="icon-btn danger" onClick={(event) => { event.stopPropagation(); handleDelete(user); }} title="Xóa"><Trash2 size={17} /></button></div></td>
                </tr>
              ))}
              {!loading && !filtered.length && <tr><td colSpan="5"><div className="empty-state">Không tìm thấy người dùng.</div></td></tr>}
              {loading && <tr><td colSpan="5"><div className="empty-state">Đang tải dữ liệu...</div></td></tr>}
            </tbody>
          </table>
        </div>
      </Panel>
    </div>
  );
}
