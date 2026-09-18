# FinVault — Digital Wallet & Payment Platform

A production-style **fintech backend** built with the complete **Java Spring Boot stack**, designed as a portfolio/CV project demonstrating enterprise backend skills.

## What It Does

FinVault is a digital wallet platform where users can:

- **Register & authenticate** with JWT-based security
- **View wallet balance** (cached in Redis)
- **Deposit funds** into their wallet
- **Transfer money** peer-to-peer between wallets
- **View transaction history** with pagination
- Receive **async notifications** via Apache Kafka

## Tech Stack (Full Spring Boot Ecosystem)

| Layer | Technology |
|-------|-----------|
| **Core** | Spring Boot 3.2, Spring Framework (DI, MVC, AOP) |
| **Web / REST** | Spring Web, Jackson, Jakarta Validation |
| **Data** | Spring Data JPA, Hibernate, PostgreSQL, Flyway |
| **Security** | Spring Security, JWT (jjwt), BCrypt |
| **Cache** | Redis, Spring Cache |
| **Messaging** | Apache Kafka, Spring Kafka |
| **API Docs** | Springdoc OpenAPI / Swagger UI |
| **Observability** | Spring Boot Actuator, Prometheus, SLF4J + Logback |
| **Build** | Maven |
| **Testing** | JUnit 5, Mockito, Spring Boot Test, Testcontainers |
| **DevOps** | Docker, Docker Compose, GitHub Actions CI |

## Architecture

```
┌─────────────┐     ┌──────────────────────────────────────────────┐
│   Client    │────▶│              Spring Boot API                  │
│ (Swagger/UI)│     │  Controllers → Services → Repositories (JPA)  │
└─────────────┘     └──────┬───────────────┬───────────────┬───────┘
                           │               │               │
                    ┌──────▼──────┐ ┌──────▼──────┐ ┌──────▼──────┐
                    │ PostgreSQL  │ │    Redis    │ │    Kafka    │
                    │  (Flyway)   │ │   (Cache)   │ │ (Events)    │
                    └─────────────┘ └─────────────┘ └─────────────┘
```

## Project Structure

```
finvault/
├── src/main/java/com/finvault/
│   ├── config/          # Redis, Kafka, OpenAPI configuration
│   ├── controller/      # REST API endpoints
│   ├── dto/             # Request/Response DTOs
│   ├── entity/          # JPA entities
│   ├── event/           # Kafka event models
│   ├── exception/       # Global exception handling
│   ├── mapper/          # Entity ↔ DTO mapping
│   ├── messaging/       # Kafka producers & consumers
│   ├── repository/      # Spring Data JPA repositories
│   ├── security/        # JWT filter, Spring Security config
│   └── service/         # Business logic (@Transactional)
├── src/main/resources/
│   ├── db/migration/    # Flyway SQL migrations
│   └── application*.yml
├── src/test/            # Unit & integration tests
├── docker-compose.yml   # Full stack (Postgres, Redis, Kafka)
├── Dockerfile
└── .github/workflows/   # CI pipeline
```

## Quick Start

### Prerequisites

- Java 17+
- Maven 3.9+
- Docker & Docker Compose (for full stack)

### Option 1: Docker Compose (recommended)

```bash
cd finvault
docker compose up -d
```

API available at: **http://localhost:8080**  
Swagger UI: **http://localhost:8080/swagger-ui.html**

### Option 2: Local development

Start infrastructure only:

```bash
docker compose up -d postgres redis zookeeper kafka
```

Run the app:

```bash
mvn spring-boot:run
```

## API Endpoints

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/api/v1/auth/register` | Register + create wallet | No |
| POST | `/api/v1/auth/login` | Login, get JWT | No |
| GET | `/api/v1/wallets/me` | Get my wallet balance | JWT |
| POST | `/api/v1/transactions/deposit` | Deposit funds | JWT |
| POST | `/api/v1/transactions/transfer` | P2P transfer | JWT |
| GET | `/api/v1/transactions` | Transaction history | JWT |
| GET | `/actuator/health` | Health check | No |
| GET | `/actuator/prometheus` | Metrics (Admin) | JWT |

## Example Usage

```bash
# 1. Register
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@example.com","password":"password123","firstName":"Alice","lastName":"Smith"}'

# 2. Login (save the accessToken)
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@example.com","password":"password123"}'

# 3. Deposit
curl -X POST http://localhost:8080/api/v1/transactions/deposit \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"amount":500.00,"description":"Salary"}'

# 4. Check balance
curl http://localhost:8080/api/v1/wallets/me \
  -H "Authorization: Bearer <TOKEN>"
```

## Running Tests

```bash
mvn clean verify
```

Tests include:
- **Unit tests** (Mockito) for auth service
- **Controller tests** (MockMvc) for REST layer
- **Integration tests** (Testcontainers PostgreSQL + Embedded Kafka)

## CV / Interview Talking Points

When presenting this project, highlight:

1. **Transactional integrity** — Pessimistic locking on wallets prevents race conditions during transfers
2. **Event-driven architecture** — Kafka publishes transaction/notification events for decoupled processing
3. **Caching strategy** — Redis caches wallet lookups; cache eviction on balance changes
4. **Security** — Stateless JWT auth, role-based access, BCrypt password hashing
5. **Database migrations** — Flyway version-controlled schema (production-ready)
6. **Observability** — Actuator health checks + Prometheus metrics endpoint
7. **Containerization** — Multi-stage Docker build, full docker-compose stack
8. **CI/CD** — GitHub Actions pipeline with PostgreSQL service container

## License

MIT
