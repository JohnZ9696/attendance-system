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
  getAttendanceBetween(start, end) {
    const params = new URLSearchParams({ start, end });
    return request(`/attendance/between?${params}`);
  },
  recordAttendance(data) {
    const params = new URLSearchParams(data);
    return request(`/attendance?${params}`, {
      method: 'POST',
    });
  },
};
