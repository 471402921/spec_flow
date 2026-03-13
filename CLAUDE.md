# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
# Build (full project)
./mvnw clean package -DskipTests

# Build specific module
./mvnw package -pl soulpal-api -am

# Run all tests (uses H2 in-memory DB, no external services needed)
./mvnw clean test

# Run a single test class
./mvnw test -Dtest=SessionServiceTest

# Run a single test method
./mvnw test -Dtest=SessionServiceTest#testCreateSession

# Checkstyle (fails build on warnings)
./mvnw checkstyle:check

# Coverage report (output: **/target/site/jacoco/)
./mvnw clean test jacoco:report
```

## Architecture: DDD Light Modular Monolith

Java 21 + Spring Boot 3.4.2 + Maven multi-module. Three modules: `soulpal-api` (REST service), `soulpal-worker` (async tasks, placeholder), `soulpal-common` (shared code). Current business modules in `soulpal-api`: `auth` (sessions/tokens), `user` (users, pets, verification), `family` (families, members, invitations).

### Shared Infrastructure

- **`soulpal-common`**: `Result<T>` unified response wrapper, exception hierarchy (`BusinessException`, `NotFoundException`, `AuthenticationException`)
- **`soulpal-api/config/`**: `GlobalExceptionHandler` (maps exceptions → `Result<T>` with HTTP status), `TraceIdInterceptor` (MDC-based `traceId` injected into all logs and `X-Trace-Id` response header), `AuthInterceptor` (Bearer token validation), `SecurityConfig` (BCrypt password encoder), `OpenApiConfig`. Both interceptors registered in `WebConfig`
- All controllers return `Result<T>` — use `Result.success(data)` / `Result.failure(code, msg)`
- Business config namespace: `soulpal.*` (e.g. `soulpal.session.expiration-days`)

### Module Layout (per business module in soulpal-api)

Each business module under `soulpal-api/src/main/java/com/soulpal/api/modules/{module}/` follows four layers:

```
interfaces/          # Controllers, DTOs (Request/Response)
application/         # Service classes, @Transactional boundaries
domain/              # Entities, repository interfaces, domain services — pure POJOs, NO Spring/ORM annotations
infrastructure/      # Repository impls, DO classes (MyBatis-Plus @TableName), converters, plus optional subdirs (e.g. email/)
```

**Critical rule**: The `domain/` layer must be framework-agnostic — no `@Component`, `@Autowired`, `@TableName`, or any Spring/MyBatis annotations. Domain entities are pure Java.

### Key Patterns

- **DO/Entity separation**: `SessionDO` (database, `LocalDateTime`) ↔ `Session` (domain, `Instant`) via `SessionConverter`
- **Repository pattern**: Interface in `domain/repository/`, implementation in `infrastructure/persistence/`
- **ID generation**: `ASSIGN_UUID` (MyBatis-Plus auto-generated UUIDs)
- **Soft deletes**: MyBatis-Plus `logic-delete-field=deleted` with `deleted_at` timestamp for 30-day retention
- **Timezone**: Always use `ZoneId.of("UTC")` explicitly, never `ZoneId.systemDefault()`
- **Authentication**: `AuthInterceptor` validates Bearer tokens via `TokenProvider`, stores `userId` in request attributes; public paths configured in `EXCLUDE_PATHS`. Controllers access it via `(String) request.getAttribute("userId")`
- **Security**: Use `PasswordEncoder` bean from `SecurityConfig` for password hashing (BCrypt)

## Logging

`logback-spring.xml` includes `traceId` in all log entries via `[%X{traceId}]`. File rolling: 100MB max, 30 days retention.

## Configuration

- Profiles: `dev` (default), `prod`, `test`
- All configs use `${ENV_VAR:default}` placeholders for Docker compatibility
- Production config has **no defaults** for sensitive values (DB password, etc.)
- Test profile: H2 in-memory with `MODE=PostgreSQL`, Flyway disabled, schema from `schema-h2.sql`
- Swagger UI: `/swagger-ui/index.html`
- Health check: `/actuator/health`

## Code Quality

- **Checkstyle**: Google Java Style (simplified) at `config/checkstyle.xml`. Runs at `validate` phase — all warnings fail the build.
- **Key rules**: no star imports, no unused imports, braces required, `@Override` required, max method length 150 lines, max 7 parameters.
- **Lombok**: Use `@RequiredArgsConstructor` (final fields), `@Data` (DTOs), `@Slf4j` (logging). Prefer primitive `boolean` over `Boolean` wrapper. Note: `Boolean` wrapper generates `getXxx()`, primitive `boolean` generates `isXxx()`.
- **Null safety**: Use `@NonNull` from `org.springframework.lang` for constructor parameters; validate with `Objects.requireNonNull()`

## CI Pipeline

GitHub Actions (`.github/workflows/ci.yml`): three sequential jobs — checkstyle → test → package. Fail-fast (later jobs only run if prior ones pass).

## Deployment

- Target: home-node (WSL2/Ubuntu) via `ssh home-node`
- Docker Compose in `deploy/`: postgres:16, redis:7-alpine, api
- Deploy: `ssh home-node "cd /srv/soulpal-service/deploy && bash deploy.sh"` (git pull → compose down → rebuild → health check)
- Public access: `api.soulpal.me` via Cloudflare Tunnel
- Docker services use healthcheck dependencies (api waits for postgres + redis to be healthy)

## Testing Conventions

- Unit tests: `@ExtendWith(MockitoExtension.class)`, mock repositories
- Integration tests: `@SpringBootTest` + `@AutoConfigureMockMvc` + `@ActiveProfiles("test")`
- For `@Value` injection in `@InjectMocks` tests: use `ReflectionTestUtils.setField()`
- Test files named `*Test.java` or `*Tests.java`
- Mock `TokenProvider` when testing authenticated endpoints in unit tests

## Database Migrations

Flyway migrations in `soulpal-api/src/main/resources/db/migration/V*.sql`. Baseline-on-migrate enabled for existing databases. PostgreSQL tables use a trigger for auto-updating `updated_at` columns; the H2 test schema (`schema-h2.sql`) omits triggers and simplifies indexes for compatibility. **When adding a new Flyway migration, always update `schema-h2.sql` to keep tests in sync.**

## Development Skills (Claude Code)

Custom skills available via slash commands:

- `/soulpal-code-review` — Architecture compliance, Spring Boot practices, type safety, security
- `/soulpal-test-gen` — Auto-generate unit + integration tests for a business module
- `/soulpal-doc-prd` — Write or review PRD-Lite documents
- `/soulpal-doc-techpack` — Generate Tech Pack from finalized PRD
- `/soulpal-doc-review` — Cross-document completeness and consistency check
- `/soulpal-module-gen` — Scaffold a new business module from Tech Pack (DDD Light 4-layer structure)
- `/soulpal-deploy` — Deployment operations: deploy, status, logs, restart, rollback, DB backup

## Development Workflow

The project follows a 4-stage AI-automation workflow documented in `doc/process/ai-automation-dev-workflow.md`: PRD-Lite → Tech Pack → Implementation → Review. Requirements live in `doc/requirements/`, designs in `doc/design/{module}/`. Operational runbook at `deploy/RUNBOOK.md`.
