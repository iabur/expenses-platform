import { useEffect } from 'react';
import { useAuthStore } from '../stores/auth.store';

/**
 * Authentication hook
 * Provides auth state and methods, loads user on mount
 */
export function useAuth() {
  const store = useAuthStore();

  // Load user on mount
  useEffect(() => {
    store.loadUser();
  }, []);

  return {
    user: store.user,
    token: store.token,
    isAuthenticated: store.isAuthenticated,
    isLoading: store.isLoading,
    error: store.error,
    login: store.login,
    logout: store.logout,
    clearError: store.clearError,
  };
}

export default useAuth;

