import axios from 'axios';
import API_CONFIG from '../config/api.config';

export interface LoginRequest {
  username: string;
  password: string;
}

export interface TokenResponse {
  access_token: string;
  refresh_token: string;
  expires_in: number;
  refresh_expires_in: number;
  token_type: string;
}

export interface UserProfile {
  id: string;
  keycloakUserId: string;
  email: string;
  firstName: string;
  lastName: string;
  displayName: string;
  defaultCurrency: string;
  locale: string;
  timezone: string;
  avatarUrl: string | null;
  createdAt: string;
  updatedAt: string;
}

/**
 * Authentication Service
 * Handles Keycloak authentication and token management
 */
class AuthService {
  private readonly keycloakTokenUrl: string;

  constructor() {
    this.keycloakTokenUrl = `${API_CONFIG.KEYCLOAK.URL}/realms/${API_CONFIG.KEYCLOAK.REALM}/protocol/openid-connect/token`;
  }

  /**
   * Login with username and password
   */
  async login(credentials: LoginRequest): Promise<TokenResponse> {
    const response = await axios.post<TokenResponse>(
      this.keycloakTokenUrl,
      new URLSearchParams({
        grant_type: 'password',
        client_id: API_CONFIG.KEYCLOAK.CLIENT_ID,
        username: credentials.username,
        password: credentials.password,
      }),
      {
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded',
        },
      }
    );

    return response.data;
  }

  /**
   * Refresh access token
   */
  async refreshToken(refreshToken: string): Promise<TokenResponse> {
    const response = await axios.post<TokenResponse>(
      this.keycloakTokenUrl,
      new URLSearchParams({
        grant_type: 'refresh_token',
        client_id: API_CONFIG.KEYCLOAK.CLIENT_ID,
        refresh_token: refreshToken,
      }),
      {
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded',
        },
      }
    );

    return response.data;
  }

  /**
   * Logout (revoke tokens)
   */
  async logout(refreshToken: string): Promise<void> {
    try {
      await axios.post(
        `${API_CONFIG.KEYCLOAK.URL}/realms/${API_CONFIG.KEYCLOAK.REALM}/protocol/openid-connect/logout`,
        new URLSearchParams({
          client_id: API_CONFIG.KEYCLOAK.CLIENT_ID,
          refresh_token: refreshToken,
        }),
        {
          headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
          },
        }
      );
    } catch (error) {
      console.error('Logout error:', error);
      // Continue with local logout even if server logout fails
    }
  }

  /**
   * Get current user profile from User Service
   */
  async getUserProfile(token: string): Promise<UserProfile> {
    const response = await axios.get<UserProfile>(
      `${API_CONFIG.USER_SERVICE}/user/me`,
      {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      }
    );

    return response.data;
  }

  /**
   * Check if token is expired
   */
  isTokenExpired(token: string): boolean {
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      const expirationTime = payload.exp * 1000; // Convert to milliseconds
      const currentTime = Date.now();
      
      // Check if token expires within buffer time
      return currentTime >= expirationTime - API_CONFIG.TOKEN_REFRESH_BUFFER;
    } catch (error) {
      console.error('Error checking token expiration:', error);
      return true;
    }
  }

  /**
   * Store tokens in localStorage
   */
  storeTokens(tokens: TokenResponse): void {
    localStorage.setItem('access_token', tokens.access_token);
    localStorage.setItem('refresh_token', tokens.refresh_token);
    localStorage.setItem('token_expires_at', String(Date.now() + tokens.expires_in * 1000));
  }

  /**
   * Clear tokens from localStorage
   */
  clearTokens(): void {
    localStorage.removeItem('access_token');
    localStorage.removeItem('refresh_token');
    localStorage.removeItem('token_expires_at');
  }

  /**
   * Get stored access token
   */
  getAccessToken(): string | null {
    return localStorage.getItem('access_token');
  }

  /**
   * Get stored refresh token
   */
  getRefreshToken(): string | null {
    return localStorage.getItem('refresh_token');
  }
}

export const authService = new AuthService();
export default authService;

