import { create } from 'zustand';
import { authService } from '../services/auth.service';
import type { UserProfile } from '../services/auth.service';

interface AuthState {
  user: UserProfile | null;
  token: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  error: string | null;
}

interface AuthActions {
  login: (username: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  loadUser: () => Promise<void>;
  clearError: () => void;
}

type AuthStore = AuthState & AuthActions;

/**
 * Authentication Store
 * Manages authentication state and user session
 */
export const useAuthStore = create<AuthStore>((set, get) => ({
  // State
  user: null,
  token: null,
  isAuthenticated: false,
  isLoading: false,
  error: null,

  // Actions
  login: async (username: string, password: string) => {
    set({ isLoading: true, error: null });
    
    try {
      // 1. Login to Keycloak
      const tokens = await authService.login({ username, password });
      
      // 2. Store tokens
      authService.storeTokens(tokens);
      
      // 3. Get user profile from User Service
      const userProfile = await authService.getUserProfile(tokens.access_token);
      
      // 4. Update state
      set({
        user: userProfile,
        token: tokens.access_token,
        isAuthenticated: true,
        isLoading: false,
        error: null,
      });
    } catch (error: any) {
      console.error('Login failed:', error);
      
      let errorMessage = 'Login failed. Please check your credentials.';
      
      if (error.response?.status === 401) {
        errorMessage = 'Invalid username or password.';
      } else if (error.response?.status === 403) {
        errorMessage = 'Access forbidden.';
      } else if (error.code === 'ERR_NETWORK') {
        errorMessage = 'Network error. Please check your connection.';
      }
      
      set({
        user: null,
        token: null,
        isAuthenticated: false,
        isLoading: false,
        error: errorMessage,
      });
      
      throw error;
    }
  },

  logout: async () => {
    const refreshToken = authService.getRefreshToken();
    
    // Logout from Keycloak
    if (refreshToken) {
      await authService.logout(refreshToken);
    }
    
    // Clear local state
    authService.clearTokens();
    set({
      user: null,
      token: null,
      isAuthenticated: false,
      error: null,
    });
  },

  loadUser: async () => {
    const token = authService.getAccessToken();
    
    if (!token) {
      set({ isAuthenticated: false });
      return;
    }
    
    // Check if token is expired
    if (authService.isTokenExpired(token)) {
      const refreshToken = authService.getRefreshToken();
      
      if (refreshToken) {
        try {
          // Try to refresh token
          const tokens = await authService.refreshToken(refreshToken);
          authService.storeTokens(tokens);
          
          // Get user profile with new token
          const userProfile = await authService.getUserProfile(tokens.access_token);
          
          set({
            user: userProfile,
            token: tokens.access_token,
            isAuthenticated: true,
          });
          return;
        } catch (error) {
          console.error('Token refresh failed:', error);
          // Fall through to logout
        }
      }
      
      // Token expired and refresh failed - logout
      await get().logout();
      return;
    }
    
    // Token is valid - load user profile
    try {
      set({ isLoading: true });
      const userProfile = await authService.getUserProfile(token);
      
      set({
        user: userProfile,
        token,
        isAuthenticated: true,
        isLoading: false,
      });
    } catch (error) {
      console.error('Failed to load user profile:', error);
      await get().logout();
    }
  },

  clearError: () => set({ error: null }),
}));

export default useAuthStore;

