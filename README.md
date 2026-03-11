# ReLab Budget — Spring Boot Backend

Modular Monolith + Event-Driven AI · Spring Boot 3.3 · PostgreSQL · Spring AI (Open AI)

---

## Prerequisites

| Tool                    | Version         |
|-------------------------|-----------------|
| Java                    | 21+             |
| Gradle                  | 9.4.0           |
| Docker & Docker Compose | 24+             |
| PostgreSQL              | 16 (via Docker) |

---

## Quick Start

### 1. Clone and configure environment

```bash
cp .env.example .env.dev
# Edit .env and fill in:
#   OPEN_AI_KEY=sk-ant-...
#   JWT_SECRET=<64+ char random string>
#   WEBHOOK_HMAC_SECRET=<shared with Payment ClientB>
```

### 2. Start infrastructure

```bash
docker compose up -d
# Starts PostgreSQL on :5432 and Redis on :6379
```

### 3. Run the application

```bash
# With Gradle wrapper (recommended)
set -a && source .env.dev && set +a && ./gradlew bootRun -Dspring.profiles.active=dev &
```

Flyway will automatically run `V1__init_schemas_and_tables.sql` on first startup.

The API will be available at `http://localhost:8080/api/v1`

---

## Project Structure

```
src/main/java/com/relab/budget/
├── RelabBudgetApplication.java     # Entry point
├── identity/                       # Auth, users, JWT
├── profile/                        # Financial survey profile
├── advisor/                        # AI template generation (async)
├── budget/                         # Budget & saved configurations
├── pocket/                         # Pocket lifecycle
├── wallet/                         # Deposits, payment requests
├── recurring/                      # Scheduled payments
├── notification/                   # Event-driven notifications
└── shared/
    ├── config/                     # Async, cache config
    ├── security/                   # JWT filter, SecurityConfig
    ├── response/                   # ApiResponse, BusinessException
    └── util/                       # HMAC util, date helpers
```

---

## Module Dependency Rules

- Modules **never** import each other's JPA repositories
- Cross-module reads use a **public service interface** in the target module
- Cross-module state changes use **Spring domain events** (`ApplicationEventPublisher`)
- The `notification` module is **consumer-only** — it never publishes events

---

## Key Dependencies

| Dependency | Purpose |
|------------|---------|
| `spring-boot-starter-web` | REST API |
| `spring-boot-starter-data-jpa` | JPA / Hibernate |
| `spring-boot-starter-security` | Auth filter chain |
| `spring-ai-anthropic-spring-boot-starter` | Claude AI integration |
| `spring-modulith-starter-core` | Module boundary enforcement |
| `flyway-core` | Database migrations |
| `jjwt-api` | JWT generation & validation |
| `spring-boot-starter-data-redis` | Distributed locks (scheduler) |
| `mapstruct` | Entity ↔ DTO mapping |
| `lombok` | Boilerplate reduction |
| `testcontainers` | Integration tests with real PostgreSQL |

---

## Environment Variables

See `.env.example` for the full list. The three **required** variables are:

- `OPENAI_API_KEY` — Open AI API key
- `JWT_SECRET` — Minimum 64-character HMAC-SHA256 key
- `WEBHOOK_HMAC_SECRET` — Shared secret with Payment Client

---


---

## API Base URL

```
http://localhost:8080/api/v1
```

See the **ReLab Budget API Contract** document for the full endpoint reference.
