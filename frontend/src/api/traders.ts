import apiClient from './client';
import type { TraderProfile, TraderRegistrationRequest, UpdateTraderRequest } from '../types';

export const tradersApi = {
  // ── Registration (public) ─────────────────────────────────────────────────
  register: (data: TraderRegistrationRequest) =>
    apiClient.post<string>('/traders', data).then((r) => r.data),

  // ── Own profile (Trader role) ─────────────────────────────────────────────
  /**
   * GET /traders/me
   * Returns the trader profile for the currently logged-in user.
   * Uses the JWT to identify the user — no ID needed in the URL.
   */
  getMyProfile: () =>
    apiClient.get<TraderProfile>('/traders/me').then((r) => r.data),

  // ── Profile by ID ─────────────────────────────────────────────────────────
  getProfile: (id: string) =>
    apiClient.get<TraderProfile>(`/traders/${id}`).then((r) => r.data),

  updateProfile: (id: string, data: UpdateTraderRequest) =>
    apiClient.put<TraderProfile>(`/traders/${id}`, data).then((r) => r.data),
};
