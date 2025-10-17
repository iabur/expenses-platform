import { create } from 'zustand';

/**
 * Authentication store - placeholder for Day 2
 * Will be properly implemented with Keycloak integration
 */

interface User {
  id: string;
  name: string;
  email: string;
}

interface AuthStore {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  logout: () => void;
}

export const useAuthStore = create<AuthStore>((set) => ({
  user: null,
  token: null,
  isAuthenticated: false,
  logout: () => {
    localStorage.removeItem('access_token');
    localStorage.removeItem('refresh_token');
    set({ user: null, token: null, isAuthenticated: false });
    window.location.href = '/login';
  },
}));


