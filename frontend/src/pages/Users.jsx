import { useEffect, useState } from 'react';
import { Camera, Edit2, Fingerprint, Plus, ScanLine, Search, Trash2, X } from 'lucide-react';
import { apiClient } from '../api/client';
import { Button, ErrorBanner, PageHeader, Panel } from '../components/ui';

const emptyForm = { name: '', mssv: '', rfidUid: '', faceEmbedding: '' };

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
    } catch {
      setError('Không thể xóa người dùng này. Kiểm tra kết nối backend và thử lại.');
    }
  };

  const search = keyword.trim().toLowerCase();
  const filtered = users.filter((user) => !search || user.name?.toLowerCase().includes(search) || user.mssv?.toLowerCase().includes(search) || user.rfidUid?.toLowerCase().includes(search));

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
                <tr key={user.id}>
                  <td><div className="person-cell"><span className="initials">{user.name?.slice(0, 1) || '?'}</span><strong>{user.name}</strong></div></td>
                  <td className="cell-code">{user.mssv}</td>
                  <td>{user.rfidUid ? <span className="badge badge-info"><Fingerprint size={13} /> {user.rfidUid}</span> : <span className="text-muted">Chưa đăng ký</span>}</td>
                  <td>{user.faceEmbedding ? <span className="badge badge-success"><Camera size={13} /> Đã có</span> : <span className="badge badge-neutral">Chưa có</span>}</td>
                  <td><div className="row-actions"><button className="icon-btn" onClick={() => openEdit(user)} title="Chỉnh sửa"><Edit2 size={17} /></button><button className="icon-btn danger" onClick={() => handleDelete(user)} title="Xóa"><Trash2 size={17} /></button></div></td>
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
