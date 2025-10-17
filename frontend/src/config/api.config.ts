/**
 * API Configuration
 * Central configuration for all API endpoints and settings
 */

export const API_CONFIG = {
  // Base URLs
  GATEWAY: import.meta.env.VITE_API_GATEWAY_URL || 'http://localhost:8080',
  USER_SERVICE: import.meta.env.VITE_USER_SERVICE_URL || 'http://localhost:8081',
  GROUP_SERVICE: import.meta.env.VITE_GROUP_SERVICE_URL || 'http://localhost:8082',
  EXPENSE_SERVICE: import.meta.env.VITE_EXPENSE_SERVICE_URL || 'http://localhost:8083',
  SPLIT_SERVICE: import.meta.env.VITE_SPLIT_SERVICE_URL || 'http://localhost:8084',
  SETTLEMENT_SERVICE: import.meta.env.VITE_SETTLEMENT_SERVICE_URL || 'http://localhost:8085',
  FX_SERVICE: import.meta.env.VITE_FX_SERVICE_URL || 'http://localhost:8086',
  
  // Keycloak
  KEYCLOAK: {
    URL: import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:8090',
    REALM: import.meta.env.VITE_KEYCLOAK_REALM || 'expenses',
    CLIENT_ID: import.meta.env.VITE_KEYCLOAK_CLIENT_ID || 'web',
  },
  
  // Timeouts
  TIMEOUT: parseInt(import.meta.env.VITE_API_TIMEOUT || '10000'),
  TOKEN_REFRESH_BUFFER: parseInt(import.meta.env.VITE_TOKEN_REFRESH_BUFFER || '60000'),
  
  // Polling intervals (milliseconds)
  POLLING: {
    BALANCE_UPDATE: 2000,
    SETTLEMENT_STATUS: 3000,
    MAX_ATTEMPTS: 15,
  },
} as const;

export default API_CONFIG;


