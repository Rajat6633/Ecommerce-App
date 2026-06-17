# Phase 5 — Product Service

Catalog service: product CRUD, hierarchical categories, and paginated/sorted search. Public reads, **ADMIN-only writes** (validated as a JWT resource server), with **Redis caching** on hot product lookups.

---

## 1. Endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/products` | public | Search: `name, categoryId, minPrice, maxPrice, activeOnly, page, size, sortBy, sortDirection` |
| GET | `/api/products/{id}` | public | Get one product (cached) |
| POST | `/api/products` | **ADMIN** | Create product → `201` |
| PUT | `/api/products/{id}` | **ADMIN** | Update product (evicts cache) |
| DELETE | `/api/products/{id}` | **ADMIN** | Delete product (evicts cache) → `204` |
| GET | `/api/categories` | public | List categories |
| GET | `/api/categories/{id}` | public | Get category |
| POST | `/api/categories` | **ADMIN** | Create category → `201` |

Sort allow-list: `name, price, createdAt, sku` (anything else → `createdAt`). Page size capped at 100.

---

## 2. Security model (resource server)

- product-service is a **JWT resource server** — it does **not** mint tokens. It validates RS256 tokens with the **auth-service public key** (`auth.jwt.public-key-location`; in k8s the `jwt-keys` Secret mounted at `/etc/jwt/public.pem`).
- Path rule: `GET` on products/categories is public; all writes require authentication.
- Method rule (defense in depth): `@PreAuthorize("hasRole('ADMIN')")` on every write.
- `roles` claim → `ROLE_*` authorities. Missing token on a write → `401`; `CUSTOMER` on a write → `403`.

---

## 3. Caching

- Spring Cache abstraction. `getById` is `@Cacheable("products")`; `update`/`delete` are `@CacheEvict`.
- `spring.cache.type=redis` in docker/k8s (JSON-serialized, 10-min TTL via `CacheConfig`); `simple` (in-memory) locally and in tests — same annotations, no code change.

---

## 4. Search implementation

`ProductPersistenceAdapter` builds a dynamic JPA **Specification** from the optional filters and applies a validated `Sort` + `PageRequest`. The application layer stays Spring-Data-free via the `PageResult<T>` abstraction; the API maps it to `PageResponse<T>`.

---

## 5. Persistence (product_db)

Flyway `V1__init.sql`: `categories` (self-referencing `parent_id`) and `products` (unique `sku`, `NUMERIC(12,2)` price, `@Version` optimistic locking). Hibernate `ddl-auto: validate`. See [03-database-design.md](03-database-design.md).

---

## 6. Build & run

```bash
# Unit tests (no Docker) — JDK 21
JAVA_HOME=/path/to/jdk-21 mvn -pl services/product-service -am test
# Full verify incl. Testcontainers IT + coverage (needs Docker)
mvn -pl services/product-service -am verify
docker build -f services/product-service/Dockerfile -t ecommerce/product-service:latest .
```

---

## 7. Tests

| Test | Type | Docker | Covers |
|---|---|---|---|
| `ProductServiceTest` | unit | no | create (ok/dup-sku/missing-category), update/get/delete not-found, search delegation |
| `CategoryServiceTest` | unit | no | create root / unknown-parent, get not-found |
| `ProductFlowIT` | integration | **yes** | admin create category+product, public read+search, **401** no-token, **403** customer |

`ProductFlowIT` mints RS256 tokens with a dev keypair (`src/test/resources/keys`) whose public half the resource server trusts — exercising the real security chain.

---

## 8. Verification status

**Verified on this machine (JDK 21, Maven 3.6.3):**

```
mvn -pl services/product-service -am test
...
[INFO] Compiling 34 source files with javac [debug parameters release 21]
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

- ✅ Compiles on Java 21; all 10 unit tests pass (`ProductServiceTest` 7, `CategoryServiceTest` 3).
- ⏳ `ProductFlowIT` (Testcontainers + minted JWT) **not run here** — needs Docker. Run `mvn -pl services/product-service -am verify`.
- 🐞 First build failed on a Maven scope trap (test-scoped `oauth2-jose` demoted the compile-scoped copy from the resource-server starter) — fixed by removing the redundant declaration.

---

## Phase 5 — Product Service

Delivered: full Clean Architecture catalog service — CRUD, categories, dynamic search (pagination/sorting), Redis caching, JWT resource-server with RBAC, Flyway, OpenAPI, JSON logging, multi-stage non-root Dockerfile, unit + integration tests.

**Next:** Phase 6 — Inventory Service (stock management, **reservation/release saga participant**, the first Kafka producer/consumer + `common-events` shared module).
