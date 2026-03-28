import apiClient from './client';
import type {
  ComplianceRecord, CreateComplianceRecordRequest,
  Audit, AuditLog, Page,
} from '../types';

export interface ComplianceFilters {
  entityType?: string;
  entityId?: string;
}

export interface AuditLogFilters {
  startDate?: string;
  endDate?: string;
  actionType?: string;
  resourceType?: string;
  page?: number;
  size?: number;
}

export const complianceApi = {
  // Compliance Records
  getRecords: (filters: ComplianceFilters = {}) =>
    apiClient.get<ComplianceRecord[]>('/compliance/records', { params: filters }).then((r) => r.data),

  createRecord: (data: CreateComplianceRecordRequest) =>
    apiClient.post<string>('/compliance/records', data).then((r) => r.data),

  // Audits
  getAudits: () =>
    apiClient.get<Audit[]>('/compliance/audits').then((r) => r.data),

  createAudit: (scope: string) =>
    apiClient.post<string>('/compliance/audits', { scope }).then((r) => r.data),

  submitFindings: (id: string, findings: string) =>
    apiClient.put<void>(`/compliance/audits/${id}/findings`, { findings }),

  exportAuditPdf: (id: string, signal?: AbortSignal) =>
    apiClient.get<Blob>(`/compliance/audits/${id}/export`, {
      responseType: 'blob',
      signal,
    }).then((r) => r.data),

  // Audit Log
  getAuditLog: (filters: AuditLogFilters = {}) =>
    apiClient.get<Page<AuditLog>>('/identity/audit-log', { params: filters }).then((r) => r.data),
};
