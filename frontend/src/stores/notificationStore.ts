import { create } from 'zustand';
import { notificationsApi } from '../api/notifications';
import type { Notification } from '../types';

interface NotificationState {
  notifications: Notification[];
  unreadCount: number;

  fetchNotifications: () => Promise<void>;
  markRead: (id: string) => Promise<void>;
  setNotifications: (notifs: Notification[]) => void;
}

export const useNotificationStore = create<NotificationState>((set, get) => ({
  notifications: [],
  unreadCount: 0,

  fetchNotifications: async () => {
    try {
      const page = await notificationsApi.getMyNotifications(0, 50);
      const notifs = page.content ?? [];
      const unread = notifs.filter((n) => n.status !== 'Read').length;
      set({ notifications: notifs, unreadCount: unread });
    } catch {
      // Silently fail — do not interrupt user session
    }
  },

  markRead: async (id) => {
    try {
      await notificationsApi.markRead(id);
      const updated = get().notifications.map((n) =>
        n.id === id ? { ...n, status: 'Read' as const } : n
      );
      const unread = updated.filter((n) => n.status !== 'Read').length;
      set({ notifications: updated, unreadCount: unread });
    } catch {
      // Silently fail
    }
  },

  setNotifications: (notifs) => {
    const unread = notifs.filter((n) => n.status !== 'Read').length;
    set({ notifications: notifs, unreadCount: unread });
  },
}));
