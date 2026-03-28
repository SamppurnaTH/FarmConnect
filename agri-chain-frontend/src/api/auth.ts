import apiClient from './client';
import type { LoginRequest, LoginResponse } from '../types';

export const authApi = {
  login: (data: LoginRequest) =>
    apiClient.post<LoginResponse>('/auth/login', data).then((r) => r.data),

  logout: () =>
    apiClient.post('/auth/logout').then((r) => r.data),

  refresh: () =>
    apiClient.post<LoginResponse>('/auth/refresh').then((r) => r.data),
};
