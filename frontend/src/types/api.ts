// API Types based on backend DTOs

export interface User {
  id: string;
  email: string;
  firstName?: string;
  lastName?: string;
  displayName?: string;
  defaultCurrency: string;
  locale?: string;
  timezone?: string;
  createdAt: string;
  updatedAt: string;
}

export interface UserProfileUpdateRequest {
  firstName?: string;
  lastName?: string;
  displayName?: string;
  defaultCurrency?: string;
  locale?: string;
  timezone?: string;
}

export interface UserPreferences {
  digestFrequency: 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'NEVER';
  notificationEmail: boolean;
  notificationPush: boolean;
  notificationSms: boolean;
}

export interface UserPreferencesUpdateRequest {
  digestFrequency?: 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'NEVER';
  notificationEmail?: boolean;
  notificationPush?: boolean;
  notificationSms?: boolean;
}

export interface Group {
  id: string;
  name: string;
  description?: string;
  currency: string;
  ownerId: string;
  createdAt: string;
  updatedAt?: string;
  memberCount: number;
  isActive: boolean;
  members?: GroupMember[];
}

export interface GroupMember {
  userId: string;
  role: 'OWNER' | 'ADMIN' | 'MEMBER';
  joinedAt: string;
  user?: User;
}

export interface CreateGroupRequest {
  name: string;
  description?: string;
  currency: string;
}

export interface UpdateGroupRequest {
  name?: string;
  description?: string;
  currency?: string;
}

export interface Expense {
  id: string;
  groupId: string;
  creatorId: string;
  paidBy: string;
  currency: string;
  amountCents: number;
  amount: number;
  occurredAt: string;
  note?: string;
  category: string;
  fxRate?: number;
  fxBaseCurrency?: string;
  createdAt: string;
  updatedAt: string;
  isDeleted: boolean;
  participants: ExpenseParticipant[];
  lineItems: ExpenseLineItem[];
  attachments: ExpenseAttachment[];
}

export interface ExpenseParticipant {
  id: string;
  userId: string;
  ruleType: 'EQUAL' | 'PERCENTAGE' | 'EXACT' | 'SHARES';
  ruleValue: number;
  calculatedAmountCents: number;
  calculatedAmount: number;
  createdAt: string;
  user?: User;
}

export interface ExpenseLineItem {
  id: string;
  description: string;
  quantity: number;
  unitPriceCents: number;
  unitPrice: number;
  totalPriceCents: number;
  totalPrice: number;
  category?: string;
  createdAt: string;
}

export interface ExpenseAttachment {
  id: string;
  fileName: string;
  fileUrl: string;
  fileSizeBytes: number;
  mimeType: string;
  uploadedBy: string;
  createdAt: string;
}

export interface CreateExpenseRequest {
  groupId: string;
  currency: string;
  amount: number;
  occurredAt: string;
  note?: string;
  category: string;
  participants: ParticipantRequest[];
  lineItems?: LineItemRequest[];
  paidBy?: string;
}

export interface UpdateExpenseRequest {
  currency: string;
  amount: number;
  occurredAt: string;
  note?: string;
  category: string;
  participants: ParticipantRequest[];
  lineItems?: LineItemRequest[];
}

export interface ParticipantRequest {
  userId: string;
  ruleType: 'EQUAL' | 'PERCENTAGE' | 'EXACT' | 'SHARES';
  ruleValue: number;
}

export interface LineItemRequest {
  description: string;
  quantity: number;
  unitPrice: number;
  category?: string;
}

export interface ExpenseSummary {
  id: string;
  groupId: string;
  creatorId: string;
  paidBy: string;
  currency: string;
  amount: number;
  occurredAt: string;
  note?: string;
  category: string;
  participantCount: number;
  createdAt: string;
}

export interface ExpenseStatistics {
  totalExpenses: number;
  totalExpenseCount: number;
  averageExpenseAmount: number;
  categoryBreakdown: Record<string, number>;
  memberSpending: Array<{
    userId: string;
    totalSpent: number;
    expenseCount: number;
  }>;
}

export interface Settlement {
  id: string;
  groupId: string;
  fromUserId: string;
  toUserId: string;
  amountCents: number;
  amount: number;
  currency: string;
  description?: string;
  status: 'PENDING' | 'CONFIRMED' | 'CANCELLED';
  paymentMethod?: 'CASH' | 'BANK_TRANSFER' | 'DIGITAL_WALLET';
  notes?: string;
  createdAt: string;
  updatedAt: string;
  confirmedAt?: string;
}

export interface CreateSettlementRequest {
  groupId: string;
  fromUserId: string;
  toUserId: string;
  amount: number;
  currency: string;
  description?: string;
}

export interface ConfirmSettlementRequest {
  paymentMethod: 'CASH' | 'BANK_TRANSFER' | 'DIGITAL_WALLET';
  notes?: string;
}

export interface GroupBalance {
  userId: string;
  totalOwed: number;
  totalOwedTo: number;
  netBalance: number;
  currency: string;
  user?: User;
}

export interface ExchangeRate {
  baseCurrency: string;
  targetCurrency: string;
  rate: number;
  lastUpdated: string;
}

export interface CurrencyConversionRequest {
  amount: number;
  fromCurrency: string;
  toCurrency: string;
}

export interface CurrencyConversionResponse {
  amount: number;
  convertedAmount: number;
  fromCurrency: string;
  toCurrency: string;
  rate: number;
  timestamp: string;
}

// Common API response types
export interface ApiResponse<T> {
  data: T;
  message?: string;
  timestamp: string;
}

export interface PaginatedResponse<T> {
  content: T[];
  pageable: {
    pageNumber: number;
    pageSize: number;
    sort: {
      sorted: boolean;
      unsorted: boolean;
    };
  };
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  size: number;
  number: number;
}

export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  details?: Array<{
    field: string;
    message: string;
  }>;
}

// Authentication types
export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  access_token: string;
  refresh_token: string;
  token_type: string;
  expires_in: number;
}

export interface AuthUser {
  sub: string;
  email: string;
  name?: string;
  roles: string[];
  groups: string[];
  exp: number;
  iat: number;
}
