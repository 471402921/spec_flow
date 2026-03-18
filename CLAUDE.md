# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What is SpecFlow

SpecFlow is a **Spec-Driven / Agentic Engineering** development framework — an opinionated scaffold for building Java backend services using DDD Light architecture, with AI-native development workflow (PRD → Tech Pack → Implementation → Review) baked in via Claude Code skills.

## Build & Test Commands

```bash
# Build (full project)
./mvnw clean package -DskipTests

# Build specific module
./mvnw package -pl specflow-api -am

# Run all tests (uses H2 in-memory DB, no external services needed)
./mvnw clean test

# Run a single test class
./mvnw test -Dtest=AuthInterceptorTest

# Run a single test method
./mvnw test -Dtest=AuthInterceptorTest#preHandle_withValidToken_shouldPassAndInjectUserId

# Checkstyle (fails build on warnings)
./mvnw checkstyle:check

# Coverage report (output: **/target/site/jacoco/)
./mvnw clean test jacoco:report
```

> Note: 需要 Java 21 运行环境。构建和测试可在本地或 Docker 中完成。

## Architecture: DDD Light Modular Monolith

Java 21 + Spring Boot 3.4.2 + Maven multi-module. Three modules: `specflow-api` (REST service), `specflow-worker` (async tasks, **placeholder — 当前不接受业务代码，未来用于异步任务处理**), `specflow-common` (shared code).

### Shared Infrastructure

- **`specflow-common`**: `Result<T>` unified response wrapper, exception hierarchy (`BusinessException`, `NotFoundException`, `AuthenticationException`)
- **`specflow-api/config/`**: `GlobalExceptionHandler` (maps exceptions → `Result<T>` with HTTP status), `TraceIdInterceptor` (MDC-based `traceId` injected into all logs and `X-Trace-Id` response header), `AuthInterceptor` (Bearer token validation via `TokenProvider` interface), `SecurityConfig` (BCrypt password encoder), `OpenApiConfig`, `NoOpTokenProvider` (default fallback). Both interceptors registered in `WebConfig`
- All controllers return `Result<T>` — use `Result.success(data)` / `Result.failure(code, msg)`

### Module Layout (per business module in specflow-api)

Each business module under `specflow-api/src/main/java/com/specflow/api/modules/{module}/` follows four layers:

```
interfaces/          # Controllers, DTOs (Request/Response)
application/         # Service classes, @Transactional boundaries
domain/              # Entities, repository interfaces, domain services — pure POJOs, NO Spring/ORM annotations
infrastructure/      # Repository impls, DO classes (MyBatis-Plus @TableName), converters, plus optional subdirs (e.g. email/)
```

**Critical rule**: The `domain/` layer must be framework-agnostic — no `@Component`, `@Autowired`, `@TableName`, or any Spring/MyBatis annotations. Domain entities are pure Java.

### Key Patterns

- **DO/Entity separation**: `XxxDO` (database, `LocalDateTime`) ↔ `Xxx` (domain, `Instant`) via `XxxConverter`
- **Repository pattern**: Interface in `domain/repository/`, implementation in `infrastructure/persistence/`
- **ID generation**: `ASSIGN_UUID` (MyBatis-Plus auto-generated UUIDs)
- **Soft deletes**: MyBatis-Plus `logic-delete-field=deleted` with `deleted_at` timestamp for 30-day retention
- **Timezone**: Always use `ZoneId.of("UTC")` explicitly, never `ZoneId.systemDefault()`
- **Authentication**: `AuthInterceptor` validates Bearer tokens via `TokenProvider` interface (in `config/` package), stores `userId` in request attributes. Business modules implement `TokenProvider` to provide token resolution; `NoOpTokenProvider` is the default fallback (rejects all tokens). Controllers access userId via `(String) request.getAttribute("userId")`
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

## Deployment

- Docker Compose in `deploy/`: postgres:16, redis:7-alpine, api
- Deploy script: `deploy/deploy.sh` (git pull → compose down → rebuild → health check)
- HTTPS via Nginx + Let's Encrypt (see `deploy/RUNBOOK.md`)
- Docker services use healthcheck dependencies (api waits for postgres + redis to be healthy)
- Operational runbook: `deploy/RUNBOOK.md`
- Server-specific config (SSH alias, cloud provider) should be maintained in user's local memory, not in this file

## Testing Conventions

- Unit tests: `@ExtendWith(MockitoExtension.class)`, mock repositories
- Integration tests: `@SpringBootTest` + `@AutoConfigureMockMvc` + `@ActiveProfiles("test")`
- For `@Value` injection in `@InjectMocks` tests: use `ReflectionTestUtils.setField()`
- Test files named `*Test.java` or `*Tests.java`
- Mock `TokenProvider` when testing authenticated endpoints in unit tests

## Database Migrations

Flyway migrations in `specflow-api/src/main/resources/db/migration/V*.sql`. Baseline-on-migrate enabled for existing databases. PostgreSQL tables use a trigger for auto-updating `updated_at` columns; the H2 test schema (`schema-h2.sql`) omits triggers and simplifies indexes for compatibility. **When adding a new Flyway migration, always update `schema-h2.sql` to keep tests in sync.**

## Development Skills (Claude Code)

Custom skills available via slash commands:

- `/specflow-code-review` — Architecture compliance, Spring Boot practices, type safety, security
- `/specflow-test-gen` — Auto-generate unit + integration tests for a business module
- `/specflow-doc-prd` — Write or review PRD-Lite documents
- `/specflow-doc-techpack` — Generate Tech Pack from finalized PRD
- `/specflow-doc-review` — Cross-document completeness and consistency check
- `/specflow-module-gen` — Scaffold a new business module from Tech Pack (DDD Light 4-layer structure)
- `/specflow-deploy` — Deployment operations: deploy, status, logs, restart, rollback, DB backup

## Development Workflow

The project follows a 4-stage AI-automation workflow: PRD-Lite → Tech Pack → Implementation → Review. Requirements live in `doc/requirements/`, designs in `doc/design/{module}/`.

### Skills 调用约束

每个 Skill 有明确的前置条件，**必须由用户明确指令触发，禁止自动串联执行**。

| 阶段 | Skill | 前置条件 |
|------|-------|---------|
| 1. 需求 | `/specflow-doc-prd` | 用户提供需求描述或草稿 |
| 2. 设计 | `/specflow-doc-techpack` | 对应 PRD 已**定稿**（用户确认） |
| 3. 实现 | `/specflow-module-gen` | 对应 Tech Pack 已**定稿**（用户确认） |
| 4. 测试 | `/specflow-test-gen` | 模块代码已生成且可编译 |
| 5. 审核 | `/specflow-code-review` | 模块代码已完成（含测试） |
| 6. 文档审核 | `/specflow-doc-review` | 至少有一份 PRD 或 Tech Pack 存在 |
| 7. 部署 | `/specflow-deploy` | 代码已合并到部署分支，构建通过 |

**关键原则**：
- **不跨阶段**：没有定稿 PRD 不得生成 Tech Pack，没有定稿 Tech Pack 不得生成模块代码
- **不自动推进**：完成一个阶段后，等待用户指令，不主动建议或执行下一阶段
- **稳定优先**：每个阶段的产出必须经用户确认后才能作为下一阶段的输入
