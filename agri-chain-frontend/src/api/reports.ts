import apiClient from './client';
import type { ReportMetadata, CreateReportRequest } from '../types';

export const reportsApi = {
  generateReport: (data: CreateReportRequest) =>
    apiClient.post<string>('/reporting/reports', data).then((r) => r.data),

  exportReport: (id: string, signal?: AbortSignal) =>
    apiClient.get<Blob>(`/reporting/reports/${id}/export`, {
      responseType: 'blob',
      signal,
    }).then((r) => r.data),

  listReports: () =>
    apiClient.get<ReportMetadata[]>('/reporting/reports').then((r) => r.data),
};
