# 💰 Expenses Platform - Next-Generation Expense Sharing

> **A modern, scalable expense sharing platform built to surpass Splitwise with advanced features, better UX, and enterprise-grade capabilities.**

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Microservices](https://img.shields.io/badge/Architecture-Microservices-blue.svg)](https://microservices.io/)
[![Event Driven](https://img.shields.io/badge/Pattern-Event%20Driven-purple.svg)](https://martinfowler.com/articles/201701-event-driven.html)
[![Docker](https://img.shields.io/badge/Deployment-Docker-blue.svg)](https://www.docker.com/)

## 🎯 Vision

Building the **next-generation expense sharing platform** that provides:
- 🔥 **Smart Debt Optimization** - Minimize transaction complexity
- 🔥 **Real-time Collaboration** - Live updates with WebSocket
- 🔥 **Receipt Intelligence** - OCR + AI for automatic data extraction
- 🔥 **Advanced Analytics** - Predictive insights and spending patterns
- 🔥 **Enterprise Ready** - Scalable microservices architecture

## 🏗️ Architecture

### Microservices
- **`services/gateway`** — API Gateway with routing, rate limiting, circuit breaker
- **`services/svc-user`** — User management, authentication, profiles
- **`services/svc-group`** — Group creation, membership, invitations
- **`services/svc-expense`** — Expense tracking, participant management
- **`services/svc-split-engine`** — Split calculations, debt optimization
- **`services/svc-settlement`** — Payment settlements, dispute resolution
- **`services/svc-fx`** — Currency exchange rates and conversion
- **`services/svc-identity-config`** — Keycloak configuration (realm: `expenses`)
- **`libs/common`** — Shared domain events and utilities

### Event-Driven Architecture
- **Apache Kafka** for asynchronous communication
- **Domain Events** for service decoupling
- **Event Sourcing** for audit trails

### Infrastructure
- **PostgreSQL** databases per service
- **Redis** for caching and rate limiting
- **Keycloak** for OAuth2/JWT authentication
- **Docker Compose** for local development

## 🚀 Quick Start

### Prerequisites
- **Java 21+** - [Download OpenJDK](https://openjdk.java.net/projects/jdk/21/)
- **Docker & Docker Compose** - [Install Docker](https://docs.docker.com/get-docker/)
- **Gradle 8.x** (wrapper included)

### 1. Clone & Build
```bash
git clone <repository-url>
cd expenses-platform
./gradlew clean build
```

### 2. Start Infrastructure
```bash
docker compose up --build
```

### 3. Access Services
- **API Gateway**: http://localhost:8080
- **Swagger UI**: http://localhost:8082/swagger-ui/index.html (Group Service example)
- **Keycloak Admin**: http://localhost:8081 (admin / admin)

### 4. Test Users
Pre-seeded test accounts:
- `alice@example.com` / `password`
- `bob@example.com` / `password`

## 📚 API Documentation

### Service Endpoints
| Service | Port | Swagger UI |
|---------|------|------------|
| **User Service** | 8084 | http://localhost:8084/swagger-ui/index.html |
| **Group Service** | 8082 | http://localhost:8082/swagger-ui/index.html |
| **Expense Service** | 8083 | http://localhost:8083/swagger-ui/index.html |
| **Settlement Service** | 8085 | http://localhost:8085/swagger-ui/index.html |
| **Split Engine** | 8087 | http://localhost:8087/swagger-ui/index.html |
| **FX Service** | 8088 | http://localhost:8088/swagger-ui/index.html |

### Gateway Routes
All services accessible through Gateway at `http://localhost:8080`:
- `/api/users/**` → User Service
- `/api/groups/**` → Group Service  
- `/api/expenses/**` → Expense Service
- `/api/settlements/**` → Settlement Service
- `/api/splits/**` → Split Engine Service
- `/api/fx/**` → FX Service

## 🧪 Testing

### Health Checks
```bash
# Gateway health
curl http://localhost:8080/actuator/health

# Individual service health
curl http://localhost:8082/actuator/health  # Group Service
curl http://localhost:8083/actuator/health  # Expense Service
```

### API Testing with Authentication
1. **Get JWT Token** from Keycloak:
```bash
curl -X POST http://localhost:8081/realms/expenses/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&client_id=web&username=alice@example.com&password=password"
```

2. **Use Token** in API calls:
```bash
curl -H "Authorization: Bearer <JWT_TOKEN>" \
  http://localhost:8080/api/groups
```

## 🛠️ Development

### Project Structure
```
expenses-platform/
├── services/           # Microservices
│   ├── gateway/       # API Gateway
│   ├── svc-user/      # User Service
│   ├── svc-group/     # Group Service
│   └── ...
├── libs/              # Shared libraries
│   └── common/        # Domain events & utilities
├── docker-compose.yml # Local development stack
└── DEVELOPMENT_PLAN.md # 3-phase development roadmap
```

### Adding New Services
1. Create service directory under `services/`
2. Add to `settings.gradle`
3. Configure in `docker-compose.yml`
4. Add Gateway routes in `application.yml`

### Database Migrations
Using Flyway for schema versioning:
```bash
# Located in each service
src/main/resources/db/migration/V1__init.sql
```

## 🎯 Roadmap

See [DEVELOPMENT_PLAN.md](./DEVELOPMENT_PLAN.md) for detailed 3-phase roadmap:

### Phase 1: Basic MVP (67% Complete)
- ✅ Core services (User, Group, Expense, Settlement, Split Engine)
- ✅ Event-driven architecture
- ✅ Docker deployment
- ❌ Testing & validation (in progress)

### Phase 2: Advanced Features
- Receipt OCR & AI categorization  
- Real-time WebSocket updates
- Payment integration (Stripe/PayPal)
- Advanced analytics & insights

### Phase 3: Enterprise & Scale
- Multi-region deployment
- Advanced security & compliance
- ML-based features
- Native mobile apps

## 🤝 Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🔗 Links

- [Development Plan](./DEVELOPMENT_PLAN.md) - Detailed roadmap and requirements
- [Documentation Hub](./docs/INDEX.md) - Complete documentation index
- [Architecture Guide](./docs/ARCHITECTURE.md) - System design and patterns
- [API Guide](./docs/API_GUIDE.md) - Complete API reference
- [Deployment Guide](./docs/DEPLOYMENT_GUIDE.md) - Setup and deployment instructions
- [Interactive API Docs](http://localhost:8080/swagger-ui.html) - Swagger UI

---

**Built with ❤️ to revolutionize expense sharing**
