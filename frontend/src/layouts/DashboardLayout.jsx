import { useEffect, useRef, useState } from 'react';
import { NavLink, Outlet } from 'react-router-dom';
import { Activity, LayoutDashboard, Users, Clock, BarChart3, Settings, Bell, User, Menu, X, ScanFace, CircleAlert } from 'lucide-react';
import { apiClient } from '../api/client';
import './DashboardLayout.css';

const navItems = [
  { path: '/', label: 'Dashboard', icon: LayoutDashboard },
  { path: '/users', label: 'Quản lý người dùng', icon: Users },
  { path: '/history', label: 'Lịch sử điểm danh', icon: Clock },
  { path: '/reports', label: 'Báo cáo & Thống kê', icon: BarChart3 },
  { path: '/monitoring', label: 'Giám sát thiết bị', icon: Activity },
  { path: '/settings', label: 'Cài đặt hệ thống', icon: Settings },
];

function LiveClock() {
  const [now, setNow] = useState(new Date());
  useEffect(() => {
    const id = window.setInterval(() => setNow(new Date()), 1000);
    return () => window.clearInterval(id);
  }, []);
  return <span className="header-clock" title={now.toLocaleDateString('vi-VN', { weekday: 'long', day: '2-digit', month: 'long', year: 'numeric' })}>{now.toLocaleTimeString('vi-VN')}</span>;
}

export default function DashboardLayout() {
  const [menuOpen, setMenuOpen] = useState(false);
  const [helpRequest, setHelpRequest] = useState(null);
  const knownHelpIds = useRef(new Set());
  const initializedHelpPolling = useRef(false);

  useEffect(() => {
    let cancelled = false;
    let loading = false;

    const pollHelpRequests = async () => {
      if (loading) return;
      loading = true;
      try {
        const requests = await apiClient.getAssistanceRequests();
        if (cancelled) return;

        if (!initializedHelpPolling.current) {
          requests.forEach((request) => knownHelpIds.current.add(request.id));
          initializedHelpPolling.current = true;
          return;
        }

        const newRequests = requests.filter((request) => !knownHelpIds.current.has(request.id));
        requests.forEach((request) => knownHelpIds.current.add(request.id));
        if (newRequests.length > 0) {
          setHelpRequest(newRequests[newRequests.length - 1]);
        }
      } catch {
        // Other API-backed pages already surface connectivity failures.
      } finally {
        loading = false;
      }
    };

    pollHelpRequests();
    const intervalId = window.setInterval(pollHelpRequests, 3000);
    return () => {
      cancelled = true;
      window.clearInterval(intervalId);
    };
  }, []);

  return (
    <div className="layout-container">
      {menuOpen && <button className="sidebar-backdrop" aria-label="Đóng menu" onClick={() => setMenuOpen(false)} />}
      <aside className={`sidebar ${menuOpen ? 'open' : ''}`}>
        <div className="sidebar-header flex items-center gap-4">
          <div className="logo-icon">
            <ScanFace size={22} />
          </div>
          <div><h2 className="logo-text">Attendly</h2><span className="logo-subtitle">Operations console</span></div>
          <button className="mobile-close icon-btn" onClick={() => setMenuOpen(false)}><X size={18} /></button>
        </div>
        
        <nav className="sidebar-nav flex-col gap-2">
          {navItems.map((item) => (
            <NavLink
              key={item.path}
              to={item.path}
              onClick={() => setMenuOpen(false)}
              className={({ isActive }) => `nav-link flex items-center gap-4 ${isActive ? 'active' : ''}`}
            >
              <item.icon size={20} />
              <span>{item.label}</span>
            </NavLink>
          ))}
        </nav>
        <div className="sidebar-footer"><span className="status-pulse" /> API workspace</div>
      </aside>

      {/* Main Content Area */}
      <div className="main-content flex-col">
        <header className="header flex items-center justify-between">
          <div className="header-title">
            <button className="mobile-menu icon-btn" onClick={() => setMenuOpen(true)}><Menu size={20} /></button>
            <div><h3>Trung tâm vận hành điểm danh</h3><p>Theo dõi và xử lý dữ liệu tập trung</p></div>
          </div>
          <div className="header-actions flex items-center gap-4">
            <LiveClock />
            <button className="icon-btn relative" aria-label="Thông báo hỗ trợ">
              <Bell size={20} />
              {helpRequest && <span className="notification-dot" />}
            </button>
            <div className="user-profile flex items-center gap-2">
              <div className="avatar flex items-center justify-center">
                <User size={18} />
              </div>
              <span className="user-name">Admin</span>
            </div>
          </div>
        </header>

        <main className="page-wrapper">
          <Outlet />
        </main>
      </div>

      {helpRequest && (
        <div className="help-alert-backdrop" role="presentation">
          <section className="help-alert" role="alertdialog" aria-modal="true" aria-labelledby="help-alert-title">
            <div className="help-alert-icon"><CircleAlert size={26} /></div>
            <div className="help-alert-content">
              <span className="help-alert-kicker">Yêu cầu hỗ trợ mới</span>
              <h2 id="help-alert-title">{helpRequest.user?.name || 'Người dùng tại thiết bị'}</h2>
              <p>{helpRequest.message || 'Cần quản lý hỗ trợ tại máy điểm danh.'}</p>
              {helpRequest.user?.mssv && <span className="help-alert-meta">MSSV {helpRequest.user.mssv}</span>}
            </div>
            <button className="help-alert-dismiss" autoFocus onClick={() => setHelpRequest(null)}>Đã nhận</button>
          </section>
        </div>
      )}
    </div>
  );
}
