import React, { useEffect, useState } from 'react';
import { NavMenu } from '../../components/NavMenu';
import { LoadingSpinner } from '../../components/LoadingSpinner';
import { notificationsApi } from '../../api/notifications';
import { useNotificationStore } from '../../stores/notificationStore';
import type { Notification } from '../../types';
import { Bell } from 'lucide-react';

const NotificationsPage: React.FC = () => {
  const { markRead } = useNotificationStore();
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);

  useEffect(() => {
    notificationsApi.getMyNotifications(page, 20).then((p) => {
      setNotifications(p.content);
      setTotalPages(p.totalPages);
    }).finally(() => setLoading(false));
  }, [page]);

  const handleMarkRead = async (id: string) => {
    await markRead(id);
    setNotifications((prev) => prev.map((n) => n.id === id ? { ...n, status: 'Read' as const } : n));
  };

  const sc: Record<string, string> = {
    Read: 'badge-settled', Delivered: 'badge-active', Pending: 'badge-pending', Failed: 'badge-failed',
  };

  if (loading) return <><NavMenu /><LoadingSpinner /></>;

  return (
    <div className="min-h-screen bg-gray-50">
      <NavMenu />
      <main className="max-w-3xl mx-auto px-4 py-8">
        <h1 className="text-2xl font-bold text-gray-900 mb-6 flex items-center gap-3">
          <Bell className="w-6 h-6" />Notifications
        </h1>

        {notifications.length === 0 ? (
          <div className="card text-center py-12 text-gray-400">No notifications yet.</div>
        ) : (
          <div className="space-y-3">
            {notifications.map((n) => (
              <button
                key={n.id}
                onClick={() => handleMarkRead(n.id)}
                className={`w-full text-left card hover:shadow-md transition-shadow ${n.status !== 'Read' ? 'border-blue-200 bg-blue-50' : ''}`}
                aria-label={`${n.status !== 'Read' ? 'Unread: ' : ''}${n.message}`}
              >
                <div className="flex items-start justify-between gap-3">
                  <p className="text-sm text-gray-800 flex-1">{n.message}</p>
                  <span className={sc[n.status] ?? 'badge-pending'}>{n.status}</span>
                </div>
                <p className="text-xs text-gray-400 mt-2">{new Date(n.createdAt).toLocaleString()} · {n.channel}</p>
              </button>
            ))}
          </div>
        )}

        <div className="flex items-center justify-between mt-6">
          <button onClick={() => setPage((p) => Math.max(0, p - 1))} disabled={page === 0} className="btn-secondary">← Prev</button>
          <span className="text-sm text-gray-500">Page {page + 1} of {totalPages}</span>
          <button onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))} disabled={page >= totalPages - 1} className="btn-secondary">Next →</button>
        </div>
      </main>
    </div>
  );
};

export default NotificationsPage;
