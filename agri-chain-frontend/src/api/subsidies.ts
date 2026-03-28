import apiClient from './client';
import type {
  SubsidyProgram, CreateProgramRequest,
  Disbursement, CreateDisbursementRequest,
} from '../types';

export const subsidiesApi = {
  // Programs
  getPrograms: () =>
    apiClient.get<SubsidyProgram[]>('/subsidies/programs').then((r) => r.data),

  getProgram: (id: string) =>
    apiClient.get<SubsidyProgram>(`/subsidies/programs/${id}`).then((r) => r.data),

  createProgram: (data: CreateProgramRequest) =>
    apiClient.post<string>('/subsidies/programs', data).then((r) => r.data),

  activateProgram: (id: string) =>
    apiClient.put<void>(`/subsidies/programs/${id}/activate`),

  closeProgram: (id: string) =>
    apiClient.put<void>(`/subsidies/programs/${id}/close`),

  // Disbursements
  getDisbursements: () =>
    apiClient.get<Disbursement[]>('/subsidies/disbursements').then((r) => r.data),

  createDisbursement: (programId: string, data: CreateDisbursementRequest) =>
    apiClient.post<string>(`/subsidies/programs/${programId}/disbursements`, data).then((r) => r.data),

  approveDisbursement: (id: string) =>
    apiClient.put<void>(`/subsidies/disbursements/${id}/approve`),

  getTotalDisbursed: () =>
    apiClient.get<number>('/subsidies/total-disbursed').then((r) => r.data),
};
