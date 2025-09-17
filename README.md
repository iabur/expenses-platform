## Contents
- `services/gateway` — Spring Cloud Gateway + JWT resource server
- `services/svc-user` — user profile stub
- `services/svc-group` — group CRUD stub
- `services/svc-expense` — expense CRUD stub
- `services/svc-fx` — FX rates stub
- `services/svc-identity-config` — Keycloak realm export (realm: `expenses`)
- `docker-compose.yml` — local dev stack (Keycloak, Kafka, Postgres per service)

## Prereqs
- Java 21
- Gradle 8.x (`gradle` on PATH) – wrapper not included for brevity
- Docker & Docker Compose

## Build
```bash
gradle clean build
```

## Run (local stack)
```bash
docker compose up --build
```
Keycloak admin: http://localhost:8081  (admin / admin). Users seeded: `alice@example.com` / `password`, `bob@example.com` / `password`.

Gateway: http://localhost:8080

## Quick test (without OAuth)
- Gateway root: `curl http://localhost:8080/` → `Gateway up`
- Services expose `/actuator/health` unauthenticated by default; user/group/expense endpoints expect JWT.
- Use Keycloak to obtain a token for client `web` and call the services via Gateway.
