import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuthStore } from '../stores/authStore';
import { isRoutePermitted } from '../utils/permissions';

interface RouteGuardProps {
  children: React.ReactNode;
  route: string;
}

export const RouteGuard: React.FC<RouteGuardProps> = ({ children, route }) => {
  const { token, role } = useAuthStore();
  const location = useLocation();

  // Not authenticated → redirect to login
  if (!token) {
    return <Navigate to={`/login?next=${encodeURIComponent(location.pathname)}`} replace />;
  }

  // Authenticated but unauthorized for this route → 403
  if (!isRoutePermitted(route, role)) {
    return <Navigate to="/403" replace />;
  }

  return <>{children}</>;
};
