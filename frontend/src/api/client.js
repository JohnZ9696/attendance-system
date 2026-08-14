const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

async function request(path, options) {
  const response = await fetch(`${API_BASE_URL}${path}`, options);
  if (!response.ok) {
    const message = await response.text();
    throw new Error(message || `Request failed with status ${response.status}`);
  }
  if (response.status === 204) return null;
  return response.json();
}

export const apiClient = {
  getUsers: () => request('/users'),
  getUserByRfid: (rfidUid) => request(`/users/rfid/${encodeURIComponent(rfidUid)}`),
  createUser: (user) => request('/users', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(user),
  }),
  updateUser: (id, user) => request(`/users/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(user),
  }),
  deleteUser: (id) => request(`/users/${id}`, { method: 'DELETE' }),
  getAttendanceRecords: () => request('/attendance'),
  getTodayAttendance: () => request('/attendance/today'),
  deleteAttendanceRecord: (id) => request(`/attendance/${id}`, { method: 'DELETE' }),
  getAssistanceRequests: () => request('/assistance'),
  startRfidEnrollment: () => request('/rfid-enrollment/start', { method: 'POST' }),
  getRfidEnrollment: () => request('/rfid-enrollment'),
  cancelRfidEnrollment: () => request('/rfid-enrollment/cancel', { method: 'POST' }),
  getAttendanceBetween(start, end) {
    const params = new URLSearchParams({ start, end });
    return request(`/attendance/between?${params}`);
  },
  getSettings: () => request('/settings'),
  updateSettings(settings) {
    return request('/settings', {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(settings),
    });
  },
  recordAttendance(data) {
    const params = new URLSearchParams(data);
    return request(`/attendance?${params}`, {
      method: 'POST',
    });
  },
};
