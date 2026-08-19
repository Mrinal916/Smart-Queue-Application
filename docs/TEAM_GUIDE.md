# SmartQueue Team Guide

This is the technical handover document for SmartQueue. It describes the current application, how a teammate runs it, how its database is managed, and the safest way to extend it.

## Purpose

SmartQueue is a real-time virtual queue system for public-service locations such as RTO offices and hospitals. Citizens register and book appointment tokens. Officers operate assigned counters and move tokens through the queue. Administrators configure the service network, manage accounts, and review reports.

The Spring Boot application serves the REST API and browser application together. There is no separate frontend process or CORS setup for local development.

## Stack

| Concern | Implementation |
| --- | --- |
| Language/runtime | Java 21 |
| Framework | Spring Boot 4.1.0, MVC, Security, WebSocket |
| Build | Maven multi-module project |
| Durable data | PostgreSQL 18 / compatible PostgreSQL |
| Live queue state | Redis |
| Schema management | Flyway |
| Authentication | JWT bearer tokens |
| Messaging | STOMP/SockJS WebSocket |
| API reference | OpenAPI / Swagger UI |
| Reporting | CSV and XLSX, using Apache POI |
| Tests | Spring Boot Test with H2 in PostgreSQL compatibility mode |

## Repository layout

```text
.
├── pom.xml                         Parent Maven project
├── README.md                       Quick start and milestone summary
├── CHANGELOG.md                    Change history
├── PROJECT_PROGRESS.md             Current delivery status
├── docs/TEAM_GUIDE.md              This document
├── database/                       Empty placeholder; no database dump is committed
└── backend/
    ├── pom.xml                     Backend module dependencies
    └── src/
        ├── main/java/com/smartqueue/
        │   ├── auth/               Login, registration, JWT
        │   ├── office/, department/ and servicecatalog/
        │   ├── counter/, token/ and queue/
        │   ├── analytics/ and user/
        │   └── common/             Configuration, errors, health
        ├── main/resources/
        │   ├── application*.yml    Profile configuration
        │   ├── db/migration/       Flyway migrations
        │   └── static/             Browser UI
        └── test/                   Unit and integration tests
```

## Prerequisites

Install Java 21, Maven 3.9+, PostgreSQL, Redis, and Git. Verify the local tools with `java -version`, `mvn -version`, and `redis-cli ping`. Redis should reply with `PONG`.

## Local setup

Clone the private repository and enter it:

```powershell
git clone https://github.com/Tanmay052002/Digital-Queue-Management-Private.git
cd Digital-Queue-Management-Private
```

Create a dedicated local PostgreSQL account and database using an administrator connection:

```sql
CREATE ROLE smartqueue LOGIN PASSWORD 'choose-a-local-password';
CREATE DATABASE smartqueue OWNER smartqueue;
```

Set secrets for the current PowerShell session. These values are examples; use your own local password and a long random JWT secret.

```powershell
$env:DB_PASSWORD = 'choose-a-local-password'
$env:JWT_SECRET = 'use-a-long-random-development-secret-with-at-least-32-bytes'
```

Optional development overrides are `DB_URL`, `DB_USERNAME`, `REDIS_HOST`, `REDIS_PORT`, and `SERVER_PORT`. Defaults are PostgreSQL at `localhost:5432/smartqueue`, Redis at `localhost:6379`, and port 8080.

Start the application from the repository root:

```powershell
mvn -pl backend spring-boot:run
```

Useful local URLs:

| URL | Purpose |
| --- | --- |
| `http://localhost:8080/` | Browser application |
| `http://localhost:8080/api/v1/health` | Health check |
| `http://localhost:8080/swagger-ui.html` | Interactive API documentation |
| `http://localhost:8080/api-docs` | OpenAPI JSON |

## Configuration and deployment

The default Spring profile is `dev`. The `prod` profile requires explicit database, Redis, and JWT configuration. Activate it with `$env:SPRING_PROFILES_ACTIVE = 'prod'` followed by `mvn -pl backend spring-boot:run`.

| Variable | Development default | Production |
| --- | --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/smartqueue` | Required |
| `DB_USERNAME` | `smartqueue` | Required |
| `DB_PASSWORD` | Empty | Required |
| `REDIS_HOST` | `localhost` | Required |
| `REDIS_PORT` | `6379` | Defaults to 6379 |
| `JWT_SECRET` | Development fallback | Required |
| `JWT_EXPIRATION` | 15 days | Optional override |
| `QR_EXPIRATION` | 5 minutes | Optional override |

Production guidance: use a unique strong JWT secret, terminate TLS at the deployment boundary, restrict PostgreSQL and Redis network access, and configure database backups. Never store production credentials in Git or issue them in chat.

## Database rules

PostgreSQL is the durable source of truth. Redis holds live queue ordering and temporary queue state. Hibernate runs in validation mode (`ddl-auto: validate`) and never owns schema creation.

Flyway is the only schema-change mechanism. Migrations are located in `backend/src/main/resources/db/migration` and run in numeric order at startup.

| Versions | Content |
| --- | --- |
| V1-V2 | Roles, users, and seeded `CITIZEN`, `OFFICER`, `ADMIN` roles |
| V3 | Offices, departments, services, counters, and assignments |
| V4-V6 | Tokens, queue history, idempotency, waiting status |
| V7-V9 | Analytics indexes, counter history, integrity constraints |
| V10-V16 | Appointment data, booking details, priority, global numbers, breaks, appeared state |

Never edit a migration that has already been run in a shared database. Add the next numbered migration instead. Do not commit database dumps, database data files, or real user data. The database folder intentionally contains only a placeholder; teammates generate their schema by running Flyway.

## Roles and access

| Role | Main abilities |
| --- | --- |
| `CITIZEN` | Register, log in, book/cancel tokens, view own history and QR payload |
| `OFFICER` | Operate assigned counters, inspect assigned queues, advance token states |
| `ADMIN` | Manage offices, services, counters, assignments, users, and analytics |

Authentication is stateless. After login, send every protected request with `Authorization: Bearer <jwt-token>`. Public routes include registration, login, health, Swagger/OpenAPI, browser assets, and the WebSocket handshake. Security rules protect the remaining routes by role.

## Main user flows

### Citizen

1. Register using `POST /api/v1/auth/register`.
2. Log in at `POST /api/v1/auth/login` and save the JWT.
3. Query available appointment times, then book a token with an idempotency key.
4. View the active token, history, and estimated wait.
5. Retrieve the expiring signed QR payload and check in.
6. Cancel the token when it is still eligible for cancellation.

### Officer

1. An administrator assigns the officer to a counter and assigns services to the counter.
2. The officer opens the counter and views its queue summary.
3. The officer calls the next token and can skip, recall, complete, mark no-show, or mark appeared where the state machine permits.
4. Queue and counter updates are published only after the related database transaction commits.

### Administrator

1. Create offices, departments, services, and counters.
2. Configure officer-counter and counter-service assignments.
3. Manage users, including enablement, role updates, token history, and deletion.
4. Use dashboards, date-range reports, performance metrics, statistics, and CSV/XLSX exports.

## API navigation

Swagger UI is the authoritative endpoint and request/response reference. The high-level routes are:

| Area | Base route |
| --- | --- |
| Authentication | `/api/v1/auth` |
| Offices | `/api/v1/offices` |
| Departments | `/api/v1/departments` |
| Services | `/api/v1/services` |
| Counters | `/api/v1/counters` |
| Officer operations | `/api/v1/officer/counters` |
| Tokens | `/api/v1/tokens` |
| Live queue | `/api/v1/queue/live-status` |
| User directory | `/api/v1/users` |
| Reporting | `/api/v1/analytics` |

Important token operations include booking, availability, active token, history, wait time, QR generation/check-in, cancellation, next, skip, recall, complete, no-show, and appeared. Analytics includes dashboard, daily/weekly/monthly and date-range reports, office/service/counter/officer performance, queue statistics, and CSV/XLSX exports.

## Real-time messaging

SmartQueue uses STOMP/SockJS WebSocket updates for queue and counter events. The handshake endpoint is under `/ws/**`; the STOMP connection flow uses JWT authentication. Do not publish queue changes before their PostgreSQL transaction succeeds, or the browser may receive an update that is later rolled back.

## Testing

Run all tests from the repository root with `mvn clean test`. Then run `git diff --check` and `git status`. To run only the backend module, use `mvn -pl backend test`.

The test profile uses H2 in PostgreSQL compatibility mode. Current tests cover token state transitions plus selected role journey, authentication-filter, analytics-export, analytics-service, and Redis-reconciliation behaviour.

## Team workflow

1. Pull the latest `main` before starting work.
2. Use a focused feature branch and include tests with behaviour changes.
3. Add a new Flyway migration for every schema change.
4. Run the test suite before opening a pull request.
5. Never commit `.env`, passwords, JWT secrets, database dumps, `target/`, `.metadata/`, or IDE-generated project files.
6. Use code review before merging to `main`.

## Current delivery status

Completed: core queue engine, citizen booking and QR operations, JWT-secured real-time updates, counter management, user administration, and the first analytics/reporting implementation.

In progress or deferred: analytics/reporting completion, authenticated WebSocket browser integration tests, load and performance testing, wider QR and authorization edge-case coverage, and a final OpenAPI documentation review. See `CHANGELOG.md` and `PROJECT_PROGRESS.md` for the delivery history.
