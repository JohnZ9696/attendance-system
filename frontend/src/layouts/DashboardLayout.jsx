import { useEffect, useRef, useState } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { Activity, LayoutDashboard, Users, Clock, BarChart3, Settings, Bell, User, Menu, X, ScanFace, CircleAlert, LogOut, LifeBuoy } from 'lucide-react';
import { apiClient } from '../api/client';
import { useAuth } from '../contexts/AuthContext';
import './DashboardLayout.css';

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
  const { user, role, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const navItems = [
    { path: '/', label: 'Tổng quan', icon: LayoutDashboard },
    role === 'LEAD_PROCTOR' && { path: '/users', label: 'Quản lý người dùng', icon: Users },
    { path: '/history', label: 'Lịch sử điểm danh', icon: Clock },
    { path: '/reports', label: 'Báo cáo & Thống kê', icon: BarChart3 },
    { path: '/monitoring', label: 'Giám sát trực tiếp', icon: Activity },
    role === 'LEAD_PROCTOR' && { path: '/settings', label: 'Cài đặt hệ thống', icon: Settings },
    { path: '/support', label: 'Hỗ trợ sự cố', icon: LifeBuoy }
  ].filter(Boolean);

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
        <div className="sidebar-footer">
          <span className="status-pulse" /> Phiên bản API v1
        </div>
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
            <div className="user-profile flex items-center gap-2">
              <div className="avatar flex items-center justify-center">
                <User size={18} />
              </div>
              <span className="user-name">{user?.username || 'Admin'}</span>
              <span className="badge badge-info" style={{ marginLeft: '4px' }}>{role}</span>
            </div>
            <button className="icon-btn text-muted" onClick={handleLogout} title="Đăng xuất" id="logout-btn">
              <LogOut size={18} />
            </button>
          </div>
        </header>

        <main className="page-wrapper">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
