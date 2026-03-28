import React, { useState, useRef, useEffect } from 'react';
import { Bell } from 'lucide-react';
import { useNotificationStore } from '../stores/notificationStore';
import { useAuthStore } from '../stores/authStore';
import { usePolling } from '../hooks/usePolling';
import { useToast } from './ToastProvider';

/**
 * NotificationPanel — Requirement 11.1-11.5
 * Bell icon with unread count badge, dropdown of recent notifications,
 * click-to-mark-read, and 30-second polling with toast on new items.
 */
export const NotificationPanel: React.FC = () => {
  const { notifications, unreadCount, fetchNotifications, markRead } = useNotificationStore();
  const { token } = useAuthStore();
  const { showToast } = useToast();
  const [open, setOpen] = useState(false);
  const prevIdsRef = useRef<Set<string>>(new Set());
  const panelRef = useRef<HTMLDivElement>(null);

  // 30-second polling while authenticated
  usePolling(
    async () => {
      if (!token) return;
      const prevIds = prevIdsRef.current;
      await fetchNotifications();

      // Detect new unread notifications for toast
      const newUnread = useNotificationStore
        .getState()
        .notifications.filter((n) => !prevIds.has(n.id) && n.status !== 'Read');

      newUnread.forEach((n) => showToast(n.message, 'info'));
      prevIdsRef.current = new Set(
        useNotificationStore.getState().notifications.map((n) => n.id)
      );
    },
    30_000,
    !!token,
  );

  // Close on click outside
  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (panelRef.current && !panelRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  return (
    <div ref={panelRef} className="relative">
      <button
        onClick={() => setOpen((o) => !o)}
        aria-label={`Notifications — ${unreadCount} unread`}
        aria-expanded={open}
        aria-haspopup="true"
        className="relative p-2 rounded-full text-gray-600 hover:bg-gray-100 focus:outline-none focus:ring-2 focus:ring-primary-500"
      >
        <Bell className="w-5 h-5" aria-hidden="true" />
        {unreadCount > 0 && (
          <span className="absolute -top-1 -right-1 w-5 h-5 bg-red-500 text-white text-xs rounded-full flex items-center justify-center font-bold">
            {unreadCount > 99 ? '99+' : unreadCount}
          </span>
        )}
      </button>

      {open && (
        <div
          role="menu"
          className="absolute right-0 top-10 w-80 bg-white rounded-xl shadow-xl border border-gray-200 z-50 max-h-[480px] overflow-y-auto"
        >
          <div className="px-4 py-3 border-b border-gray-100 flex items-center justify-between">
            <h3 className="font-semibold text-gray-900">Notifications</h3>
            {unreadCount > 0 && (
              <span className="text-xs text-gray-500">{unreadCount} unread</span>
            )}
          </div>

          {notifications.length === 0 ? (
            <div className="px-4 py-8 text-center text-gray-500 text-sm">
              No notifications yet
            </div>
          ) : (
            <ul>
              {notifications.slice(0, 50).map((n) => (
                <li key={n.id}>
                  <button
                    role="menuitem"
                    onClick={() => markRead(n.id)}
                    className={`w-full text-left px-4 py-3 hover:bg-gray-50 transition-colors border-b border-gray-50 last:border-0 ${
                      n.status !== 'Read' ? 'bg-blue-50' : ''
                    }`}
                    aria-label={`${n.status !== 'Read' ? 'Unread: ' : ''}${n.message}`}
                  >
                    <div className="flex items-start gap-2">
                      {n.status !== 'Read' && (
                        <span className="w-2 h-2 bg-blue-500 rounded-full mt-1.5 shrink-0" aria-hidden="true" />
                      )}
                      <div className="flex-1 min-w-0">
                        <p className="text-sm text-gray-800 line-clamp-2">{n.message}</p>
                        <p className="text-xs text-gray-400 mt-0.5">
                          {new Date(n.createdAt).toLocaleString()}
                        </p>
                      </div>
                    </div>
                  </button>
                </li>
              ))}
            </ul>
          )}

          <a
            href="/notifications"
            className="block text-center text-sm text-primary-600 hover:text-primary-700 py-3 border-t border-gray-100 font-medium"
          >
            View all notifications
          </a>
        </div>
      )}
    </div>
  );
};
