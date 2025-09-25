import axios, { AxiosInstance } from 'axios';
import { DEV_CONFIG, simulateDelay } from '@/config/development';
import { 
  User, 
  UserProfileUpdateRequest, 
  UserPreferences,
  UserPreferencesUpdateRequest,
  Group, 
  CreateGroupRequest, 
  UpdateGroupRequest,
  Expense,
  CreateExpenseRequest,
  UpdateExpenseRequest,
  ExpenseSummary,
  ExpenseStatistics,
  Settlement,
  CreateSettlementRequest,
  ConfirmSettlementRequest,
  GroupBalance,
  ExchangeRate,
  CurrencyConversionRequest,
  CurrencyConversionResponse,
  PaginatedResponse,
  ApiError,
  LoginRequest,
  LoginResponse,
  AuthUser
} from '@/types/api';

class ApiClient {
  private client: AxiosInstance;
  private baseURL: string;

  constructor() {
    this.baseURL = (import.meta as any).env?.VITE_API_BASE_URL || 'http://localhost:8080';
    
    this.client = axios.create({
      baseURL: this.baseURL,
      timeout: 10000,
      headers: {
        'Content-Type': 'application/json',
      },
    });

    this.setupInterceptors();
  }

  private setupInterceptors() {
    // Request interceptor to add auth token
    this.client.interceptors.request.use(
      (config) => {
        const token = this.getAuthToken();
        if (token) {
          config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
      },
      (error) => Promise.reject(error)
    );

    // Response interceptor for error handling
    this.client.interceptors.response.use(
      (response) => response,
      (error) => {
        if (error.response?.status === 401) {
          this.clearAuthToken();
          window.location.href = '/login';
        }
        return Promise.reject(this.handleError(error));
      }
    );
  }

  private getAuthToken(): string | null {
    return localStorage.getItem('auth_token');
  }

  private setAuthToken(token: string): void {
    localStorage.setItem('auth_token', token);
  }

  private clearAuthToken(): void {
    localStorage.removeItem('auth_token');
  }

  private handleError(error: any): ApiError {
    if (error.response?.data) {
      return error.response.data;
    }
    return {
      timestamp: new Date().toISOString(),
      status: error.response?.status || 500,
      error: error.response?.statusText || 'Unknown Error',
      message: error.message || 'An unexpected error occurred',
      path: error.config?.url || '',
    };
  }

  // Authentication methods
  async login(credentials: LoginRequest): Promise<LoginResponse> {
    if (DEV_CONFIG.USE_MOCK_DATA) {
      await simulateDelay();
      
      // For development, accept any credentials
      const mockResponse: LoginResponse = {
        access_token: 'mock-jwt-token',
        refresh_token: 'mock-refresh-token',
        token_type: 'Bearer',
        expires_in: 3600
      };
      
      this.setAuthToken(mockResponse.access_token);
      return mockResponse;
    }
    
    // Real API call would go here
    const response = await this.client.post<LoginResponse>('/auth/login', credentials);
    this.setAuthToken(response.data.access_token);
    return response.data;
  }

  async logout(): Promise<void> {
    this.clearAuthToken();
  }

  async getCurrentUser(): Promise<AuthUser> {
    // Mock user data for development
    const mockUser: AuthUser = {
      sub: 'mock-user-id',
      email: 'alice@example.com',
      name: 'Alice Johnson',
      roles: ['USER'],
      groups: [],
      exp: Date.now() + 3600000,
      iat: Date.now()
    };
    return mockUser;
  }

  // User API methods
  async getUserProfile(): Promise<User> {
    // Mock user profile for development
    const mockProfile: User = {
      id: 'mock-user-id',
      email: 'alice@example.com',
      firstName: 'Alice',
      lastName: 'Johnson',
      displayName: 'Alice Johnson',
      defaultCurrency: 'USD',
      locale: 'en-US',
      timezone: 'UTC',
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    };
    return mockProfile;
  }

  async updateUserProfile(profile: UserProfileUpdateRequest): Promise<User> {
    const response = await this.client.put<User>('/api/user/me', profile);
    return response.data;
  }

  async getUserPreferences(): Promise<UserPreferences> {
    const response = await this.client.get<UserPreferences>('/api/user/me/preferences');
    return response.data;
  }

  async updateUserPreferences(preferences: UserPreferencesUpdateRequest): Promise<UserPreferences> {
    const response = await this.client.put<UserPreferences>('/api/user/me/preferences', preferences);
    return response.data;
  }

  async searchUsers(query?: string, page = 0, size = 10): Promise<PaginatedResponse<User>> {
    // Mock users data for development
    const allUsers: User[] = [
      {
        id: 'user-1',
        email: 'alice@example.com',
        firstName: 'Alice',
        lastName: 'Johnson',
        displayName: 'Alice Johnson',
        defaultCurrency: 'USD',
        locale: 'en-US',
        timezone: 'UTC',
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString()
      },
      {
        id: 'user-2',
        email: 'bob@example.com',
        firstName: 'Bob',
        lastName: 'Smith',
        displayName: 'Bob Smith',
        defaultCurrency: 'USD',
        locale: 'en-US',
        timezone: 'UTC',
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString()
      },
      {
        id: 'user-3',
        email: 'charlie@example.com',
        firstName: 'Charlie',
        lastName: 'Brown',
        displayName: 'Charlie Brown',
        defaultCurrency: 'EUR',
        locale: 'en-US',
        timezone: 'UTC',
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString()
      }
    ];
    
    const filteredUsers = query 
      ? allUsers.filter(user => 
          user.displayName?.toLowerCase().includes(query.toLowerCase()) ||
          user.email.toLowerCase().includes(query.toLowerCase())
        )
      : allUsers;
    
    return {
      content: filteredUsers,
      pageable: {
        pageNumber: page,
        pageSize: size,
        sort: { sorted: false, unsorted: true }
      },
      totalElements: filteredUsers.length,
      totalPages: Math.ceil(filteredUsers.length / size),
      first: page === 0,
      last: page >= Math.ceil(filteredUsers.length / size) - 1,
      size: size,
      number: page
    };
  }

  // Group API methods
  async createGroup(group: CreateGroupRequest): Promise<Group> {
    // Mock group creation for development
    const newGroup: Group = {
      id: `group-${Date.now()}`,
      name: group.name,
      description: group.description,
      currency: group.currency,
      ownerId: 'mock-user-id',
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      memberCount: 1,
      isActive: true,
      members: []
    };
    return newGroup;
  }

  async getGroup(groupId: string): Promise<Group> {
    const response = await this.client.get<Group>(`/api/groups/${groupId}`);
    return response.data;
  }

  async updateGroup(groupId: string, group: UpdateGroupRequest): Promise<Group> {
    const response = await this.client.put<Group>(`/api/groups/${groupId}`, group);
    return response.data;
  }

  async deleteGroup(groupId: string): Promise<void> {
    await this.client.delete(`/api/groups/${groupId}`);
  }

  async getUserGroups(page = 0, size = 20): Promise<PaginatedResponse<Group>> {
    // Mock groups data for development
    const mockGroups: Group[] = [
      {
        id: 'group-1',
        name: 'Weekend Trip',
        description: 'Paris weekend getaway',
        currency: 'EUR',
        ownerId: 'mock-user-id',
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        memberCount: 4,
        isActive: true,
        members: []
      },
      {
        id: 'group-2',
        name: 'Office Lunch',
        description: 'Team lunch expenses',
        currency: 'USD',
        ownerId: 'mock-user-id',
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        memberCount: 8,
        isActive: true,
        members: []
      }
    ];
    
    return {
      content: mockGroups,
      pageable: {
        pageNumber: page,
        pageSize: size,
        sort: { sorted: false, unsorted: true }
      },
      totalElements: mockGroups.length,
      totalPages: 1,
      first: true,
      last: true,
      size: size,
      number: page
    };
  }

  async searchGroups(query?: string, page = 0, size = 20): Promise<PaginatedResponse<Group>> {
    const response = await this.client.get<PaginatedResponse<Group>>('/api/groups/search', {
      params: { q: query, page, size }
    });
    return response.data;
  }

  async getAdminGroups(): Promise<Group[]> {
    const response = await this.client.get<Group[]>('/api/groups/admin');
    return response.data;
  }

  async addGroupMember(groupId: string, userId: string, role: 'MEMBER' | 'ADMIN' = 'MEMBER'): Promise<void> {
    await this.client.post(`/api/groups/${groupId}/members`, { userId, role });
  }

  async removeGroupMember(groupId: string, userId: string): Promise<void> {
    await this.client.delete(`/api/groups/${groupId}/members/${userId}`);
  }

  // Expense API methods
  async createExpense(expense: CreateExpenseRequest): Promise<Expense> {
    const response = await this.client.post<Expense>('/api/expenses', expense);
    return response.data;
  }

  async getExpense(expenseId: string): Promise<Expense> {
    const response = await this.client.get<Expense>(`/api/expenses/${expenseId}`);
    return response.data;
  }

  async updateExpense(expenseId: string, expense: UpdateExpenseRequest): Promise<Expense> {
    const response = await this.client.put<Expense>(`/api/expenses/${expenseId}`, expense);
    return response.data;
  }

  async deleteExpense(expenseId: string): Promise<void> {
    await this.client.delete(`/api/expenses/${expenseId}`);
  }

  async getGroupExpenses(groupId: string, page = 0, size = 20): Promise<PaginatedResponse<ExpenseSummary>> {
    const response = await this.client.get<PaginatedResponse<ExpenseSummary>>(`/api/expenses/group/${groupId}`, {
      params: { page, size }
    });
    return response.data;
  }

  async getMyExpenses(page = 0, size = 20): Promise<PaginatedResponse<ExpenseSummary>> {
    // Mock expenses data for development
    const mockExpenses: ExpenseSummary[] = [
      {
        id: 'expense-1',
        groupId: 'group-1',
        creatorId: 'mock-user-id',
        paidBy: 'mock-user-id',
        currency: 'EUR',
        amount: 45.50,
        occurredAt: new Date().toISOString().split('T')[0],
        note: 'Dinner at restaurant',
        category: 'Food & Dining',
        participantCount: 4,
        createdAt: new Date().toISOString()
      },
      {
        id: 'expense-2',
        groupId: 'group-2',
        creatorId: 'mock-user-id',
        paidBy: 'mock-user-id',
        currency: 'USD',
        amount: 120.00,
        occurredAt: new Date().toISOString().split('T')[0],
        note: 'Team lunch',
        category: 'Food & Dining',
        participantCount: 8,
        createdAt: new Date().toISOString()
      }
    ];
    
    return {
      content: mockExpenses,
      pageable: {
        pageNumber: page,
        pageSize: size,
        sort: { sorted: false, unsorted: true }
      },
      totalElements: mockExpenses.length,
      totalPages: 1,
      first: true,
      last: true,
      size: size,
      number: page
    };
  }

  async getParticipatedExpenses(page = 0, size = 20): Promise<PaginatedResponse<ExpenseSummary>> {
    // Return the same mock data as getMyExpenses for now
    return this.getMyExpenses(page, size);
  }

  async searchExpenses(query: string, groupId?: string, page = 0, size = 20): Promise<PaginatedResponse<ExpenseSummary>> {
    const response = await this.client.get<PaginatedResponse<ExpenseSummary>>('/api/expenses/search', {
      params: { query, groupId, page, size }
    });
    return response.data;
  }

  async getGroupExpenseStatistics(groupId: string): Promise<ExpenseStatistics> {
    const response = await this.client.get<ExpenseStatistics>(`/api/expenses/group/${groupId}/statistics`);
    return response.data;
  }

  // Settlement API methods
  async createSettlement(settlement: CreateSettlementRequest): Promise<Settlement> {
    const response = await this.client.post<Settlement>('/api/settlements/proposals', settlement);
    return response.data;
  }

  async confirmSettlement(settlementId: string, confirmation: ConfirmSettlementRequest): Promise<Settlement> {
    const response = await this.client.post<Settlement>(`/api/settlements/${settlementId}/confirm`, confirmation);
    return response.data;
  }

  async getGroupSettlements(groupId: string): Promise<Settlement[]> {
    const response = await this.client.get<Settlement[]>(`/api/settlements/group/${groupId}`);
    return response.data;
  }

  async getMySettlements(): Promise<Settlement[]> {
    const response = await this.client.get<Settlement[]>('/api/settlements/my');
    return response.data;
  }

  // Split Engine API methods
  async getGroupBalances(groupId: string): Promise<GroupBalance[]> {
    const response = await this.client.get<GroupBalance[]>(`/api/splits/group/${groupId}/balances`);
    return response.data;
  }

  async calculateSplits(expenseId: string): Promise<any> {
    const response = await this.client.post(`/api/splits/calculate`, { expenseId });
    return response.data;
  }

  // FX API methods
  async getExchangeRates(baseCurrency: string, targetCurrency?: string): Promise<ExchangeRate[]> {
    const response = await this.client.get<ExchangeRate[]>('/api/fx/rates', {
      params: { base: baseCurrency, target: targetCurrency }
    });
    return response.data;
  }

  async convertCurrency(request: CurrencyConversionRequest): Promise<CurrencyConversionResponse> {
    const response = await this.client.post<CurrencyConversionResponse>('/api/fx/convert', request);
    return response.data;
  }

  // Utility methods
  async uploadFile(file: File, endpoint: string): Promise<string> {
    const formData = new FormData();
    formData.append('file', file);
    
    const response = await this.client.post<{ url: string }>(endpoint, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    
    return response.data.url;
  }
}

// Create and export a singleton instance
export const apiClient = new ApiClient();
export default apiClient;
