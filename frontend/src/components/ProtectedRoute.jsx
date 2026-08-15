import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

/**
 * Protect nested routes using authentication and role-based access checks.
 * @param {string[]} [allowedRoles] - Roles permitted to access the nested routes.
 * @returns {JSX.Element} The nested route outlet, or a redirect for unauthorized users.
 */
export default function ProtectedRoute({ allowedRoles }) {
  const { token, role } = useAuth();

  if (!token) {
    return <Navigate to="/login" replace />;
  }

  if (allowedRoles && !allowedRoles.includes(role)) {
    return <Navigate to="/" replace />; // Or to a generic unauthorized page
  }

  return <Outlet />;
}
