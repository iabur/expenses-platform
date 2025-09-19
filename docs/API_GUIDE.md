# 📚 API Guide - Expenses Platform

## 🎯 Overview
This guide provides comprehensive documentation for integrating with the Expenses Platform APIs.

## 🚀 Quick Start

### Base URLs
- **Development:** `http://localhost:8080`
- **Production:** `https://api.expenses-platform.com` (Future)

### Authentication
All API endpoints require JWT authentication except health checks.

#### Get JWT Token
```bash
curl -X POST http://localhost:8081/realms/expenses/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&client_id=web&username=alice@example.com&password=password"
```

#### Use Token in Requests
```bash
curl -H "Authorization: Bearer <JWT_TOKEN>" \
  http://localhost:8080/api/groups
```

---

## 👤 User Service API

### Base Path: `/api/users`

#### Get Current User Profile
```http
GET /api/users/me
Authorization: Bearer <JWT_TOKEN>
```

**Response:**
```json
{
  "id": "uuid",
  "email": "user@example.com",
  "name": "John Doe",
  "preferences": {
    "defaultCurrency": "USD",
    "timezone": "UTC"
  },
  "createdAt": "2025-01-01T00:00:00Z"
}
```

#### Update User Profile
```http
PUT /api/users/me
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json

{
  "name": "John Smith",
  "preferences": {
    "defaultCurrency": "EUR",
    "timezone": "Europe/London"
  }
}
```

---

## 👥 Group Service API

### Base Path: `/api/groups`

#### Create Group
```http
POST /api/groups
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json

{
  "name": "Weekend Trip",
  "description": "Paris weekend getaway",
  "type": "TRIP",
  "defaultCurrency": "EUR"
}
```

#### Get User's Groups
```http
GET /api/groups
Authorization: Bearer <JWT_TOKEN>
```

#### Add Member to Group
```http
POST /api/groups/{groupId}/members
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json

{
  "userId": "user-uuid",
  "role": "MEMBER"
}
```

---

## 💰 Expense Service API

### Base Path: `/api/expenses`

#### Create Expense
```http
POST /api/expenses
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json

{
  "groupId": "group-uuid",
  "description": "Dinner at restaurant",
  "totalAmountCents": 8500,
  "currency": "USD",
  "paidBy": "user-uuid",
  "splitMethod": "EQUAL",
  "participants": [
    {
      "userId": "user-uuid-1",
      "shareAmountCents": 2833
    },
    {
      "userId": "user-uuid-2", 
      "shareAmountCents": 2833
    }
  ]
}
```

#### Get Group Expenses
```http
GET /api/expenses?groupId={groupId}
Authorization: Bearer <JWT_TOKEN>
```

---

## 🧮 Split Engine API

### Base Path: `/api/splits`

#### Calculate Splits
```http
POST /api/splits/calculate
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json

{
  "expenseId": "expense-uuid",
  "groupId": "group-uuid",
  "totalAmountCents": 10000,
  "currency": "USD",
  "splitMethod": "PERCENTAGE",
  "participants": [
    {
      "userId": "user-1",
      "percentage": 60.0
    },
    {
      "userId": "user-2",
      "percentage": 40.0
    }
  ]
}
```

#### Get Group Balances
```http
GET /api/splits/group/{groupId}/balances
Authorization: Bearer <JWT_TOKEN>
```

---

## 💳 Settlement Service API

### Base Path: `/api/settlements`

#### Create Settlement Proposal
```http
POST /api/settlements/proposals
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json

{
  "groupId": "group-uuid",
  "fromUserId": "debtor-uuid",
  "toUserId": "creditor-uuid",
  "amountCents": 5000,
  "currency": "USD",
  "description": "Settlement for dinner expenses"
}
```

#### Confirm Payment
```http
POST /api/settlements/{settlementId}/confirm
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json

{
  "paymentMethod": "CASH",
  "notes": "Paid in cash"
}
```

---

## 💱 FX Service API

### Base Path: `/api/fx`

#### Get Exchange Rates
```http
GET /api/fx/rates?base=USD&target=EUR
Authorization: Bearer <JWT_TOKEN>
```

#### Convert Amount
```http
POST /api/fx/convert
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json

{
  "amountCents": 10000,
  "fromCurrency": "USD",
  "toCurrency": "EUR"
}
```

---

## 📊 Common Patterns

### Pagination
Most list endpoints support pagination:

```http
GET /api/expenses?page=0&size=20&sort=createdAt,desc
```

**Response:**
```json
{
  "content": [...],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "sort": {...}
  },
  "totalElements": 150,
  "totalPages": 8,
  "first": true,
  "last": false
}
```

### Error Handling
Standard HTTP status codes with detailed error responses:

```json
{
  "timestamp": "2025-01-01T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/expenses",
  "details": [
    {
      "field": "totalAmountCents",
      "message": "Amount must be positive"
    }
  ]
}
```

### Filtering & Search
Many endpoints support filtering:

```http
GET /api/expenses?groupId={id}&category=FOOD&dateFrom=2025-01-01&dateTo=2025-01-31
```

---

## 🔄 Webhooks (Future)

### Event Notifications
Subscribe to real-time events:

- `expense.created`
- `expense.updated`
- `settlement.proposed`
- `payment.confirmed`

### Webhook Payload Example
```json
{
  "eventType": "expense.created",
  "timestamp": "2025-01-01T12:00:00Z",
  "data": {
    "expenseId": "uuid",
    "groupId": "uuid",
    "amount": 10000,
    "currency": "USD"
  }
}
```

---

## 📱 Mobile SDKs (Future)

### iOS SDK
```swift
import ExpensesPlatformSDK

let client = ExpensesClient(baseURL: "https://api.expenses-platform.com")
client.authenticate(token: jwtToken)

// Create expense
let expense = CreateExpenseRequest(
    groupId: "uuid",
    description: "Lunch",
    amount: 2500
)
client.expenses.create(expense) { result in
    // Handle result
}
```

### Android SDK
```kotlin
import com.expenses.platform.sdk.ExpensesClient

val client = ExpensesClient("https://api.expenses-platform.com")
client.authenticate(jwtToken)

// Get user groups
client.groups.getUserGroups { groups ->
    // Handle groups
}
```

---

## 🧪 Testing

### Postman Collection
Import our Postman collection for easy API testing:
```bash
curl -o expenses-platform.postman_collection.json \
  https://api.expenses-platform.com/postman/collection
```

### Test Data
Use these test accounts for development:

| Email | Password | Role |
|-------|----------|------|
| alice@example.com | password | Standard User |
| bob@example.com | password | Standard User |
| admin@example.com | password | Admin User |

---

## 📈 Rate Limits

### Current Limits
- **Authenticated requests:** 1000 requests/hour
- **Public endpoints:** 100 requests/hour
- **Bulk operations:** 10 requests/minute

### Rate Limit Headers
```http
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 999
X-RateLimit-Reset: 1640995200
```

---

## 🔍 OpenAPI Specifications

### Interactive Documentation
- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8080/v3/api-docs

### Service-Specific Docs
- **User Service:** http://localhost:8084/swagger-ui/index.html
- **Group Service:** http://localhost:8082/swagger-ui/index.html
- **Expense Service:** http://localhost:8083/swagger-ui/index.html

---

*Last Updated: September 19, 2025*
*API Version: v1*
