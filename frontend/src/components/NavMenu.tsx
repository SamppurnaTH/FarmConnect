import React from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { LogOut, Sprout } from 'lucide-react';
import { useAuthStore } from '../stores/authStore';
import { getPermittedRoutes, ROUTE_LABELS } from '../utils/permissions';
import { NotificationPanel } from './NotificationPanel';

/**
 * NavMenu — Requirement 2.1, 14.7
 * Renders navigation links filtered by the current user's role.
 * All links have accessible text and ARIA labels.
 */
export const NavMenu: React.FC = () => {
  const { role, logout } = useAuthStore();
  const navigate = useNavigate();

  const permittedRoutes = role ? getPermittedRoutes(role) : [];

  // Exclude '/notifications' from main nav (it's in the bell icon)
  const navRoutes = permittedRoutes.filter((r) => r !== '/notifications');

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  return (
    <header className="bg-white border-b border-gray-200 sticky top-0 z-40">
      <nav
        aria-label="Main navigation"
        className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex items-center justify-between h-16"
      >
        {/* Logo */}
        <NavLink
          to="/dashboard"
          className="flex items-center gap-2 text-primary-700 font-bold text-lg"
          aria-label="Agri-Chain — go to dashboard"
        >
          <Sprout className="w-6 h-6" aria-hidden="true" />
          <span>Agri-Chain</span>
        </NavLink>

        {/* Nav links */}
        <ul className="hidden md:flex items-center gap-1 flex-1 px-8" role="list">
          {navRoutes.map((route) => (
            <li key={route}>
              <NavLink
                to={route}
                aria-label={ROUTE_LABELS[route] ?? route}
                className={({ isActive }) =>
                  `px-3 py-2 rounded-lg text-sm font-medium transition-colors ${
                    isActive
                      ? 'bg-primary-50 text-primary-700'
                      : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900'
                  }`
                }
              >
                {ROUTE_LABELS[route] ?? route}
              </NavLink>
            </li>
          ))}
        </ul>

        {/* Right-side: notifications + role badge + logout */}
        <div className="flex items-center gap-3">
          <NotificationPanel />

          {role && (
            <span className="hidden sm:inline-flex badge-active text-xs">
              {role.replace(/_/g, ' ')}
            </span>
          )}

          <button
            onClick={handleLogout}
            aria-label="Log out"
            className="flex items-center gap-1.5 text-sm text-gray-600 hover:text-red-600 transition-colors px-3 py-2 rounded-lg hover:bg-red-50"
          >
            <LogOut className="w-4 h-4" aria-hidden="true" />
            <span className="hidden sm:inline">Logout</span>
          </button>
        </div>
      </nav>
    </header>
  );
};
