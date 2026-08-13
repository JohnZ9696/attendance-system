import { useState } from 'react';
import { NavLink, Outlet } from 'react-router-dom';
import { LayoutDashboard, Users, Clock, BarChart3, Settings, Bell, User, Menu, X, ScanFace } from 'lucide-react';
import './DashboardLayout.css';

const navItems = [
  { path: '/', label: 'Dashboard', icon: LayoutDashboard },
  { path: '/users', label: 'Quản lý người dùng', icon: Users },
  { path: '/history', label: 'Lịch sử điểm danh', icon: Clock },
  { path: '/reports', label: 'Báo cáo & Thống kê', icon: BarChart3 },
  { path: '/settings', label: 'Cài đặt hệ thống', icon: Settings },
];

export default function DashboardLayout() {
  const [menuOpen, setMenuOpen] = useState(false);

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
            <button className="icon-btn relative">
              <Bell size={20} />
              <span className="notification-dot" />
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
    </div>
  );
}
