# Budge-It — Spring Boot Backend

Modular Monolith · Event-Driven AI · Spring Boot 4.0 · PostgreSQL · Redis · Spring AI (OpenAI + Anthropic)

---

## Prerequisites

| Tool                    | Version         |
|-------------------------|-----------------|
| Java                    | 21+             |
| Gradle                  | 9.4.0+          |
| Docker & Docker Compose | 24+             |
| PostgreSQL              | 16 (via Docker) |
| Redis                   | 7 (via Docker)  |

---

## Quick Start

### 1. Clone and configure environment

```bash
cp .env.example .env.dev
# Edit .env.dev and fill in:
#   OPEN_AI_KEY=sk-...
#   ANTHROPIC_API_KEY=sk-ant-...
#   JWT_SECRET=<64+ char random string>
#   WEBHOOK_HMAC_SECRET=<shared secret>
```

### 2. Start infrastructure

```bash
docker compose up -d
# Starts PostgreSQL on :5432 and Redis on :6379
```

### 3. Run the application

```bash
set -a && source .env.dev && set +a && ./gradlew bootRun --args='--spring.profiles.active=dev'
```

Flyway will automatically run all pending migrations under `src/main/resources/db/migration/` on startup.

---

## API

| Resource | Base URL |
|----------|----------|
| REST API | `http://localhost:8080/api/v1` |
| Swagger UI | `http://localhost:8080/swagger-ui/index.html` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |
| Health check | `http://localhost:8080/actuator/health` |

```bash
curl http://localhost:8080/actuator/health | python3 -m json.tool
```

---

## Endpoints Overview

### Authentication — `/api/v1/auth`

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/register` | Public | Create a new account |
| `POST` | `/login` | Public | Login and receive JWT + refresh token |
| `POST` | `/refresh` | Public | Exchange a refresh token for a new access token |
| `POST` | `/logout` | Public | Invalidate a refresh token |
| `POST` | `/change-password` | Bearer | Change the authenticated user's password |

### Users — `/api/v1/users`

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `GET` | `/me` | Bearer | Get the authenticated user's profile |

### Financial Profile — `/api/v1/users/me/financial-profile`

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `GET` | `/` | Bearer | Get the financial profile |
| `PUT` | `/` | Bearer | Create or update the financial profile |
| `DELETE` | `/` | Bearer | Delete the financial profile |

### Advisor — `/api/v1/advisor`

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/generate` | Bearer | Trigger async AI budget generation (returns 202) |
| `GET` | `/jobs/{jobId}` | Bearer | Poll job status (PENDING / COMPLETED / FAILED) |
| `GET` | `/jobs/{jobId}/stream` | Bearer | SSE stream — real-time job completion notification |
| `GET` | `/templates` | Bearer | List all generated budget templates |
| `GET` | `/templates/{id}` | Bearer | Get a single template |
| `PATCH` | `/templates/{id}/save` | Bearer | Toggle saved flag on a template |

---

## Project Structure

```
src/main/java/com/relab/budge_it/
├── BudgeItApplication.java
├── identity/                       # Registration, login, JWT, refresh tokens
│   ├── controller/
│   ├── domain/
│   ├── dto/
│   ├── event/
│   ├── repository/
│   └── service/
├── profile/                        # Financial survey profile (AI input)
│   ├── controller/
│   ├── domain/
│   ├── dto/
│   ├── event/
│   ├── repository/
│   └── service/
├── advisor/                        # Async AI budget generation & templates
│   ├── controller/
│   ├── domain/
│   ├── dto/
│   ├── event/
│   ├── repository/
│   └── service/
└── shared/
    ├── config/                     # AsyncConfig, ObjectMapperConfig, OpenApiConfig
    ├── domain/                     # RecordStatus enum
    ├── response/                   # ApiResponse, BusinessException, GlobalExceptionHandler
    └── security/                   # JwtService, JwtAuthenticationFilter, SecurityConfig,
                                    # AppUserDetails, AuthenticatedUserProvider,
                                    # SSETicket, SSETicketRepository
```

---

## Module Dependency Rules

- Modules **never** import each other's JPA repositories
- Cross-module reads use a **public service interface** in the target module
- Cross-module state changes use **Spring domain events** (`ApplicationEventPublisher`)

---

## Key Dependencies

| Dependency | Purpose |
|------------|---------|
| `spring-boot-starter-webmvc` | REST API |
| `spring-boot-starter-data-jpa` | JPA / Hibernate |
| `spring-boot-starter-security` | JWT auth filter chain |
| `spring-ai-starter-model-openai` | OpenAI integration |
| `spring-ai-starter-model-anthropic` | Anthropic / Claude integration |
| `spring-modulith-starter-core` | Module boundary enforcement |
| `flyway-database-postgresql` | Database migrations |
| `jjwt-api` | JWT generation & validation |
| `spring-boot-starter-data-redis` | Redis cache |
| `springdoc-openapi-starter-webmvc-ui` | Swagger UI / OpenAPI docs |
| `lombok` | Boilerplate reduction |
| `testcontainers` | Integration tests with real PostgreSQL |

---

## Environment Variables

See `.env.example` for the full list.

| Variable | Required | Description |
|----------|----------|-------------|
| `DB_HOST` | Yes | PostgreSQL host (default: `localhost`) |
| `DB_PORT` | Yes | PostgreSQL port (default: `5432`) |
| `DB_NAME` | Yes | Database name |
| `DB_USERNAME` | Yes | Database user |
| `DB_PASSWORD` | Yes | Database password |
| `REDIS_HOST` | Yes | Redis host (default: `localhost`) |
| `REDIS_PORT` | Yes | Redis port (default: `6379`) |
| `JWT_SECRET` | Yes | Min 64-character HMAC-SHA256 key |
| `OPEN_AI_KEY` | Yes | OpenAI API key |
| `ANTHROPIC_API_KEY` | Yes | Anthropic API key |
| `WEBHOOK_HMAC_SECRET` | Yes | Shared secret for webhook HMAC validation |

---

## Database Migrations

Migrations live in `src/main/resources/db/migration/` and are applied automatically by Flyway on startup.

| Version | Description |
|---------|-------------|
| `V1` | Init schemas and tables |
| `V2` | Add indexes to `ai_jobs` and `budget_templates` |
| `V3` | Add `security` schema, `sse_tickets` table, profile columns, column type corrections |