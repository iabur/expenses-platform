# Local Development Setup Guide

This guide will help you set up the Expenses Platform for local development using IntelliJ IDEA.

## Prerequisites

- Java 17 or higher
- IntelliJ IDEA (Community or Ultimate)
- Docker and Docker Compose
- Gradle (or use the included Gradle wrapper)

## Quick Start

### 1. Start Infrastructure Services

Start only the infrastructure services (databases, Kafka, Redis, Keycloak) using the development Docker Compose:

```bash
docker-compose -f docker-compose.dev.yml up -d
```

This will start:
- **Keycloak** (Authentication) - http://localhost:8090
- **PostgreSQL Databases** (one per service):
  - User DB: localhost:5433
  - Group DB: localhost:5434
  - Expense DB: localhost:5435
  - Ledger DB: localhost:5436
  - Split Engine DB: localhost:5437
  - Settlement DB: localhost:5438
  - FX DB: localhost:5439
- **Kafka** - localhost:9092
- **Redis** - localhost:6380

### 2. Import Project in IntelliJ IDEA

1. Open IntelliJ IDEA
2. Select "Open" and choose the project root directory
3. IntelliJ will automatically detect it as a Gradle project
4. Wait for Gradle sync to complete

### 3. Run Services in IntelliJ IDEA

The project includes pre-configured run configurations for all services. Each service is configured to use the `dev` profile which connects to local infrastructure.

#### Available Run Configurations:

1. **Gateway Service** (Port 8080)
2. **User Service** (Port 8081)
3. **Group Service** (Port 8082)
4. **Expense Service** (Port 8083)
5. **Split Engine Service** (Port 8084)
6. **Settlement Service** (Port 8085)
7. **FX Service** (Port 8086)
8. **Ledger Service** (Port 8087)

#### Running Services:

1. Go to Run → Edit Configurations
2. You should see all service configurations listed
3. Select a service and click the "Run" button (▶️)
4. Or use the dropdown in the toolbar to select and run services

#### Recommended Startup Order:

1. **Gateway Service** (should be started last as it depends on other services)
2. **User Service**
3. **Group Service**
4. **Expense Service**
5. **Split Engine Service**
6. **Settlement Service**
7. **FX Service**
8. **Ledger Service**

### 4. Verify Services are Running

- **Gateway**: http://localhost:8080/swagger-ui.html
- **User Service**: http://localhost:8081/swagger-ui.html
- **Group Service**: http://localhost:8082/swagger-ui.html
- **Expense Service**: http://localhost:8083/swagger-ui.html
- **Split Engine**: http://localhost:8084/swagger-ui.html
- **Settlement Service**: http://localhost:8085/swagger-ui.html
- **FX Service**: http://localhost:8086/swagger-ui.html
- **Ledger Service**: http://localhost:8087/swagger-ui.html

## Development Profiles

Each service has been configured with a `dev` profile that:

- Connects to local databases (localhost with different ports)
- Uses local Kafka (localhost:9092)
- Uses local Redis (localhost:6380)
- Uses local Keycloak (localhost:8090)
- Enables debug logging for development

## Keycloak Setup

Keycloak is pre-configured with:
- **Admin Console**: http://localhost:8090
- **Username**: admin
- **Password**: admin
- **Realm**: expenses (pre-configured)

## Database Access

You can connect to individual databases using your preferred PostgreSQL client:

```bash
# User Database
psql -h localhost -p 5433 -U user -d userdb

# Group Database
psql -h localhost -p 5434 -U group -d groupdb

# Expense Database
psql -h localhost -p 5435 -U expense -d expensedb

# Ledger Database
psql -h localhost -p 5436 -U ledger -d ledgerdb

# Split Engine Database
psql -h localhost -p 5437 -U splitengine -d splitenginedb

# Settlement Database
psql -h localhost -p 5438 -U settlement -d settlementdb

# FX Database
psql -h localhost -p 5439 -U fxservice -d fxdb
```

## Troubleshooting

### Services Won't Start

1. **Check if infrastructure is running**:
   ```bash
   docker-compose -f docker-compose.dev.yml ps
   ```

2. **Check service logs** in IntelliJ IDEA console

3. **Verify database connections**:
   ```bash
   docker-compose -f docker-compose.dev.yml logs pg-user
   ```

### Port Conflicts

If you have port conflicts, you can modify the ports in:
- `docker-compose.dev.yml` for infrastructure services
- Individual service `application.yml` files for application ports

### Keycloak Issues

If Keycloak is not accessible:
1. Check if it's running: `docker-compose -f docker-compose.dev.yml logs keycloak`
2. Wait for it to fully start (may take 1-2 minutes)
3. Access admin console at http://localhost:8090

### Database Migration Issues

If you encounter Flyway migration issues:
1. Check database logs
2. Ensure databases are empty or properly migrated
3. You may need to clean databases: `docker-compose -f docker-compose.dev.yml down -v`

## Stopping Services

To stop infrastructure services:
```bash
docker-compose -f docker-compose.dev.yml down
```

To stop application services, simply stop them in IntelliJ IDEA.

## Development Workflow

1. Start infrastructure services with Docker Compose
2. Run services in IntelliJ IDEA using the provided run configurations
3. Use the Gateway service (port 8080) for API access
4. Individual service Swagger UIs are available for direct testing
5. Make code changes and restart services as needed

## Additional Notes

- All services use the `dev` profile automatically when run from IntelliJ IDEA
- Database schemas are automatically created/updated using Flyway
- Kafka topics are automatically created when services start
- Redis is used for rate limiting in the Gateway service
- All services are configured for local development with appropriate logging levels

