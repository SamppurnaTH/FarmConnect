import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { authApi } from '../api/auth';
import type { UserRole, LoginRequest } from '../types';

interface AuthState {
  token: string | null;
  role: UserRole | null;
  /** Identity-service user ID (from JWT). Used for API calls. */
  userId: string | null;
  refreshTimer: ReturnType<typeof setTimeout> | null;

  login: (credentials: LoginRequest) => Promise<void>;
  logout: () => Promise<void>;
  refresh: () => Promise<void>;
  clearSession: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      token: null,
      role: null,
      userId: null,
      refreshTimer: null,

      login: async (credentials) => {
        const response = await authApi.login(credentials);
        const { token, role, userId, expiresAt } = response;

        // Schedule silent refresh 5 minutes before expiry
        const expiryMs = new Date(expiresAt).getTime() - Date.now();
        const refreshIn = Math.max(expiryMs - 5 * 60 * 1000, 0);

        const timer = setTimeout(() => {
          get().refresh();
        }, refreshIn);

        set({ token, role, userId, refreshTimer: timer });
      },

      logout: async () => {
        try {
          await authApi.logout();
        } catch {
          // Ignore errors on logout — session is cleared regardless
        }
        get().clearSession();
        window.location.href = '/login';
      },

      refresh: async () => {
        try {
          const response = await authApi.refresh();
          const { token, expiresAt } = response;

          const expiryMs = new Date(expiresAt).getTime() - Date.now();
          const refreshIn = Math.max(expiryMs - 5 * 60 * 1000, 0);

          const { refreshTimer } = get();
          if (refreshTimer) clearTimeout(refreshTimer);

          const timer = setTimeout(() => {
            get().refresh();
          }, refreshIn);

          set({ token, refreshTimer: timer });
        } catch {
          // Refresh failed — clear session and redirect
          get().clearSession();
          window.location.href = '/login?reason=session_expired';
        }
      },

      clearSession: () => {
        const { refreshTimer } = get();
        if (refreshTimer) clearTimeout(refreshTimer);
        set({ token: null, role: null, userId: null, refreshTimer: null });
      },
    }),
    {
      name: 'agrichain-auth',
      // Only persist the token, role, and userId — not the timer (timers can't be serialised)
      partialize: (state) => ({
        token:  state.token,
        role:   state.role,
        userId: state.userId,
      }),
    }
  )
);
