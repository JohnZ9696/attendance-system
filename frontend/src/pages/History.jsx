import { useEffect, useState } from 'react';
import { Download, Search, Trash2 } from 'lucide-react';
import { apiClient } from '../api/client';
import { Button, ErrorBanner, PageHeader, Panel } from '../components/ui';

const today = new Date().toISOString().slice(0, 10);
const statusLabels = { IN: 'Vào', OUT: 'Ra', LATE: 'Đi muộn' };
const statusBadge = { IN: 'badge-success', OUT: 'badge-info', LATE: 'badge-warning' };

export default function History() {
  const [records, setRecords] = useState([]);
  const [filters, setFilters] = useState({ keyword: '', date: today, status: 'ALL' });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadRecords = async (date, silent = false) => {
    if (!silent) setLoading(true);
    const start = new Date(`${date}T00:00:00Z`);
    const end = new Date(start.getTime() + 24 * 60 * 60 * 1000);
    try {
      setRecords(await apiClient.getAttendanceBetween(start.toISOString(), end.toISOString()));
      setError('');
    } catch {
      if (!silent) setError('Không thể tải lịch sử điểm danh.');
    } finally {
      if (!silent) setLoading(false);
    }
  };

  useEffect(() => {
    loadRecords(filters.date);
    const intervalId = window.setInterval(() => loadRecords(filters.date, true), 3000);
    return () => window.clearInterval(intervalId);
  }, [filters.date]);

  const keyword = filters.keyword.trim().toLowerCase();
  const filtered = [...records]
    .sort((a, b) => new Date(b.checkInTime) - new Date(a.checkInTime))
    .filter((record) => {
      const matchesKeyword = !keyword || record.user?.name?.toLowerCase().includes(keyword) || record.user?.mssv?.toLowerCase().includes(keyword);
      return matchesKeyword && (filters.status === 'ALL' || record.status === filters.status);
    });

  const handleDelete = async (record) => {
    if (!window.confirm(`Xóa lượt điểm danh lúc ${new Date(record.checkInTime).toLocaleString('vi-VN')} của ${record.user?.name || 'người dùng'}?`)) return;
    try {
      await apiClient.deleteAttendanceRecord(record.id);
      setRecords((current) => current.filter((item) => item.id !== record.id));
      setError('');
    } catch {
      setError('Không thể xóa lượt điểm danh này.');
    }
  };

  const exportCsv = () => {
    const rows = [['Thời gian', 'MSSV', 'Họ và tên', 'Phương thức', 'Trạng thái'], ...filtered.map((record) => [record.checkInTime, record.user?.mssv || '', record.user?.name || '', record.method || '', statusLabels[record.status] || record.status])];
    const csv = rows.map((row) => row.map((cell) => `"${String(cell).replaceAll('"', '""')}"`).join(',')).join('\n');
    const link = document.createElement('a');
    link.href = URL.createObjectURL(new Blob([`\ufeff${csv}`], { type: 'text/csv;charset=utf-8' }));
    link.download = `attendance-${filters.date}.csv`;
    link.click();
    URL.revokeObjectURL(link.href);
  };

  return (
    <div className="page-stack">
      <PageHeader title="Lịch sử điểm danh" description="Tra cứu, lọc và xuất dữ liệu vận hành" actions={<Button variant="primary" onClick={exportCsv} disabled={!filtered.length}><Download size={16} /> Xuất CSV</Button>} />
      <ErrorBanner message={error} onRetry={() => loadRecords(filters.date)} />
      <Panel>
        <div className="filter-bar">
          <label className="search-field"><Search size={17} /><input value={filters.keyword} onChange={(event) => setFilters({ ...filters, keyword: event.target.value })} placeholder="Tên hoặc MSSV" /></label>
          <input className="input compact-input" type="date" value={filters.date} onChange={(event) => setFilters({ ...filters, date: event.target.value })} />
          <select className="input compact-input" value={filters.status} onChange={(event) => setFilters({ ...filters, status: event.target.value })}>
            <option value="ALL">Tất cả trạng thái</option><option value="IN">Vào</option><option value="OUT">Ra</option><option value="LATE">Đi muộn</option>
          </select>
          <span className="result-count">{filtered.length} kết quả</span>
        </div>
        <div className="table-container">
          <table>
            <thead><tr><th>Thời gian</th><th>MSSV</th><th>Họ và tên</th><th>Phương thức</th><th>Trạng thái</th><th></th></tr></thead>
            <tbody>
              {filtered.map((record) => <tr key={record.id}><td>{new Date(record.checkInTime).toLocaleString('vi-VN')}</td><td className="cell-code">{record.user?.mssv || '-'}</td><td>{record.user?.name || 'Không xác định'}</td><td>{record.method || '-'}</td><td><span className={`badge ${statusBadge[record.status] || 'badge-success'}`}>{statusLabels[record.status] || record.status}</span></td><td><button className="icon-btn" onClick={() => handleDelete(record)} aria-label="Xóa lượt điểm danh" title="Xóa lượt điểm danh"><Trash2 size={16} /></button></td></tr>)}
              {!loading && !filtered.length && <tr><td colSpan="6"><div className="empty-state">Không có dữ liệu phù hợp.</div></td></tr>}
              {loading && <tr><td colSpan="6"><div className="empty-state">Đang tải dữ liệu...</div></td></tr>}
            </tbody>
          </table>
        </div>
      </Panel>
    </div>
  );
}
