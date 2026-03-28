import apiClient from './client';
import type { Notification, Page } from '../types';

export const notificationsApi = {
  getMyNotifications: (page = 0, size = 50) =>
    apiClient.get<Page<Notification>>('/notifications/me', { params: { page, size } }).then((r) => r.data),

  markRead: (id: string) =>
    apiClient.put<void>(`/notifications/${id}/read`),
};
