# 🏗️ Architecture Documentation - Expenses Platform

## 📋 Table of Contents
- [System Overview](#-system-overview)
- [Microservices Architecture](#-microservices-architecture)
- [Event-Driven Design](#-event-driven-design)
- [Data Architecture](#-data-architecture)
- [Security Architecture](#-security-architecture)
- [Infrastructure & Deployment](#-infrastructure--deployment)
- [API Gateway Pattern](#-api-gateway-pattern)
- [Scalability & Performance](#-scalability--performance)
- [Monitoring & Observability](#-monitoring--observability)

---

## 🎯 System Overview

### High-Level Architecture
```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Mobile App    │    │    Web Client    │    │   Admin Panel   │
└─────────┬───────┘    └─────────┬────────┘    └─────────┬───────┘
          │                      │                       │
          └──────────────────────┼───────────────────────┘
                                 │
                    ┌────────────▼────────────┐
                    │     API Gateway         │
                    │  (Spring Cloud Gateway) │
                    └────────────┬────────────┘
                                 │
        ┌────────────────────────┼────────────────────────┐
        │                       │                        │
┌───────▼───────┐    ┌─────────▼────────┐    ┌─────────▼────────┐
│ User Service  │    │  Group Service   │    │ Expense Service  │
└───────┬───────┘    └─────────┬────────┘    └─────────┬────────┘
        │                      │                       │
        └──────────────────────┼───────────────────────┘
                               │
                    ┌─────────▼────────┐
                    │  Apache Kafka    │
                    │ (Event Backbone) │
                    └─────────┬────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        │                    │                     │
┌───────▼───────┐  ┌────────▼────────┐  ┌────────▼────────┐
│Split Engine   │  │Settlement Svc   │  │   FX Service    │
│   Service     │  │                 │  │                 │
└───────────────┘  └─────────────────┘  └─────────────────┘
```

### Core Principles
- **Domain-Driven Design** - Services aligned with business domains
- **Microservices Pattern** - Loosely coupled, independently deployable
- **Event-Driven Architecture** - Asynchronous communication via events
- **API-First Design** - OpenAPI specifications for all services
- **Cloud-Native** - Container-ready, horizontally scalable

---

## 🔧 Microservices Architecture

### Service Catalog

#### 🌐 **API Gateway** (`services/gateway`)
**Purpose:** Single entry point, routing, cross-cutting concerns
- **Technology:** Spring Cloud Gateway (WebFlux)
- **Responsibilities:**
  - Request routing to downstream services
  - Rate limiting (Redis-backed)
  - Circuit breaker (Resilience4j)
  - CORS handling
  - JWT token validation
  - Request/response transformation
- **Patterns:** Gateway Aggregation, Backend for Frontend

#### 👤 **User Service** (`services/svc-user`)
**Purpose:** User identity and profile management
- **Technology:** Spring Boot, JPA, PostgreSQL
- **Responsibilities:**
  - User registration and authentication
  - Profile management (name, email, preferences)
  - User preferences and settings
  - Account verification and password reset
- **Events Published:** `UserCreated`, `UserUpdated`, `UserDeleted`

#### 👥 **Group Service** (`services/svc-group`)
**Purpose:** Group lifecycle and membership management
- **Technology:** Spring Boot, JPA, PostgreSQL
- **Responsibilities:**
  - Group creation and management
  - Member invitations and approvals
  - Role-based permissions (Owner, Admin, Member)
  - Group settings and preferences
- **Events Published:** `GroupCreated`, `MemberAdded`, `MemberRemoved`
- **Events Consumed:** `UserCreated`, `UserDeleted`

#### 💰 **Expense Service** (`services/svc-expense`)
**Purpose:** Expense tracking and participant management
- **Technology:** Spring Boot, JPA, PostgreSQL
- **Responsibilities:**
  - Expense CRUD operations
  - Participant management
  - Expense categories and tags
  - File attachments (receipts)
- **Events Published:** `ExpenseCreated`, `ExpenseUpdated`, `ExpenseDeleted`
- **Events Consumed:** `GroupCreated`, `MemberAdded`

#### 🧮 **Split Engine Service** (`services/svc-split-engine`)
**Purpose:** Expense splitting algorithms and debt calculation
- **Technology:** Spring Boot, JPA, PostgreSQL
- **Responsibilities:**
  - Split calculations (equal, percentage, exact, shares)
  - Debt optimization algorithms
  - Balance tracking per user/group
  - Settlement recommendations
- **Events Published:** `SplitCalculated`, `BalanceUpdated`
- **Events Consumed:** `ExpenseCreated`, `ExpenseUpdated`

#### 💳 **Settlement Service** (`services/svc-settlement`)
**Purpose:** Payment settlements and dispute resolution
- **Technology:** Spring Boot, JPA, PostgreSQL
- **Responsibilities:**
  - Settlement proposals and tracking
  - Payment confirmations
  - Dispute resolution workflow
  - Settlement history
- **Events Published:** `SettlementProposed`, `PaymentConfirmed`
- **Events Consumed:** `SplitCalculated`, `BalanceUpdated`

#### 💱 **FX Service** (`services/svc-fx`)
**Purpose:** Currency exchange and conversion
- **Technology:** Spring Boot, JPA, PostgreSQL
- **Responsibilities:**
  - Exchange rate management
  - Currency conversion calculations
  - Historical rate tracking
  - Rate alert notifications
- **Events Published:** `ExchangeRateUpdated`

#### 📚 **Common Library** (`libs/common`)
**Purpose:** Shared domain events and utilities
- **Technology:** Java Library
- **Contents:**
  - Domain event definitions
  - Event publisher/handler interfaces
  - Shared DTOs and utilities
  - OpenAPI configurations

---

## ⚡ Event-Driven Design

### Event Architecture Pattern
```
┌─────────────┐    Publish    ┌─────────────┐    Consume    ┌─────────────┐
│  Service A  │ ────────────► │   Apache    │ ────────────► │  Service B  │
│             │    Events     │   Kafka     │    Events     │             │
└─────────────┘               └─────────────┘               └─────────────┘
```

### Domain Events

#### User Domain Events
```java
// User lifecycle events
UserEvent.UserCreated(userId, email, name, timestamp)
UserEvent.UserUpdated(userId, changes, timestamp)
UserEvent.UserDeleted(userId, timestamp)
```

#### Group Domain Events
```java
// Group management events
GroupEvent.GroupCreated(groupId, name, ownerId, timestamp)
GroupEvent.MemberAdded(groupId, userId, role, timestamp)
GroupEvent.MemberRemoved(groupId, userId, timestamp)
GroupEvent.OwnershipTransferred(groupId, oldOwnerId, newOwnerId, timestamp)
```

#### Expense Domain Events
```java
// Expense tracking events
ExpenseEvent.ExpenseCreated(expenseId, groupId, amount, participants, timestamp)
ExpenseEvent.ExpenseUpdated(expenseId, changes, timestamp)
ExpenseEvent.ExpenseDeleted(expenseId, timestamp)
```

#### Ledger Domain Events
```java
// Financial tracking events
LedgerEvent.SplitCalculated(calculationId, expenseId, splits, timestamp)
LedgerEvent.BalanceUpdated(userId, groupId, newBalance, timestamp)
LedgerEvent.SettlementProposed(settlementId, fromUser, toUser, amount, timestamp)
```

### Event Processing Patterns
- **Event Sourcing** - Complete audit trail of all changes
- **CQRS** - Separate read/write models for complex queries
- **Saga Pattern** - Distributed transaction management
- **Event Streaming** - Real-time event processing

---

## 🗄️ Data Architecture

### Database per Service Pattern
Each microservice owns its data and database:

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│ User Service│    │Group Service│    │Expense Svc  │
│             │    │             │    │             │
│ ┌─────────┐ │    │ ┌─────────┐ │    │ ┌─────────┐ │
│ │  User   │ │    │ │ Group   │ │    │ │Expense  │ │
│ │   DB    │ │    │ │   DB    │ │    │ │   DB    │ │
│ │(Postgres│ │    │ │(Postgres│ │    │ │(Postgres│ │
│ └─────────┘ │    │ └─────────┘ │    │ └─────────┘ │
└─────────────┘    └─────────────┘    └─────────────┘
```

### Database Schema Design

#### User Service Schema
```sql
-- Users table
CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- User preferences
CREATE TABLE user_preferences (
    user_id UUID REFERENCES users(id),
    default_currency VARCHAR(3) DEFAULT 'USD',
    timezone VARCHAR(50) DEFAULT 'UTC',
    notification_settings JSONB
);
```

#### Group Service Schema
```sql
-- Groups table
CREATE TABLE groups (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    owner_id UUID NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Group members
CREATE TABLE group_members (
    group_id UUID REFERENCES groups(id),
    user_id UUID NOT NULL,
    role VARCHAR(20) DEFAULT 'MEMBER',
    joined_at TIMESTAMP DEFAULT NOW(),
    PRIMARY KEY (group_id, user_id)
);
```

### Data Consistency Strategies
- **Strong Consistency** - Within service boundaries
- **Eventual Consistency** - Across service boundaries
- **Compensating Transactions** - For distributed rollbacks
- **Event Sourcing** - For complete audit trails

---

## 🔐 Security Architecture

### Authentication & Authorization Flow
```
┌─────────────┐    1. Login    ┌─────────────┐
│   Client    │ ─────────────► │  Keycloak   │
│             │                │ (OAuth2/JWT)│
└─────────────┘                └─────────────┘
       │                              │
       │ 2. JWT Token                 │
       ▼                              │
┌─────────────┐    3. API Call  ┌─────▼───────┐
│   Client    │ ─────────────► │ API Gateway │
│ (JWT Token) │                │             │
└─────────────┘                └─────────────┘
                                      │
                               4. Validate JWT
                                      │
                                      ▼
                               ┌─────────────┐
                               │ Microservice│
                               │             │
                               └─────────────┘
```

### Security Layers
1. **Network Security** - HTTPS/TLS everywhere
2. **API Gateway Security** - JWT validation, rate limiting
3. **Service-to-Service** - Internal API keys or mTLS
4. **Data Security** - Encryption at rest and in transit
5. **Audit Logging** - Complete security event tracking

### JWT Token Structure
```json
{
  "sub": "user-uuid",
  "email": "user@example.com",
  "roles": ["USER"],
  "groups": ["group-uuid-1", "group-uuid-2"],
  "exp": 1640995200,
  "iat": 1640908800
}
```

---

## 🚀 Infrastructure & Deployment

### Container Architecture
```
┌─────────────────────────────────────────────────────────┐
│                    Docker Compose                       │
├─────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐     │
│  │API Gateway  │  │User Service │  │Group Service│     │
│  │   :8080     │  │   :8084     │  │   :8082     │     │
│  └─────────────┘  └─────────────┘  └─────────────┘     │
│                                                         │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐     │
│  │Expense Svc  │  │Settlement   │  │Split Engine │     │
│  │   :8083     │  │   :8085     │  │   :8087     │     │
│  └─────────────┘  └─────────────┘  └─────────────┘     │
├─────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐     │
│  │ PostgreSQL  │  │    Redis    │  │Apache Kafka │     │
│  │(per service)│  │   :6379     │  │   :9092     │     │
│  └─────────────┘  └─────────────┘  └─────────────┘     │
│                                                         │
│  ┌─────────────┐                                       │
│  │  Keycloak   │                                       │
│  │   :8081     │                                       │
│  └─────────────┘                                       │
└─────────────────────────────────────────────────────────┘
```

### Production Deployment (Future)
- **Kubernetes** - Container orchestration
- **Helm Charts** - Application packaging
- **GitOps** - Infrastructure as Code
- **Multi-region** - Global deployment
- **Auto-scaling** - Horizontal pod autoscaling

---

## 🌐 API Gateway Pattern

### Gateway Responsibilities
```
┌─────────────────────────────────────────────────────────┐
│                   API Gateway                           │
├─────────────────────────────────────────────────────────┤
│  🔀 Request Routing      │  🛡️  Security               │
│  • Path-based routing    │  • JWT validation           │
│  • Load balancing        │  • Rate limiting            │
│  • Service discovery     │  • CORS handling            │
├─────────────────────────────────────────────────────────┤
│  ⚡ Performance          │  🔧 Cross-cutting           │
│  • Response caching      │  • Request/response logging │
│  • Circuit breaker       │  • Metrics collection       │
│  • Timeout handling      │  • Error handling           │
└─────────────────────────────────────────────────────────┘
```

### Routing Configuration
```yaml
# Gateway routes
routes:
  - id: user-service
    uri: lb://svc-user
    predicates:
      - Path=/api/users/**
    filters:
      - RewritePath=/api/users/(?<segment>.*), /api/me/$\{segment}
      - RequestRateLimiter
```

---

## 📈 Scalability & Performance

### Horizontal Scaling Strategy
```
                    Load Balancer
                         │
        ┌────────────────┼────────────────┐
        │                │                │
   ┌─────────┐      ┌─────────┐      ┌─────────┐
   │Gateway-1│      │Gateway-2│      │Gateway-3│
   └─────────┘      └─────────┘      └─────────┘
        │                │                │
   ┌─────────┐      ┌─────────┐      ┌─────────┐
   │Service-1│      │Service-2│      │Service-3│
   └─────────┘      └─────────┘      └─────────┘
```

### Performance Optimizations
- **Connection Pooling** - Database connections
- **Caching Strategy** - Redis for frequently accessed data
- **Async Processing** - Event-driven, non-blocking I/O
- **Database Optimization** - Indexes, query optimization
- **CDN Integration** - Static asset delivery

### Scalability Patterns
- **Database Sharding** - Horizontal data partitioning
- **Read Replicas** - Separate read/write databases
- **Event Streaming** - Kafka for high-throughput messaging
- **Microservice Decomposition** - Fine-grained service boundaries

---

## 📊 Monitoring & Observability

### Observability Stack (Future)
```
┌─────────────────────────────────────────────────────────┐
│                  Observability                          │
├─────────────────────────────────────────────────────────┤
│  📈 Metrics          │  📋 Logs             │  🔍 Traces│
│  • Prometheus        │  • ELK Stack         │  • Jaeger │
│  • Grafana           │  • Structured logs   │  • OpenTel│
│  • Custom metrics    │  • Log aggregation   │  • Distrib│
├─────────────────────────────────────────────────────────┤
│  🚨 Alerting         │  📊 Dashboards       │  🔧 Health│
│  • PagerDuty         │  • Business metrics  │  • Actuator│
│  • Slack integration │  • Technical metrics │  • Readines│
│  • SLA monitoring    │  • Real-time views   │  • Liveness│
└─────────────────────────────────────────────────────────┘
```

### Health Checks
- **Liveness Probes** - Service is running
- **Readiness Probes** - Service ready to accept traffic
- **Dependency Checks** - Database, Kafka connectivity
- **Business Health** - Critical business flow validation

---

## 🎯 Architecture Decisions & Trade-offs

### Why Microservices?
✅ **Pros:**
- Independent deployability
- Technology diversity
- Team autonomy
- Fault isolation
- Horizontal scalability

❌ **Cons:**
- Distributed system complexity
- Network latency
- Data consistency challenges
- Operational overhead

### Why Event-Driven Architecture?
✅ **Pros:**
- Loose coupling between services
- Asynchronous processing
- Better scalability
- Audit trail capabilities
- Real-time features

❌ **Cons:**
- Eventual consistency
- Debugging complexity
- Message ordering challenges
- Infrastructure dependencies

### Technology Choices
- **Java 21** - Modern JVM features, performance improvements
- **Spring Boot 3** - Mature ecosystem, excellent tooling
- **PostgreSQL** - ACID compliance, JSON support, performance
- **Apache Kafka** - High-throughput event streaming
- **Redis** - In-memory caching, session storage
- **Docker** - Consistent deployment, easy local development

---

## 🔮 Future Architecture Evolution

### Phase 2 Enhancements
- **GraphQL Gateway** - Flexible client queries
- **Event Sourcing** - Complete audit trail
- **CQRS** - Optimized read/write models
- **Service Mesh** - Advanced networking (Istio)

### Phase 3 Scaling
- **Multi-region Deployment** - Global availability
- **Event Streaming Platform** - Real-time analytics
- **Machine Learning Pipeline** - AI-powered features
- **Edge Computing** - Reduced latency

---

*Last Updated: September 19, 2025*
*Architecture Version: 1.0*
