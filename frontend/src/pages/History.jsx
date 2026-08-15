import { useEffect, useState } from 'react';
import { Download, Search, Trash2 } from 'lucide-react';
import { apiClient } from '../api/client';
import { Button, ErrorBanner, PageHeader, Panel } from '../components/ui';
import { SkeletonTable } from '../components/Skeleton';
import { useToast } from '../components/useToast.js';

const PAGE_SIZE = 20;

const dateKey = (date) => `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;

const shiftDate = (base, days) => {
  const shifted = new Date(base.getFullYear(), base.getMonth(), base.getDate() + days);
  return dateKey(shifted);
};

const now = new Date();
const todayKey = dateKey(now);
const yesterdayKey = shiftDate(now, -1);

const statusLabels = { IN: 'Vào', OUT: 'Ra', LATE: 'Đi muộn' };
const statusBadge = { IN: 'badge-success', OUT: 'badge-info', LATE: 'badge-warning' };

export default function History() {
  const [records, setRecords] = useState([]);
  const [filters, setFilters] = useState({ keyword: '', date: todayKey, status: 'ALL' });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [visibleCount, setVisibleCount] = useState(PAGE_SIZE);
  const toast = useToast();

  const loadRecords = async (date, silent = false) => {
    if (!silent) setLoading(true);
    const start = new Date(`${date}T00:00:00`);
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
    setVisibleCount(PAGE_SIZE);
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
  const visible = filtered.slice(0, visibleCount);

  const setDate = (value) => setFilters({ ...filters, date: value });

  const handleDelete = async (record) => {
    if (!window.confirm(`Xóa lượt điểm danh lúc ${new Date(record.checkInTime).toLocaleString('vi-VN')} của ${record.user?.name || 'người dùng'}?`)) return;
    try {
      await apiClient.deleteAttendanceRecord(record.id);
      setRecords((current) => current.filter((item) => item.id !== record.id));
      setError('');
      toast('Đã xóa lượt điểm danh.', 'success');
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
    toast('Đã xuất file CSV.', 'success');
  };

  return (
    <div className="page-stack">
      <PageHeader title="Lịch sử điểm danh" description="Tra cứu, lọc và xuất dữ liệu vận hành" actions={<Button variant="primary" onClick={exportCsv} disabled={!filtered.length}><Download size={16} /> Xuất CSV</Button>} />
      <ErrorBanner message={error} onRetry={() => loadRecords(filters.date)} />
      <Panel>
        <div className="filter-bar">
          <label className="search-field"><Search size={17} /><input value={filters.keyword} onChange={(event) => setFilters({ ...filters, keyword: event.target.value })} placeholder="Tên hoặc MSSV" /></label>
          <div className="quick-date-group">
            <button type="button" className={`quick-date-chip${filters.date === todayKey ? ' selected' : ''}`} onClick={() => setDate(todayKey)}>Hôm nay</button>
            <button type="button" className={`quick-date-chip${filters.date === yesterdayKey ? ' selected' : ''}`} onClick={() => setDate(yesterdayKey)}>Hôm qua</button>
          </div>
          <input className="input compact-input" type="date" value={filters.date} onChange={(event) => setDate(event.target.value)} />
          <select className="input compact-input" value={filters.status} onChange={(event) => setFilters({ ...filters, status: event.target.value })}>
            <option value="ALL">Tất cả trạng thái</option><option value="IN">Vào</option><option value="OUT">Ra</option><option value="LATE">Đi muộn</option>
          </select>
          <span className="result-count">{filtered.length} kết quả</span>
        </div>
        <div className={`table-container${loading ? ' loading' : ''}`}>
          <table>
            <thead><tr><th>Thời gian</th><th>MSSV</th><th>Họ và tên</th><th>Phương thức</th><th>Trạng thái</th><th></th></tr></thead>
            <tbody>
              {visible.map((record, index) => <tr key={record.id} style={{ animationDelay: `${index * 30}ms` }}><td>{new Date(record.checkInTime).toLocaleString('vi-VN')}</td><td className="cell-code">{record.user?.mssv || '-'}</td><td>{record.user?.name || 'Không xác định'}</td><td>{record.method || '-'}</td><td><span className={`badge ${statusBadge[record.status] || 'badge-success'}`}>{statusLabels[record.status] || record.status}</span></td><td><button className="icon-btn" onClick={() => handleDelete(record)} aria-label="Xóa lượt điểm danh" title="Xóa lượt điểm danh"><Trash2 size={16} /></button></td></tr>)}
              {!loading && !filtered.length && <tr><td colSpan="6"><div className="empty-state">Không có dữ liệu phù hợp.</div></td></tr>}
              {loading && <tr><td colSpan="6"><div className="empty-state"><SkeletonTable rows={8} cols={6} /></div></td></tr>}
            </tbody>
          </table>
        </div>
        {filtered.length > visibleCount && (
          <div className="load-more-wrap">
            <Button onClick={() => setVisibleCount((current) => current + PAGE_SIZE)}>Xem thêm ({filtered.length - visibleCount} lượt còn lại)</Button>
          </div>
        )}
      </Panel>
    </div>
  );
}
