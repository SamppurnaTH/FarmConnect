import apiClient from './client';
import type {
  FarmerProfile, FarmerRegistrationRequest,
  FarmerDocument, VerificationStatus, FarmerStatus,
  FarmerPageResponse,
} from '../types';

export const farmersApi = {
  // ── Registration (public — no token needed) ───────────────────────────────
  register: (data: FarmerRegistrationRequest) =>
    apiClient.post<string>('/farmers', data).then((r) => r.data),

  // ── Own profile (Farmer role) ─────────────────────────────────────────────
  /**
   * GET /farmers/me
   * Returns the farmer profile for the currently logged-in user.
   * Uses the JWT to identify the user — no ID needed in the URL.
   */
  getMyProfile: () =>
    apiClient.get<FarmerProfile>('/farmers/me').then((r) => r.data),

  // ── Profile by ID (Market Officer / Admin) ────────────────────────────────
  getProfile: (id: string) =>
    apiClient.get<FarmerProfile>(`/farmers/${id}`).then((r) => r.data),

  updateProfile: (id: string, data: Partial<Pick<FarmerRegistrationRequest, 'name' | 'address' | 'contactInfo' | 'landDetails'>>) =>
    apiClient.put<FarmerProfile>(`/farmers/${id}`, data).then((r) => r.data),

  updateStatus: (id: string, status: FarmerStatus) =>
    apiClient.put<void>(`/farmers/${id}/status`, { status }),

  // ── Documents ─────────────────────────────────────────────────────────────
  /**
   * POST /farmers/{id}/documents/upload  (multipart/form-data)
   * Sends the actual file bytes to the backend for local-disk storage.
   * Returns the new document UUID.
   */
  uploadDocument: (id: string, documentType: string, file: File) => {
    const formData = new FormData();
    formData.append('documentType', documentType);
    formData.append('file', file);
    return apiClient
      .post<string>(`/farmers/${id}/documents/upload`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      .then((r) => r.data);
  },

  /**
   * Returns the URL for downloading a document file.
   * Used to render a download/preview link in the UI.
   */
  getDocumentFileUrl: (farmerId: string, docId: string): string =>
    `/api/farmers/${farmerId}/documents/${docId}/file`,

  verifyDocument: (id: string, docId: string, status: VerificationStatus, rejectionReason?: string) =>
    apiClient.put<void>(`/farmers/${id}/documents/${docId}/verify`, { status, rejectionReason }),

  getDocuments: (id: string) =>
    apiClient.get<FarmerDocument[]>(`/farmers/${id}/documents`).then((r) => r.data),

  getMyDocuments: () =>
    apiClient.get<FarmerDocument[]>('/farmers/me/documents').then((r) => r.data),

  // ── List / count (Market Officer / Admin) ─────────────────────────────────
  /**
   * GET /farmers — full list (no pagination, backward-compatible)
   */
  listAll: (status?: string) =>
    apiClient.get<FarmerProfile[]>('/farmers', { params: { status } }).then((r) => r.data),

  /**
   * GET /farmers?page=&size=&status=&search=
   * Paginated + searchable list for Market Officer management page.
   */
  listPaged: (params: { status?: string; search?: string; page: number; size: number }) =>
    apiClient
      .get<FarmerPageResponse>('/farmers', { params: { ...params } })
      .then((r) => r.data),

  count: (status?: string) =>
    apiClient.get<number>('/farmers/count', { params: { status } }).then((r) => r.data),
};
