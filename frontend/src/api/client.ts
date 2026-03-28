import axios from 'axios';
import { useAuthStore } from '../stores/authStore';
import { useToastStore } from '../stores/toastStore';

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30000,
});

// ─── Request interceptor: attach Bearer token + X-User-ID ────────────────────
apiClient.interceptors.request.use((config) => {
  const { token, userId } = useAuthStore.getState();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  if (userId) {
    config.headers['X-User-ID'] = userId;
  }
  return config;
});

// ─── Response interceptor: handle 401 / 403 globally ─────────────────────────
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;
    const showToast = useToastStore.getState().showToast;

    if (status === 401) {
      useAuthStore.getState().clearSession();
      window.location.href = '/login?reason=session_expired';
    } else if (status === 403) {
      showToast('Access Denied: You do not have permission to perform this action.', 'error');
      error.isAccessDenied = true;
    } else {
      const message = error.response?.data?.error || error.message || 'An unexpected error occurred.';
      showToast(message, 'error');
    }

    return Promise.reject(error);
  }
);

export default apiClient;
