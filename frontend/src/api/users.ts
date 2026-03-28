import apiClient from './client';
import type { User, UserRole } from '../types';

export const usersApi = {
  listUsers: () =>
    apiClient.get<User[]>('/identity/users').then((r) => r.data),

  assignRole: (userId: string, role: UserRole) =>
    apiClient.put<void>(`/identity/roles/${userId}`, { role }),
};
