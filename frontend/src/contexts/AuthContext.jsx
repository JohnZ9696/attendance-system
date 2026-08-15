import { createContext, useContext, useState, useEffect } from 'react';

const AuthContext = createContext(null);

/**
 * Provides authentication state and actions to descendant components.
 * @param {React.ReactNode} children - The components rendered within the authentication context.
 * @returns {JSX.Element} The authentication context provider.
 */
export function AuthProvider({ children }) {
  const [token, setToken] = useState(localStorage.getItem('token'));
  const [role, setRole] = useState(localStorage.getItem('role'));
  const [user, setUser] = useState(JSON.parse(localStorage.getItem('user') || 'null'));

  const login = (data) => {
    setToken(data.token);
    setRole(data.role);
    setUser(data);
    localStorage.setItem('token', data.token);
    localStorage.setItem('role', data.role);
    localStorage.setItem('user', JSON.stringify(data));
  };

  const logout = () => {
    setToken(null);
    setRole(null);
    setUser(null);
    localStorage.removeItem('token');
    localStorage.removeItem('role');
    localStorage.removeItem('user');
  };

  return (
    <AuthContext.Provider value={{ token, role, user, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

/**
 * Accesses the authentication context.
 * @returns {Object} The authentication state and actions.
 * @throws {Error} If called outside an `AuthProvider`.
 */
export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used within an AuthProvider');
  return context;
}
