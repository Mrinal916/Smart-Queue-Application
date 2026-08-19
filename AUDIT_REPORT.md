# SmartQueue Backend Architectural Audit

Audit date: 2026-07-28  
Scope: backend source, Flyway migrations, configuration, tests, README/OpenAPI, and current uncommitted Milestone 7 work. No production code was changed for this audit.

## Overall score: 57/100

| Area | Score | Assessment |
|---|---:|---|
| Architecture | 66 | Sensible feature packages and layers; several services are overly coupled and compressed. |
| Security | 61 | JWT, BCrypt, and route rules exist; authorization and security tests are insufficient. |
| Performance | 52 | Useful indexes and Redis ZSETs; atomicity, query portability, and N+1 protections need work. |
| Maintainability | 48 | DTO discipline is good, but one-line classes, wildcard imports, duplicated mapping, and limited documentation hurt maintainability. |
| Database | 63 | UUID public IDs, audit fields, FKs, constraints, and migrations are good; lifecycle/history/index coverage is incomplete. |
| Testing | 31 | Eight tests pass, but there are no meaningful database, authorization, queue-concurrency, Redis-failure, or WebSocket integration tests. |

## Architecture

The feature-oriented package layout (`auth`, `office`, `department`, `service`, `counter`, `queue`, `token`, `websocket`, `analytics`) is appropriate for a modular monolith. Controllers generally use DTOs, services own transactions, repositories encapsulate persistence, and common concerns are centralized.

Concerns:

- `QueueEngineService` spans booking, QR, citizen queries, queue mutation, Redis coordination, authorization, history, and event publication. It violates single-responsibility and is the principal candidate for decomposition.
- `CounterService` combines CRUD, assignment management, state transitions, audit-event persistence, and event publication. Its current one-line formatting makes review and safe change difficult.
- The `service` package name is legal but ambiguous next to Spring service classes. `catalog` or `servicecatalog` would make boundaries clearer.
- No direct compile-time circular dependency was found, but `queue` depends on auth, counter, token, service, user, Redis, and WebSocket events. That is a high coupling hotspot.
- Business rules are mostly embedded in services rather than expressed as domain policies/specifications, limiting reuse and focused tests.

## Database

Strengths:

- Flyway owns schema evolution; relational FKs and core unique constraints are present.
- Public UUIDs are consistently exposed while internal numeric IDs remain relational keys.
- `AuditableEntity` supplies created/updated timestamps and public UUIDs to principal entities.
- Child-to-parent associations explicitly use `LAZY`, which is the correct default.
- Soft-delete flags are used for offices, departments, services, and counters.

Findings:

- Foreign-key relationships in `Token` and `QueueHistory` omit `optional = false` even where migrations declare `NOT NULL`; make JPA metadata match the database.
- There are no parent-side collections/cascade rules. This avoids accidental deletion, but lifecycle behavior is implicit and requires service-level enforcement.
- Soft deletes are inconsistent: assignments are released/disabled, while history is permanent; users/roles have no documented lifecycle strategy. Define a uniform retention and archival policy.
- `CounterStatusHistory` has no UUID or audit fields, unlike most domain records. Either make it a true append-only event table with actor/source metadata, or align it with the audit convention.
- Needed indexes remain: token status/counter queries, active assignment uniqueness, queue history state/time reporting, and partial indexes for active records in PostgreSQL.
- The new analytics SQL uses PostgreSQL-specific `extract`, `interval`, and CTE behavior while tests use H2. It is valid as a PostgreSQL design decision but must be tested against PostgreSQL.

## Redis / queue engine

Strengths:

- Key pattern is understandable: `<prefix>:<service UUID>:<queue date>`.
- A sorted set keyed by token number is a suitable live queue representation.
- `popMin` is atomic within Redis, reducing duplicate `NEXT` at the Redis command level.

Critical findings:

- `next()` removes from Redis before the database transition commits. A database failure after `popMin` loses the live-queue member; a Redis failure can leave database and cache inconsistent.
- Redis operations are not enclosed in a Lua script or coordinated with an outbox/reconciliation mechanism. Atomic Redis commands do not make the Redis-plus-PostgreSQL workflow atomic.
- No TTL is set for daily queue keys. Expired operational data accumulates indefinitely.
- No startup reconciliation, fallback read path, retry policy, circuit breaker, or Redis-unavailable behavior exists. These are deferred work but are production blockers.
- `skip()` re-adds a token using its original score; depending on policy, it may immediately return to its previous position rather than the intended queue position.

## Security

Strengths:

- BCrypt password hashing, stateless JWT configuration, JWT filter, role-based request matching, and a WebSocket CONNECT interceptor are present.
- Analytics routes are restricted to `ROLE_ADMIN`; citizen ownership checks exist in several token operations.
- DTOs prevent password hash exposure.

Findings:

- Authorization is mainly service-code checks plus URL rules; no method security (`@PreAuthorize`) provides a second enforcement boundary.
- `/ws/**` is HTTP-permitted and relies wholly on the STOMP interceptor. This is acceptable only with comprehensive interceptor integration tests, which are absent.
- JWT secret handling allows a development default. Production configuration must fail fast if `JWT_SECRET` is absent/weak.
- QR expiry is hard-coded in `QueueEngineService` despite a configured QR expiration property.
- There is no visible account-lockout/rate-limit policy for login, refresh-token/revocation strategy, password policy, CORS policy, or security headers policy.
- No authorization test proves a citizen/officer/admin cannot invoke another role's endpoint.

## REST APIs

Strengths:

- Versioned `/api/v1` paths, feature-oriented resources, response DTOs, and `ApiResponse` are consistent.
- Global exception handling provides a central error shape.

Findings:

- Verify all write endpoints return intentional HTTP statuses. `ApiResponse.success` alone can easily result in `200 OK` for creates/deletes where `201/204` are preferable.
- Analytics uses native `Long` IDs as filters while public APIs otherwise use UUIDs; this leaks internal IDs and is inconsistent.
- Several controller methods return `ApiResponse<?>`; replace these with exact generic DTOs everywhere (some analytics endpoints were corrected, but audit the entire API).
- Swagger has operation summaries but lacks request/response examples, error schemas per endpoint, and a documented event contract for WebSocket topics.
- Health is security-protected; operational health/readiness endpoints should be deliberately separated and documented.

## Performance

- Mapping lazy entity relationships inside streams can produce N+1 queries in list/history responses. Add fetch joins, entity graphs, or projection queries and measure them.
- Analytics performs several independent aggregate queries per request. Combine related aggregates where it materially reduces round trips; keep queries PostgreSQL-tested and indexed.
- `QueueEngineService.waitTime()` counts relational rows each call; acceptable initially, but cache or pre-aggregate under load with invalidation tied to successful transitions.
- Transaction boundaries are broadly correct, but external Redis calls occur inside database transactions. Use transactional outbox/after-commit cache updates or resilient reconciliation.
- Add query timeouts and pagination limits. Current citizen page size needs a maximum bound.

## Testing

Current coverage is eight tests: application context, token-state transitions, WebSocket publisher routing, basic analytics service behavior, and export generation.

Missing critical coverage:

- Testcontainers PostgreSQL + Redis integration tests for migrations and native analytics SQL.
- Concurrent booking and concurrent `NEXT` tests.
- Redis/database failure, retry, and reconciliation tests.
- Authorization matrix tests for citizen/officer/admin and cross-tenant ownership.
- Controller tests for status codes, validation, error bodies, pagination boundaries, and OpenAPI output.
- QR expiry/signature/ownership/check-in-state tests.
- Auth login/password/JWT expiry tests and authenticated STOMP tests.

## Code quality and documentation

- Reformat compressed single-line classes, especially services/entities. This is the highest maintainability concern.
- Remove wildcard imports and replace `var` only where it does not obscure query/result types.
- Extract repeated `map(Token)` and ownership/authorization checks into focused mappers/policies.
- README contains stale milestone claims (for example, it describes later features as intentionally deferred); synchronize it with the actual implementation.
- Add JavaDoc to public service contracts, queue invariants, Redis key format, transaction boundaries, and analytics SQL assumptions.
- Add an architecture decision record (ADR) set for UUID exposure, Redis-as-live-queue, transaction/outbox strategy, and soft deletion.

## Top 20 improvements (priority ranked)

1. **P0** — Make Redis/PostgreSQL queue transitions recoverable with an outbox plus reconciliation worker; never silently lose a token after `popMin`.
2. **P0** — Add Testcontainers integration tests against PostgreSQL and Redis for booking, `NEXT`, failures, and migrations.
3. **P0** — Add concurrency protection at the database layer for active-token booking and queue state transitions (locking/conditional updates).
4. **P0** — Define and implement Redis outage behavior, retries/timeouts, startup reconciliation, and daily-key TTL.
5. **P0** — Add authorization matrix tests for every citizen, officer, and admin API.
6. **P0** — Enforce production JWT secret validation; remove all unsafe production defaults.
7. **P1** — Split `QueueEngineService` into booking, citizen-token query, queue-operation, QR, and queue-projection services.
8. **P1** — Add rate limiting/account lockout and explicit CORS/security-header policy.
9. **P1** — Replace internal numeric analytics IDs in public query parameters with public UUIDs.
10. **P1** — Add database-level partial/unique constraints for active officer-counter assignments and active citizen-office tokens.
11. **P1** — Add fetch plans/projections to eliminate potential N+1 reads.
12. **P1** — Refactor one-line classes and wildcard imports; apply formatter and static analysis.
13. **P1** — Persist actor/source metadata for counter status history and backfill a clear status-event model.
14. **P1** — Move hard-coded QR expiry to `JwtProperties.qrExpiration`.
15. **P1** — Add a max page size and validate all pagination inputs.
16. **P2** — Consolidate analytics aggregates and test native SQL on PostgreSQL for empty and large datasets.
17. **P2** — Add OpenAPI examples, error responses, WebSocket event documentation, and API contract tests.
18. **P2** — Create ADRs and a current architecture/data-flow document.
19. **P2** — Define retention/archive policies for queue, token, counter-status, and audit data.
20. **P2** — Add observability: correlation IDs, metrics for queue latency/failures, structured audit logs, and alert thresholds.

## Technical debt

- Redis is treated as a live queue without a durable reconciliation design.
- Analytics is partly PostgreSQL-specific but lacks PostgreSQL integration coverage.
- Boundary/API documentation lags code; README milestone state is stale.
- Test suite provides low confidence for authorization, distributed consistency, and concurrency.
- Several services are difficult to review due to compressed formatting and mixed responsibilities.

## Production readiness checklist

- [ ] PostgreSQL/Redis Testcontainers integration suite passes in CI.
- [ ] Queue outbox/reconciliation/retry/TTL strategy implemented and chaos-tested.
- [ ] Concurrency tests prove no duplicate booking or duplicate `NEXT` under load.
- [ ] JWT secret validation, rotation/revocation policy, rate limits, CORS, and security headers configured.
- [ ] Complete role/ownership authorization matrix tested.
- [ ] Migrations tested on a fresh PostgreSQL database and upgrade path.
- [ ] Query plans reviewed; required indexes and pagination bounds verified.
- [ ] Structured logs, metrics, tracing/correlation IDs, backups, and alerting configured.
- [ ] OpenAPI examples/error responses and WebSocket contracts reviewed.
- [ ] Disaster recovery, Redis restart reconciliation, retention, and data privacy policies documented and exercised.

## Audit conclusion

The project is a promising local-development modular monolith with a sound basic domain shape. It is **not production-ready** because distributed queue consistency, failure recovery, authorization assurance, and integration/concurrency testing are incomplete. Address the P0 items before deploying beyond controlled development environments.
