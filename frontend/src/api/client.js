const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://172.20.10.5:8080/api/v1';

async function request(path, options = {}) {
  const headers = {
    'Content-Type': 'application/json',
    ...options.headers,
  };

  const response = await fetch(`${API_BASE_URL}${path}`, { ...options, headers });

  if (!response.ok) {
    const message = await response.text();
    throw new Error(message || `Request failed with status ${response.status}`);
  }

  if (response.status === 204) return null;
  
  const contentType = response.headers.get('content-type');
  if (contentType && contentType.includes('application/vnd.openxmlformats-officedocument.spreadsheetml.sheet')) {
    return response.blob();
  }

  return response.json();
}

export const apiClient = {
  getUsers: () => request('/users'),
  createUser: (user) => request('/users', {
    method: 'POST',
    body: JSON.stringify(user),
  }),
  updateUser: (id, user) => request(`/users/${id}`, {
    method: 'PATCH',
    body: JSON.stringify(user),
  }),
  deleteUser: (id) => request(`/users/${id}`, { method: 'DELETE' }),
  uploadFace: (id, file) => {
    const formData = new FormData();
    formData.append('image', file);
    return fetch(`${API_BASE_URL}/users/${id}/face`, {
      method: 'POST',
      body: formData,
    }).then(async res => {
      if (!res.ok) {
         const message = await res.text();
         throw new Error(message || 'Failed to upload face');
      }
      return res.json();
    });
  },
  getAttendance: (params) => {
    const searchParams = new URLSearchParams(params);
    return request(`/attendance?${searchParams}`);
  },
  getTodayAttendance: () => request('/attendance/today'),
  getAttendanceStats: () => request('/reports/metrics'),
  getAssistanceRequests: () => request('/assistance'),
  updateAssistanceStatus: (id, status) => request(`/assistance/${id}/status`, {
    method: 'PATCH',
    body: JSON.stringify({ status })
  }),
  getSettings: () => request('/settings'),
  updateSettings: (settings) => request('/settings', {
    method: 'PATCH',
    body: JSON.stringify(settings),
  }),
  downloadReport: () => request('/reports/attendance.xlsx'),
  startRfidEnrollment: () => request('/rfid-enrollment/start', { method: 'POST' }),
  getRfidEnrollment: () => request('/rfid-enrollment'),
  cancelRfidEnrollment: () => request('/rfid-enrollment/cancel', { method: 'POST' }),
  sendNotification: (message) => request('/notifications', {
    method: 'POST',
    body: JSON.stringify({ message })
  })
};
