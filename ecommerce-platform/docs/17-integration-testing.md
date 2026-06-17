# Phase 12 — Integration Testing

End-to-end **integration tests** for every service, run against **real infrastructure** (PostgreSQL + Kafka) via **Testcontainers**. ITs are `*IT` classes run by the Failsafe plugin during `mvn verify` (Surefire runs the `*Test` unit tests during `test`).

---

## 1. Test inventory

| Service | IT | Containers | What it proves |
|---|---|---|---|
| auth | `AuthFlowIT` | Postgres | register → login → refresh; protected route needs JWT |
| product | `ProductFlowIT` | Postgres | admin CRUD, public read/search, **401 (no token) / 403 (CUSTOMER)** |
| cart | `CartFlowIT` | Postgres | cart CRUD over REST with minted JWT |
| inventory | `InventorySagaIT` | Postgres + Kafka | `order.created` → reserve stock → `inventory.reserved` |
| order | `OrderPlacementIT` | Postgres + Kafka | place order (cart/product mocked) → `order.created` published |
| payment | `PaymentSagaIT` | Postgres + Kafka | `inventory.reserved` → COMPLETED payment persisted |
| notification | `NotificationSagaIT` | Postgres + Kafka | `order.confirmed` + `payment.completed` → 2 SENT audit rows _(added in this phase)_ |

**Two IT shapes:**
- **Flow ITs** — `@SpringBootTest(RANDOM_PORT)` + `TestRestTemplate`, asserting HTTP status/RBAC with RS256 tokens minted by `TestTokens`. Postgres only.
- **Saga ITs** — publish an `EventEnvelope` via `KafkaTemplate`, then `await()` until the expected DB state appears. Postgres + Kafka.

All datasource/Kafka endpoints are injected with `@DynamicPropertySource` from the running containers.

---

## 2. Running the suite

```powershell
$env:JAVA_HOME = "C:\Program Files\OpenLogic\jdk-21.0.8.9-hotspot"   # JDK 21
$env:Path = "C:\Program Files\Docker\Docker\resources\bin;$env:JAVA_HOME\bin;" + $env:Path
cd d:\MyDevWorkSpace\JS\ecommerce-platform
mvn -B "-Djacoco.skip=true" verify        # unit + integration tests; coverage gate skipped
```

Docker Desktop must be running. First run pulls `postgres:16-alpine`, `confluentinc/cp-kafka:7.6.1`, and `testcontainers/ryuk` (cached afterwards).

> **Coverage gate:** the JaCoCo 80% line-coverage check is **deferred** for this phase (`-Djacoco.skip=true`). The ITs lift coverage substantially (e.g. product 25% → ~78%) but a few modules sit just under 80%; closing that with targeted unit tests is tracked as follow-up.

---

## 3. Environment fixes (modern Docker Engine on Windows)

Running Testcontainers against **Docker Engine 29 / Docker Desktop on Windows (WSL2)** required:

| Problem | Fix |
|---|---|
| Testcontainers 1.19.8 / docker-java 3.3.6 (Spring Boot 3.3.4 default, 2023) can't talk to Engine 29 over the named pipe | Override `testcontainers.version` → **1.20.6** in the parent pom |
| docker-java defaults to Docker API **1.32**; Engine 29 has `MinAPIVersion 1.40` → every call returns **HTTP 400** | Failsafe `systemPropertyVariables` **`api.version=1.43`** in the parent pom (CI-portable; Docker 24+ supports it) |
| Default pipe `//./pipe/docker_engine` is Docker Desktop's CLI proxy | `~/.testcontainers.properties` → `docker.host=npipe:////./pipe/dockerDesktopLinuxEngine` (machine-local; Linux CI uses the default socket) |
| `docker pull` from Maven failed — `docker-credential-desktop` not found | Put `C:\Program Files\Docker\Docker\resources\bin` on `PATH` for the build |

The first two are committed in the repo (`pom.xml`); the last two are local-machine setup, not needed on Linux CI.

---

## 4. Defects the ITs caught (and fixed)

1. **`AccessDeniedException` → 500 instead of 403.** Method-security (`@PreAuthorize`) denials were swallowed by the `@RestControllerAdvice` catch-all `@ExceptionHandler(Exception.class)`. Added an explicit `@ExceptionHandler(AccessDeniedException.class)` → 403 in **product** and **inventory** (the services with role-based endpoints). Surfaced by `ProductFlowIT.createProduct_withCustomerRole_returns403`.

2. **Saga ITs had no Kafka config.** Each service's `src/test/resources/application.yml` **shadows** the main `application.yml` on the classpath, so the test context had no consumer `group-id`/deserializers → `IllegalStateException: No group.id found`. Restated the Kafka producer/consumer/listener block in the four saga services' test ymls (bootstrap-servers still comes from the container).

3. **`PaymentSagaIT` await failed fast.** Its assertion calls `getByOrderId(...)`, which **throws** `PaymentNotFoundException` until the payment exists; Awaitility's `untilAsserted` only retries on `AssertionError`, so the thrown exception failed the test on the first poll. Added `await().ignoreExceptions()`.

---

## 5. Result

`mvn -Djacoco.skip=true verify` → **BUILD SUCCESS**, all 7 services' integration tests green (Flow + Saga), unit tests green. See the per-service docs (`10`–`16`) for each service's behavior.
