const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

export const apiClient = {
  async getUsers() {
    const res = await fetch(`${API_BASE_URL}/users`);
    if (!res.ok) throw new Error('Failed to fetch users');
    return res.json();
  },
  async getUserByRfid(rfidUid) {
    const res = await fetch(`${API_BASE_URL}/users/rfid/${rfidUid}`);
    if (!res.ok) throw new Error('Failed to fetch user by RFID');
    return res.json();
  },
  async createUser(user) {
    const res = await fetch(`${API_BASE_URL}/users`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(user),
    });
    if (!res.ok) throw new Error('Failed to create user');
    return res.json();
  },
  async getAttendanceRecords() {
    const res = await fetch(`${API_BASE_URL}/attendance`);
    if (!res.ok) throw new Error('Failed to fetch attendance records');
    return res.json();
  },
  async recordAttendance(data) {
    const params = new URLSearchParams(data);
    const res = await fetch(`${API_BASE_URL}/attendance?${params}`, {
      method: 'POST',
    });
    if (!res.ok) throw new Error('Failed to record attendance');
    return res.json();
  },
};
