import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { apiClient } from '../api/client';
import { Button, ErrorBanner, Panel } from '../components/ui';

export default function Login() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleLogin = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    
    try {
      const data = await apiClient.login({ username, password });
      login(data);
      navigate('/');
    } catch (err) {
      setError('Đăng nhập thất bại. Kiểm tra lại tài khoản hoặc mật khẩu.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-container" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: '100vh', background: 'var(--bg-base)' }}>
      <Panel className="login-panel" style={{ width: '100%', maxWidth: '400px', padding: '2rem' }}>
        <div style={{ textAlign: 'center', marginBottom: '2rem' }}>
          <h1 style={{ fontSize: '1.5rem', fontWeight: 600 }}>Đăng nhập hệ thống</h1>
          <p className="text-muted" style={{ marginTop: '0.5rem' }}>Quản lý điểm danh thông minh</p>
        </div>
        
        {error && <ErrorBanner message={error} />}
        
        <form onSubmit={handleLogin} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          <div className="form-group flex-col gap-2">
            <label className="text-sm">Tài khoản</label>
            <input 
              type="text" 
              className="input" 
              required
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="Nhập tên đăng nhập"
              id="login-username"
            />
          </div>
          <div className="form-group flex-col gap-2">
            <label className="text-sm">Mật khẩu</label>
            <input 
              type="password" 
              className="input"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Nhập mật khẩu"
              id="login-password"
            />
          </div>
          <Button variant="primary" type="submit" disabled={loading} style={{ marginTop: '1rem', width: '100%' }} id="login-submit">
            {loading ? 'Đang đăng nhập...' : 'Đăng nhập'}
          </Button>
        </form>
      </Panel>
    </div>
  );
}
