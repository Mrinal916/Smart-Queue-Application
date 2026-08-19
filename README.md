# SmartQueue

SmartQueue is a real-time virtual queue management system, initially supporting Pune RTO and Sassoon General Hospital.

For architecture, setup, database, API, security, testing, and team workflow details, read the [Team Guide](docs/TEAM_GUIDE.md).

## Milestone status

### Milestone 0 — complete

- [x] Maven parent project and backend module
- [x] Java 21 and Spring Boot 4.1.0
- [x] Local PostgreSQL and Redis configuration
- [x] Flyway, OpenAPI, logging, global exception handling, and API response format
- [x] Feature-first package structure and local-development documentation
- [x] Verified with PostgreSQL 18, Redis (`PONG`), and an HTTP 200 health response

### Milestone 1 — authentication complete

- [x] Flyway-managed roles and users tables, with seeded CITIZEN, OFFICER, and ADMIN roles
- [x] BCrypt password hashing and email-based citizen registration
- [x] Stateless JWT login and bearer-token security filter
- [x] Consistent validation, duplicate-email, and invalid-credential responses
- [x] Verified registration and login against local PostgreSQL

### Milestone 3 — core queue engine complete

- [x] Token booking, cancellation, service queue operations, and queue history
- [x] Redis sorted-set live queues with PostgreSQL token persistence
- [x] Duplicate-booking prevention, idempotent booking keys, and pessimistic booking concurrency protection
- [x] Officer/counter authorization and formal token state-transition validation
- [x] Dynamic wait-time endpoint based on queue position and service duration
- [x] Unit and Flyway-backed application integration tests

QR check-in, WebSocket updates, and Redis failure recovery are intentionally scheduled for Milestones 5, 6, and 8.

### Milestone 5 — citizen operations complete

- [x] Citizen booking, cancellation, active-token, details, and paginated history APIs
- [x] Live queue/current-serving status and dynamic wait-time APIs
- [x] Signed, expiring QR payload generation and citizen-bound validation
- [x] Citizen authorization and duplicate-booking protection
- [x] Verified with Maven clean test, local PostgreSQL, Redis, health endpoint, and Swagger UI

Comprehensive API documentation and adversarial test matrices are scheduled for Milestone 8.

### Milestone 6 — real-time features complete

- [x] STOMP/SockJS WebSocket transport with JWT CONNECT authentication
- [x] Transaction-safe queue and counter event publishing
- [x] Officer office/service updates and citizen-specific notifications
- [x] Publisher routing test and clean Maven test suite

Authenticated SockJS/browser integration and load testing are scheduled for Milestone 8.

## Prerequisites

- IntelliJ IDEA Community Edition
- Java 21
- Maven 3.9+
- PostgreSQL 18 installed locally
- Redis installed locally
- Git
- Postman

## Local services

PostgreSQL must run on port `5432` and Redis on port `6379`. No Docker runtime is used for local development.

Create the development role and database once with a PostgreSQL administrator account:

    CREATE ROLE smartqueue LOGIN PASSWORD 'choose-a-local-password';
    CREATE DATABASE smartqueue OWNER smartqueue;

Set the database password in the shell that starts the application:

    $env:DB_PASSWORD = 'choose-a-local-password'

## Run locally

### 1. Start Local Infrastructure
Ensure **PostgreSQL** is running on port `5432` and **Redis** is running on port `6379`.

Create the development database once (if not already done):
```sql
CREATE ROLE smartqueue LOGIN PASSWORD 'choose-a-local-password';
CREATE DATABASE smartqueue OWNER smartqueue;
```

Set the database password in PowerShell:
```powershell
$env:DB_PASSWORD = 'choose-a-local-password'
```

### 2. Start the .NET 8 Email Notification Service
In Terminal / PowerShell window 1:
```powershell
dotnet run --project notification-service/SmartQueue.NotificationService.csproj
```
> Listening at `http://localhost:5050` (Swagger UI: `http://localhost:5050/swagger`).

### 3. Start the Spring Boot Backend & Web App
In Terminal / PowerShell window 2:
```powershell
$env:DB_PASSWORD = 'choose-a-local-password'
mvn -pl backend spring-boot:run
```
> Web Application: `http://localhost:8080/`
> Health Endpoint: `http://localhost:8080/api/v1/health`
> OpenAPI Docs: `http://localhost:8080/swagger-ui.html`

### Open the web app from another device on the same Wi-Fi/LAN

Keep PostgreSQL, Redis, and the two application processes running on this computer. Find its IPv4 address with:

```powershell
Get-NetIPAddress -AddressFamily IPv4 | Where-Object { $_.IPAddress -notlike '127.*' -and $_.IPAddress -notlike '169.254*' }
```

Then, from the other device connected to the same network, open `http://<this-computer-ip>:8080/` (on this computer it is currently `http://192.168.10.34:8080/`). The frontend uses the same address automatically, including its live WebSocket connection.

If Windows asks to allow Java through the firewall, allow it on **Private networks**. If no prompt appears, open an elevated PowerShell once and run:

```powershell
New-NetFirewallRule -DisplayName 'SmartQueue web app (LAN)' -Direction Inbound -Action Allow -Protocol TCP -LocalPort 8080 -Profile Private
```

For password-reset emails generated while sharing on the LAN, start the backend with the LAN URL so those links point to the reachable computer:

```powershell
$env:PASSWORD_RESET_URL = 'http://192.168.10.34:8080/reset-password'
mvn -pl backend spring-boot:run
```

---

## E2E Testing & HTML Reports (Playwright)

To run Playwright automated end-to-end tests:
```powershell
npm test
# OR
npx playwright test
```

To view the interactive HTML test report:
```powershell
npm run test:report
# OR
npx playwright show-report
```

---

## Web application

The backend serves the SmartQueue web application from the same origin at `http://localhost:8080/`.
No separate frontend process or CORS configuration is required. Register a citizen account from the landing page, then sign in to access citizen workflows. Seeded `OFFICER` and `ADMIN` accounts receive counter and administration views according to their existing JWT role.

## Configuration

The development profile accepts `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `REDIS_HOST`, `REDIS_PORT`, `SERVER_PORT`, and `NOTIFICATION_SERVICE_URL` environment overrides.

To configure real Twilio / SendGrid email dispatches, update `ApiKey` in [`notification-service/appsettings.json`](file:///d:/DAC-FINAL%20PROJECT/Smart%20Queue/notification-service/appsettings.json).

Flyway is the only mechanism used to change the database schema. Application-managed JPA DDL is disabled by validation mode.
