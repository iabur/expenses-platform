# 🚀 Deployment Guide - Expenses Platform

## 📋 Table of Contents
- [Local Development](#-local-development)
- [Docker Deployment](#-docker-deployment)
- [Production Deployment](#-production-deployment)
- [Environment Configuration](#-environment-configuration)
- [Database Setup](#-database-setup)
- [Monitoring & Health Checks](#-monitoring--health-checks)
- [Troubleshooting](#-troubleshooting)

---

## 💻 Local Development

### Prerequisites
- **Java 21+** - [Download OpenJDK](https://openjdk.java.net/projects/jdk/21/)
- **Docker & Docker Compose** - [Install Docker](https://docs.docker.com/get-docker/)
- **Git** - Version control

### Quick Setup
```bash
# Clone repository
git clone <repository-url>
cd expenses-platform

# Build all services
./gradlew clean build

# Start infrastructure
docker compose up --build

# Verify deployment
curl http://localhost:8080/actuator/health
```

### Service URLs
| Service | URL | Purpose |
|---------|-----|---------|
| **API Gateway** | http://localhost:8080 | Main entry point |
| **User Service** | http://localhost:8084 | User management |
| **Group Service** | http://localhost:8082 | Group operations |
| **Expense Service** | http://localhost:8083 | Expense tracking |
| **Settlement Service** | http://localhost:8085 | Payment settlements |
| **Keycloak** | http://localhost:8081 | Authentication |
| **Kafka UI** | http://localhost:8090 | Event monitoring |

---

## 🐳 Docker Deployment

### Docker Compose Architecture
```yaml
# docker-compose.yml structure
services:
  # Infrastructure
  - postgres (multiple instances)
  - redis
  - kafka + zookeeper
  - keycloak
  
  # Application Services
  - gateway
  - svc-user
  - svc-group
  - svc-expense
  - svc-settlement
  - svc-split-engine
  - svc-fx
```

### Build & Deploy Commands
```bash
# Build all services
docker compose build

# Start all services
docker compose up -d

# Start specific service
docker compose up -d svc-user

# View logs
docker compose logs -f gateway

# Scale service
docker compose up -d --scale svc-user=3

# Stop all services
docker compose down

# Clean up volumes
docker compose down -v
```

### Health Verification
```bash
# Check all containers
docker compose ps

# Health check script
#!/bin/bash
services=("gateway" "svc-user" "svc-group" "svc-expense")
for service in "${services[@]}"; do
  echo "Checking $service..."
  curl -f http://localhost:$(docker compose port $service 8080 | cut -d: -f2)/actuator/health
done
```

---

## 🏢 Production Deployment

### Kubernetes Deployment (Future)

#### Namespace Setup
```yaml
# namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: expenses-platform
```

#### ConfigMap Example
```yaml
# configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: expenses-config
  namespace: expenses-platform
data:
  SPRING_PROFILES_ACTIVE: "production"
  KAFKA_BOOTSTRAP_SERVERS: "kafka:9092"
  REDIS_HOST: "redis"
```

#### Deployment Example
```yaml
# user-service-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: user-service
  namespace: expenses-platform
spec:
  replicas: 3
  selector:
    matchLabels:
      app: user-service
  template:
    metadata:
      labels:
        app: user-service
    spec:
      containers:
      - name: user-service
        image: expenses-platform/user-service:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "production"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
```

#### Service & Ingress
```yaml
# user-service-service.yaml
apiVersion: v1
kind: Service
metadata:
  name: user-service
  namespace: expenses-platform
spec:
  selector:
    app: user-service
  ports:
  - port: 80
    targetPort: 8080
---
# ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: expenses-platform-ingress
  namespace: expenses-platform
spec:
  rules:
  - host: api.expenses-platform.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: gateway
            port:
              number: 80
```

### Helm Chart Structure (Future)
```
helm/
├── Chart.yaml
├── values.yaml
├── templates/
│   ├── deployment.yaml
│   ├── service.yaml
│   ├── configmap.yaml
│   └── ingress.yaml
└── charts/
    ├── postgresql/
    ├── redis/
    └── kafka/
```

---

## ⚙️ Environment Configuration

### Environment Variables

#### Common Variables
```bash
# Application
SPRING_PROFILES_ACTIVE=production
SERVER_PORT=8080

# Database
DB_HOST=postgres
DB_PORT=5432
DB_NAME=expenses
DB_USERNAME=expenses
DB_PASSWORD=expenses123

# Kafka
KAFKA_BOOTSTRAP_SERVERS=kafka:9092
KAFKA_GROUP_ID=expenses-platform

# Redis
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=redis123

# Security
JWT_SECRET=your-jwt-secret-key
KEYCLOAK_URL=http://keycloak:8080
```

#### Service-Specific Variables

**User Service:**
```bash
USER_DB_NAME=userdb
USER_DB_USERNAME=userservice
USER_DB_PASSWORD=userpass123
```

**Group Service:**
```bash
GROUP_DB_NAME=groupdb
GROUP_DB_USERNAME=groupservice
GROUP_DB_PASSWORD=grouppass123
```

### Configuration Profiles

#### application.yml (Common)
```yaml
spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:local}
  
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  endpoint:
    health:
      show-details: always
      
logging:
  level:
    com.expenses: INFO
    org.springframework.security: DEBUG
```

#### application-production.yml
```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS}
    producer:
      retries: 3
      batch-size: 16384
    consumer:
      group-id: ${KAFKA_GROUP_ID}
      auto-offset-reset: earliest
      
  redis:
    host: ${REDIS_HOST}
    port: ${REDIS_PORT}
    password: ${REDIS_PASSWORD}
    timeout: 2000ms
    
logging:
  level:
    com.expenses: INFO
    org.springframework.security: WARN
```

---

## 🗄️ Database Setup

### PostgreSQL Configuration

#### Database Creation Script
```sql
-- Create databases for each service
CREATE DATABASE userdb;
CREATE DATABASE groupdb;
CREATE DATABASE expensedb;
CREATE DATABASE settlementdb;
CREATE DATABASE splitenginedb;
CREATE DATABASE fxdb;

-- Create service users
CREATE USER userservice WITH PASSWORD 'userpass123';
CREATE USER groupservice WITH PASSWORD 'grouppass123';
CREATE USER expenseservice WITH PASSWORD 'expensepass123';

-- Grant permissions
GRANT ALL PRIVILEGES ON DATABASE userdb TO userservice;
GRANT ALL PRIVILEGES ON DATABASE groupdb TO groupservice;
GRANT ALL PRIVILEGES ON DATABASE expensedb TO expenseservice;
```

#### Connection Pool Configuration
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      idle-timeout: 300000
      max-lifetime: 1200000
      connection-timeout: 20000
```

### Flyway Migrations

#### Migration Structure
```
src/main/resources/db/migration/
├── V1__init.sql              # Initial schema
├── V2__add_indexes.sql       # Performance indexes
├── V3__add_audit_columns.sql # Audit trail
└── V4__add_constraints.sql   # Data integrity
```

#### Example Migration
```sql
-- V1__init.sql (User Service)
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_created_at ON users(created_at);
```

---

## 📊 Monitoring & Health Checks

### Health Check Endpoints
```bash
# Gateway health
curl http://localhost:8080/actuator/health

# Detailed health with dependencies
curl http://localhost:8080/actuator/health/liveness
curl http://localhost:8080/actuator/health/readiness

# Service-specific health
curl http://localhost:8082/actuator/health  # Group Service
curl http://localhost:8083/actuator/health  # Expense Service
```

### Custom Health Indicators
```java
@Component
public class DatabaseHealthIndicator implements HealthIndicator {
    
    @Autowired
    private DataSource dataSource;
    
    @Override
    public Health health() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(1)) {
                return Health.up()
                    .withDetail("database", "PostgreSQL")
                    .withDetail("status", "Connected")
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withDetail("database", "PostgreSQL")
                .withException(e)
                .build();
        }
        return Health.down().build();
    }
}
```

### Prometheus Metrics
```bash
# Metrics endpoint
curl http://localhost:8080/actuator/prometheus

# Key metrics to monitor
- http_requests_total
- http_request_duration_seconds
- jvm_memory_used_bytes
- database_connections_active
- kafka_consumer_lag
```

---

## 🔧 Troubleshooting

### Common Issues

#### Service Won't Start
```bash
# Check container logs
docker compose logs svc-user

# Check Java process
docker compose exec svc-user jps -l

# Check port binding
docker compose ps
netstat -tulpn | grep :8082
```

#### Database Connection Issues
```bash
# Test database connectivity
docker compose exec svc-user pg_isready -h postgres -p 5432

# Check database logs
docker compose logs postgres

# Verify credentials
docker compose exec postgres psql -U expenses -d expensedb -c "SELECT 1;"
```

#### Kafka Connection Issues
```bash
# Check Kafka status
docker compose exec kafka kafka-topics --bootstrap-server localhost:9092 --list

# Check consumer groups
docker compose exec kafka kafka-consumer-groups --bootstrap-server localhost:9092 --list

# Monitor Kafka logs
docker compose logs kafka
```

### Performance Issues

#### Memory Problems
```bash
# Check memory usage
docker stats

# Heap dump analysis
docker compose exec svc-user jcmd 1 GC.run_finalization
docker compose exec svc-user jcmd 1 VM.classloader_stats
```

#### Database Performance
```sql
-- Check slow queries
SELECT query, mean_time, calls 
FROM pg_stat_statements 
ORDER BY mean_time DESC 
LIMIT 10;

-- Check connection count
SELECT count(*) FROM pg_stat_activity;

-- Check table sizes
SELECT schemaname, tablename, 
       pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size
FROM pg_tables 
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
```

### Debugging Tips

#### Enable Debug Logging
```yaml
# application-debug.yml
logging:
  level:
    com.expenses: DEBUG
    org.springframework.web: DEBUG
    org.springframework.security: DEBUG
    org.springframework.kafka: DEBUG
```

#### Remote Debugging
```bash
# Enable remote debugging in Docker
JAVA_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"

# Connect with IDE to localhost:5005
```

---

## 🔄 CI/CD Pipeline (Future)

### GitHub Actions Example
```yaml
# .github/workflows/deploy.yml
name: Deploy to Production

on:
  push:
    branches: [main]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v2
    
    - name: Set up JDK 21
      uses: actions/setup-java@v2
      with:
        java-version: '21'
        
    - name: Build with Gradle
      run: ./gradlew build
      
    - name: Build Docker images
      run: docker compose build
      
    - name: Push to registry
      run: |
        docker tag expenses-platform/gateway:latest $REGISTRY/gateway:$GITHUB_SHA
        docker push $REGISTRY/gateway:$GITHUB_SHA
        
    - name: Deploy to Kubernetes
      run: |
        kubectl set image deployment/gateway gateway=$REGISTRY/gateway:$GITHUB_SHA
        kubectl rollout status deployment/gateway
```

---

*Last Updated: September 19, 2025*
*Deployment Guide Version: 1.0*
