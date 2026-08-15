// import { Navigate, Outlet } from 'react-router-dom';
// import { useAuth } from '../contexts/AuthContext';

// export default function ProtectedRoute({ allowedRoles }) {
//   const { token, role } = useAuth();

//   if (!token) {
//     return <Navigate to="/login" replace />;
//   }

//   if (allowedRoles && !allowedRoles.includes(role)) {
//     return <Navigate to="/" replace />; // Or to a generic unauthorized page
//   }

//   return <Outlet />;
// }

import { Outlet } from 'react-router-dom';

export default function ProtectedRoute() {
  return <Outlet />;
}
