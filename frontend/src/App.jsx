import React from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import DashboardLayout from './layouts/DashboardLayout';
import Dashboard from './pages/Dashboard';
import Users from './pages/Users';
import Monitoring from './pages/Monitoring';
import History from './pages/History';
import Reports from './pages/Reports';
import Settings from './pages/Settings';
import Support from './pages/Support';
import Notify from './pages/Notify';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<DashboardLayout />}>
          <Route index element={<Dashboard />} />
          <Route path="users" element={<Users />} />
          <Route path="monitoring" element={<Monitoring />} />
          <Route path="history" element={<History />} />
          <Route path="reports" element={<Reports />} />
          <Route path="settings" element={<Settings />} />
          <Route path="support" element={<Support />} />
          <Route path="notify" element={<Notify />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

export default App;
