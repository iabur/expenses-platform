import axios, { AxiosError } from 'axios';
import type { AxiosInstance, InternalAxiosRequestConfig } from 'axios';

/**
 * Custom Axios instance with base configuration
 * Handles authentication, error handling, and request/response interceptors
 */

// Create Axios instance
const axiosInstance: AxiosInstance = axios.create({
  timeout: parseInt(import.meta.env.VITE_API_TIMEOUT || '10000'),
  headers: {
    'Content-Type': 'application/json',
  },
});

/**
 * Request interceptor - adds JWT token to requests
 */
axiosInstance.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // Get token from localStorage
    const token = localStorage.getItem('access_token');
    
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    
    // Log request in development
    if (import.meta.env.DEV) {
      console.log(`[API Request] ${config.method?.toUpperCase()} ${config.url}`);
    }
    
    return config;
  },
  (error) => {
    console.error('[API Request Error]', error);
    return Promise.reject(error);
  }
);

/**
 * Response interceptor - handles errors and token refresh
 */
axiosInstance.interceptors.response.use(
  (response) => {
    // Log response in development
    if (import.meta.env.DEV) {
      console.log(`[API Response] ${response.config.method?.toUpperCase()} ${response.config.url} - ${response.status}`);
    }
    return response;
  },
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };
    
    // Handle 401 Unauthorized - token expired
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      
      try {
        // Try to refresh token
        const refreshToken = localStorage.getItem('refresh_token');
        if (refreshToken) {
          const response = await axios.post(
            `${import.meta.env.VITE_KEYCLOAK_URL}/realms/${import.meta.env.VITE_KEYCLOAK_REALM}/protocol/openid-connect/token`,
            new URLSearchParams({
              grant_type: 'refresh_token',
              client_id: import.meta.env.VITE_KEYCLOAK_CLIENT_ID,
              refresh_token: refreshToken,
            }),
            {
              headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
              },
            }
          );
          
          const { access_token, refresh_token: newRefreshToken } = response.data;
          
          // Store new tokens
          localStorage.setItem('access_token', access_token);
          if (newRefreshToken) {
            localStorage.setItem('refresh_token', newRefreshToken);
          }
          
          // Retry original request with new token
          if (originalRequest.headers) {
            originalRequest.headers.Authorization = `Bearer ${access_token}`;
          }
          return axiosInstance(originalRequest);
        }
      } catch (refreshError) {
        // Refresh failed - redirect to login
        console.error('[Token Refresh Failed]', refreshError);
        localStorage.removeItem('access_token');
        localStorage.removeItem('refresh_token');
        window.location.href = '/login';
        return Promise.reject(refreshError);
      }
    }
    
    // Handle 403 Forbidden
    if (error.response?.status === 403) {
      console.error('[Forbidden] You do not have permission to access this resource');
    }
    
    // Log errors in development
    if (import.meta.env.DEV) {
      console.error('[API Error]', {
        url: error.config?.url,
        method: error.config?.method,
        status: error.response?.status,
        message: error.message,
        data: error.response?.data,
      });
    }
    
    return Promise.reject(error);
  }
);

export default axiosInstance;

/**
 * Helper to create service-specific axios instance
 */
export const createServiceClient = (baseURL: string): AxiosInstance => {
  const instance = axios.create({
    baseURL,
    timeout: parseInt(import.meta.env.VITE_API_TIMEOUT || '10000'),
    headers: {
      'Content-Type': 'application/json',
    },
  });
  
  // Copy interceptors from main instance
  instance.interceptors.request = axiosInstance.interceptors.request;
  instance.interceptors.response = axiosInstance.interceptors.response;
  
  return instance;
};

