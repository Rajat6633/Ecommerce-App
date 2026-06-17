# Phase 4 — Auth Service

First production service + the **Maven foundation** (parent POM, JaCoCo 80% gate) that Phases 5–11 inherit. Implements registration, login, **JWT RS256** issuance, refresh-token rotation with reuse detection, RBAC, and BCrypt — in strict Clean Architecture.

---

## 1. Endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/auth/register` | public | Register a customer (BCrypt-hashed) → `201` |
| POST | `/api/auth/login` | public | Authenticate → access + refresh tokens |
| POST | `/api/auth/refresh` | public¹ | Rotate refresh token → new pair |
| POST | `/api/auth/logout` | bearer | Revoke a refresh token → `204` |
| GET | `/api/auth/me` | bearer | Current user's profile |

¹ "public" = no access token required, but a valid refresh token must be supplied in the body.

Swagger UI: `/swagger-ui.html` · OpenAPI: `/v3/api-docs`.

---

## 2. Clean Architecture layering

```
com.ecommerce.auth
├── api/                  controllers, DTOs (records), GlobalExceptionHandler
├── application/
│   ├── port/in/          AuthenticationUseCase (+ command/result records)
│   ├── port/out/         UserRepositoryPort, RefreshTokenRepositoryPort,
│   │                     TokenProviderPort, PasswordHasherPort, RefreshTokenFactoryPort
│   └── service/          AuthenticationService  (orchestration only)
├── domain/               User, Role, RefreshToken, exceptions  (no framework deps)
└── infrastructure/
    ├── persistence/      JPA entities, Spring Data repos, port adapters
    ├── security/         RS256 keys, JwtTokenProvider, BCrypt, SecurityConfig
    └── config/           OpenApiConfig
```

**Dependency rule holds:** `domain` imports nothing framework-specific; `application` depends only on domain + its own ports; `infrastructure` and `api` implement/consume those ports.

---

## 3. Security design

| Aspect | Choice |
|---|---|
| Access token | **JWT, RS256** (asymmetric) — signed with private key, verified with public key |
| Token lib | Spring Security OAuth2 (`NimbusJwtEncoder`/`NimbusJwtDecoder`) — no extra JWT lib |
| Claims | `iss, sub(userId), email, roles[], iat, exp, jti` |
| Refresh token | Opaque 256-bit random; **only SHA-256 hash stored**; rotated every use |
| Reuse detection | A revoked token presented again → **revoke entire token family** for that user |
| Passwords | **BCrypt** strength 12 |
| RBAC | `roles` claim → `ROLE_*` authorities; `@EnableMethodSecurity` for `@PreAuthorize` |
| Enumeration safety | Unknown email and wrong password both return the same `401` |
| Keys | PEM from configured location (k8s Secret `jwt-keys`); **ephemeral keypair** generated when unset (local/test only) |

The public key is what every other service + the gateway use to validate tokens — auth-service is the only minter.

---

## 4. Persistence (auth_db)

Flyway `V1__init.sql` creates `users`, `user_roles` (element collection), `refresh_tokens`. Hibernate runs in `ddl-auto: validate` — Flyway owns the schema. See [03-database-design.md](03-database-design.md).

---

## 5. Build & run

```bash
# Unit tests (no Docker needed) — JDK 21 required
JAVA_HOME=/path/to/jdk-21 mvn -pl services/auth-service -am test

# Full verify incl. Testcontainers IT + 80% coverage gate (needs Docker)
mvn -pl services/auth-service -am verify

# Run locally (needs docker-compose infra up: postgres on :5432)
mvn -pl services/auth-service spring-boot:run -Dspring-boot.run.profiles=local

# Container image
docker build -f services/auth-service/Dockerfile -t ecommerce/auth-service:latest .
```

> On Windows this machine, `JAVA_HOME` points at JDK 8 — override it to the JDK 21 path
> (`C:\Program Files\OpenLogic\jdk-21.0.8.9-hotspot`) when invoking Maven.

---

## 6. Tests

| Test | Type | Needs Docker | Covers |
|---|---|---|---|
| `AuthenticationServiceTest` | unit (Mockito) | no | register, login (ok/bad/unknown), refresh rotation, **reuse detection**, logout, currentUser |
| `AuthFlowIT` | integration (Testcontainers + Flyway) | **yes** | full register→login→/me→refresh flow, 401 paths |

Naming: `*Test` → surefire (unit, `mvn test`); `*IT` → failsafe (`mvn verify`). JaCoCo enforces **80% line coverage** at `verify` (Application/Config/Properties excluded).

---

## 7. Verification status

**Verified on this machine (JDK 21, Maven 3.6.3):**

```
mvn -pl services/auth-service -am test
...
[INFO] Compiling 37 source files with javac [debug parameters release 21]
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

- ✅ Compiles on Java 21.
- ✅ All 10 unit tests pass (`AuthenticationServiceTest`) — register, login (ok/wrong/unknown), refresh rotation, reuse-detection, logout, currentUser.
- ⏳ `AuthFlowIT` (Testcontainers) **not run here** — requires Docker (absent). Run `mvn -pl services/auth-service -am verify` on a machine with Docker to execute it + the JaCoCo 80% gate.

---

## Phase 4 — Auth Service

Delivered: parent POM + auth-service module, full Clean Architecture implementation, RS256 JWT with refresh rotation + reuse detection, RBAC, Flyway, OpenAPI, JSON logging, multi-stage non-root Dockerfile, unit + integration tests.

**Next:** Phase 5 — Product Service (CRUD, categories, search with pagination/sorting, Redis `@Cacheable`, OpenFeign target for cart).
