import { useEffect, useState } from 'react';
import { Camera, Edit2, Fingerprint, Plus, Search, Trash2, X } from 'lucide-react';
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

  const loadUsers = () => {
    setLoading(true);
    setError('');
    apiClient.getUsers()
      .then(setUsers)
      .catch(() => setError('Không thể tải danh sách người dùng.'))
      .finally(() => setLoading(false));
  };

  useEffect(loadUsers, []);

  const openCreate = () => {
    setEditingId(null);
    setForm(emptyForm);
    setShowForm(true);
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
    } catch {
      setError('Không thể lưu người dùng. Kiểm tra MSSV, email và mã RFID không bị trùng.');
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
      setError('Không thể xóa người dùng này. Có thể tài khoản đã có dữ liệu điểm danh liên quan.');
    }
  };

  const search = keyword.trim().toLowerCase();
  const filtered = users.filter((user) => !search || user.name?.toLowerCase().includes(search) || user.mssv?.toLowerCase().includes(search) || user.rfidUid?.toLowerCase().includes(search));

  return (
    <div className="page-stack">
      <PageHeader title="Người dùng" description="Quản lý hồ sơ, mã RFID và trạng thái dữ liệu khuôn mặt" actions={<Button variant="primary" onClick={openCreate}><Plus size={16} /> Thêm người dùng</Button>} />
      <ErrorBanner message={error} onRetry={loadUsers} />

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
