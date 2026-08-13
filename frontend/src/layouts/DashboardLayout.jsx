import React from 'react';
import { NavLink, Outlet } from 'react-router-dom';
import { LayoutDashboard, Users, Camera, Clock, BarChart3, Settings, AlertTriangle, Bell, User } from 'lucide-react';
import './DashboardLayout.css';

const navItems = [
  { path: '/', label: 'Dashboard', icon: LayoutDashboard },
  { path: '/users', label: 'Quản lý người dùng', icon: Users },
  { path: '/monitoring', label: 'Giám sát trực tiếp', icon: Camera },
  { path: '/history', label: 'Lịch sử điểm danh', icon: Clock },
  { path: '/reports', label: 'Báo cáo & Thống kê', icon: BarChart3 },
  { path: '/settings', label: 'Cài đặt hệ thống', icon: Settings },
  { path: '/support', label: 'Hỗ trợ / Báo lỗi', icon: AlertTriangle },
];

export default function DashboardLayout() {
  return (
    <div className="layout-container flex">
      {/* Sidebar */}
      <aside className="sidebar flex-col">
        <div className="sidebar-header flex items-center gap-4">
          <div className="logo-icon glass-panel">
            <Camera size={24} color="var(--accent-primary)" />
          </div>
          <h2 className="logo-text">Smart Attend</h2>
        </div>
        
        <nav className="sidebar-nav flex-col gap-2">
          {navItems.map((item) => (
            <NavLink
              key={item.path}
              to={item.path}
              className={({ isActive }) => `nav-link flex items-center gap-4 ${isActive ? 'active' : ''}`}
            >
              <item.icon size={20} />
              <span>{item.label}</span>
            </NavLink>
          ))}
        </nav>
      </aside>

      {/* Main Content Area */}
      <div className="main-content flex-col">
        {/* Header */}
        <header className="header flex items-center justify-between glass-panel">
          <div className="header-title">
            <h3>Hệ thống điểm danh khuôn mặt & RFID</h3>
          </div>
          <div className="header-actions flex items-center gap-4">
            <button className="icon-btn relative">
              <Bell size={20} />
              <span className="notification-dot"></span>
            </button>
            <div className="user-profile flex items-center gap-2">
              <div className="avatar flex items-center justify-center">
                <User size={18} />
              </div>
              <span className="user-name">Admin</span>
            </div>
          </div>
        </header>

        {/* Page Content */}
        <main className="page-wrapper">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
