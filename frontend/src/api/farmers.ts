import apiClient from './client';
import type {
  FarmerProfile, FarmerRegistrationRequest,
  FarmerDocument, VerificationStatus, FarmerStatus,
} from '../types';

export const farmersApi = {
  register: (data: FarmerRegistrationRequest) =>
    apiClient.post<string>('/farmers', data).then((r) => r.data),

  getProfile: (id: string) =>
    apiClient.get<FarmerProfile>(`/farmers/${id}`).then((r) => r.data),

  updateProfile: (id: string, data: Partial<FarmerRegistrationRequest>) =>
    apiClient.put<void>(`/farmers/${id}`, data),

  updateStatus: (id: string, status: FarmerStatus) =>
    apiClient.put<void>(`/farmers/${id}/status`, { status }),

  uploadDocument: (id: string, formData: FormData) =>
    apiClient.post<string>(`/farmers/${id}/documents`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }).then((r) => r.data),

  verifyDocument: (id: string, docId: string, status: VerificationStatus, rejectionReason?: string) =>
    apiClient.put<void>(`/farmers/${id}/documents/${docId}/verify`, { status, rejectionReason }),

  listAll: (status?: string) =>
    apiClient.get<FarmerProfile[]>('/farmers', { params: { status } }).then((r) => r.data),

  getDocuments: (id: string) =>
    apiClient.get<FarmerDocument[]>(`/farmers/${id}/documents`).then((r) => r.data),

  count: (status?: string) =>
    apiClient.get<number>('/farmers/count', { params: { status } }).then((r) => r.data),
};
