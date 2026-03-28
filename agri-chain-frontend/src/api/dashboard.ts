import apiClient from './client';
import type { DashboardKPIs } from '../types';

export const dashboardApi = {
  getKPIs: () =>
    apiClient.get<DashboardKPIs>('/reports/dashboard').then((r) => r.data),
};
