# Phase 1.2 — Service Responsibilities

Each service is a **bounded context** (DDD) with its own database, its own deployment, and a clear public contract. Internal layering follows **Clean Architecture**: `api → application → domain → infrastructure`.

---

## 1. Service Catalog

| # | Service | Bounded Context | Owns DB | Default Port | Sync Deps (Feign) | Kafka Role |
|---|---|---|---|---|---|---|
| 1 | **auth-service** | Identity & Access | `auth_db` | 8081 | — | — |
| 2 | **product-service** | Catalog | `product_db` | 8082 | — | — |
| 3 | **inventory-service** | Stock | `inventory_db` | 8083 | — | consumer + producer |
| 4 | **cart-service** | Shopping Cart | `cart_db` | 8084 | product-service | — |
| 5 | **order-service** | Order Lifecycle | `order_db` | 8085 | cart-service, product-service | producer + consumer |
| 6 | **payment-service** | Payments | `payment_db` | 8086 | — | consumer + producer |
| 7 | **notification-service** | Notifications | `notification_db` | 8087 | — | consumer |

> **Edge / API gateway** is the **Kubernetes NGINX Ingress Controller** (not a service in this catalog, no port/DB of its own). It terminates TLS and routes `/api/*` to the services above. See §8 and [05-api-gateway-design.md](05-api-gateway-design.md).

---

## 2. Detailed Responsibilities

### 1. Auth Service (`auth-service`)
**Purpose:** Single source of truth for identity and tokens.
- User registration (BCrypt-hashed passwords).
- Login → issue **JWT access token** (short-lived) + **refresh token** (long-lived, persisted/rotated).
- Refresh token endpoint with rotation + revocation.
- Role-Based Authorization: `ADMIN`, `CUSTOMER`.
- Exposes JWKS / public key so every service validates tokens **without** calling back.
- **Does NOT** call other services.

**Key endpoints:** `POST /auth/register`, `POST /auth/login`, `POST /auth/refresh`, `POST /auth/logout`, `GET /auth/me`.

---

### 2. Product Service (`product-service`)
**Purpose:** Product catalog and discovery.
- Product CRUD (`ADMIN` only for writes).
- Category management (hierarchical).
- Product search (name, category, price range) with **pagination + sorting**.
- **CQRS-lite:** read model optimized for search/list; write model for CRUD.

**Key endpoints:** `GET /products`, `GET /products/{id}`, `GET /products/search`, `POST/PUT/DELETE /products` (ADMIN), `GET/POST /categories`.

---

### 3. Inventory Service (`inventory-service`)
**Purpose:** Authoritative stock levels and reservations (saga participant).
- Stock management per SKU/product.
- **Stock reservation** on `order.created` (optimistic locking + `available = on_hand - reserved`).
- **Stock release** (compensation) on `payment.failed`.
- Inventory updates (restock, adjustments) — `ADMIN`.
- Emits: `inventory.reserved`, `inventory.reservation-failed`, `inventory.released`.

**Key endpoints:** `GET /inventory/{productId}`, `PUT /inventory/{productId}` (ADMIN), `POST /inventory/adjust` (ADMIN).

---

### 4. Cart Service (`cart-service`)
**Purpose:** Per-user shopping cart.
- Add / remove product, update quantity, view cart.
- Validates product existence/price via **OpenFeign → product-service** (resilient: circuit breaker + fallback).
- Cart is user-scoped (derived from JWT subject).

**Key endpoints:** `GET /cart`, `POST /cart/items`, `PUT /cart/items/{productId}`, `DELETE /cart/items/{productId}`, `DELETE /cart`.

---

### 5. Order Service (`order-service`)
**Purpose:** Order lifecycle + **saga initiator**.
- Place order (snapshots cart via Feign, persists `order` + `order_items`).
- Order history (per user) + status tracking.
- Saga: publishes `order.created`; consumes `payment.completed` / `payment.failed` / `inventory.reservation-failed`; updates status and publishes `order.confirmed`.
- **CQRS:** command side (place/cancel) vs query side (history, status).

**Order states:** `PENDING → (INVENTORY_RESERVED) → (PAID) → CONFIRMED` · failure paths: `REJECTED`, `PAYMENT_FAILED`, `CANCELLED`.

**Key endpoints:** `POST /orders`, `GET /orders`, `GET /orders/{id}`, `GET /orders/{id}/status`.

---

### 6. Payment Service (`payment-service`)
**Purpose:** Payment processing simulation (saga participant).
- Consumes `inventory.reserved` → simulates payment (configurable success rate / amount rules).
- Persists payment record + status (`INITIATED → COMPLETED | FAILED`).
- Emits: `payment.completed`, `payment.failed`.
- Idempotent per `orderId` (unique constraint).

**Key endpoints:** `GET /payments/{orderId}` (status query).

---

### 7. Notification Service (`notification-service`)
**Purpose:** Customer notifications.
- Consumes `order.confirmed` + `payment.completed`.
- Sends **email** (SMTP / simulated mailer) — order confirmation, payment success.
- Persists notification audit log.
- Pure consumer; emits no domain events.

---

### 8. API Gateway (NGINX Ingress Controller)
**Purpose:** Single entry point (edge). Implemented as the Kubernetes NGINX Ingress Controller — **not** a Spring service.
- **TLS termination** (`ecommerce-tls`) + HSTS, TLS 1.2/1.3.
- **Path-based routing** to services via **Kubernetes DNS** (`/api/<ctx>` → `service:port`, no rewrite).
- **Rate limiting** per client IP (`limit-rps`/`limit-connections`); stricter on `/api/auth`. In-process — no Redis.
- **CORS** + **security headers** (X-Frame-Options, X-Content-Type-Options, Referrer-Policy, …) centralized at the edge.
- **JWT validation is delegated to the services** (RS256 resource servers) — defense-in-depth. Optional edge `auth-url` external-auth documented in [05](05-api-gateway-design.md) §6.

---

## 3. Clean Architecture — internal layering (per service)

```
service-name/
├── api/             # REST controllers, request/response DTOs, exception handlers (adapters-in)
├── application/     # use cases / command + query handlers, ports (interfaces), app services
├── domain/          # entities, value objects, domain events, domain services (no framework)
├── infrastructure/  # JPA repos, Kafka producers/consumers, Feign clients, config (adapters-out)
└── config/          # Spring wiring, security, observability, resilience
```

**Dependency rule:** `api → application → domain`; `infrastructure → application/domain`. Domain depends on **nothing** framework-specific.

---

## 4. Public vs Internal contracts

| Contract type | Mechanism | Consumers |
|---|---|---|
| Public REST | OpenAPI 3 / Swagger UI per service | clients via NGINX Ingress |
| Internal REST | OpenFeign clients | cart→product, order→cart/product |
| Events | Kafka topics + versioned Avro/JSON schema | saga participants |

See [03-database-design.md](03-database-design.md) and [04-kafka-topic-design.md](04-kafka-topic-design.md).
