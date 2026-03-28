import axios from 'axios';
import { useAuthStore } from '../stores/authStore';

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30000,
});

// ─── Request interceptor: attach Bearer token ─────────────────────────────────
apiClient.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// ─── Response interceptor: handle 401 / 403 globally ─────────────────────────
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;

    if (status === 401) {
      // Clear session and redirect to login
      useAuthStore.getState().clearSession();
      window.location.href = '/login?reason=unauthorized';
    }

    if (status === 403) {
      // Discard response body — surface access-denied in UI via error propagation
      error.isAccessDenied = true;
    }

    return Promise.reject(error);
  }
);

export default apiClient;
