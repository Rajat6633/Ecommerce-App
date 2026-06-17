# Phase 1.3 — Database Design

**Pattern:** Database-per-service. No cross-service joins, no shared schema. Each service owns its PostgreSQL database; integration happens via REST/Kafka, never SQL.

- **ORM:** Spring Data JPA + Hibernate
- **Migrations:** Flyway (`V1__init.sql`, `V2__...` per service, under `src/main/resources/db/migration`)
- **IDs:** UUID (application-generated) for externally-referenced aggregates; BIGINT identity for internal-only rows
- **Money:** `NUMERIC(12,2)` + ISO currency code; never `float`/`double`
- **Auditing:** `created_at`, `updated_at`, `version` (optimistic locking) on mutable aggregates

---

## 1. ER Diagrams (per bounded context)

### 1.1 auth_db
```mermaid
erDiagram
    USERS ||--o{ USER_ROLES : has
    ROLES ||--o{ USER_ROLES : assigned
    USERS ||--o{ REFRESH_TOKENS : owns

    USERS {
        uuid id PK
        string email UK
        string password_hash
        string full_name
        boolean enabled
        timestamptz created_at
        timestamptz updated_at
    }
    ROLES {
        bigint id PK
        string name UK "ADMIN | CUSTOMER"
    }
    USER_ROLES {
        uuid user_id FK
        bigint role_id FK
    }
    REFRESH_TOKENS {
        uuid id PK
        uuid user_id FK
        string token_hash UK
        timestamptz expires_at
        boolean revoked
        timestamptz created_at
    }
```

### 1.2 product_db
```mermaid
erDiagram
    CATEGORIES ||--o{ PRODUCTS : contains
    CATEGORIES ||--o{ CATEGORIES : parent

    CATEGORIES {
        uuid id PK
        string name
        uuid parent_id FK "nullable"
        timestamptz created_at
    }
    PRODUCTS {
        uuid id PK
        string sku UK
        string name
        text description
        numeric price
        string currency
        uuid category_id FK
        boolean active
        int version
        timestamptz created_at
        timestamptz updated_at
    }
```

### 1.3 inventory_db
```mermaid
erDiagram
    INVENTORY_ITEMS ||--o{ STOCK_RESERVATIONS : reserves

    INVENTORY_ITEMS {
        uuid id PK
        uuid product_id UK
        int on_hand
        int reserved
        int reorder_level
        int version
        timestamptz updated_at
    }
    STOCK_RESERVATIONS {
        uuid id PK
        uuid order_id UK
        uuid product_id FK
        int quantity
        string status "RESERVED | RELEASED | CONFIRMED"
        timestamptz created_at
    }
```
> `available = on_hand - reserved`. Reservation uses optimistic locking (`version`) to prevent oversell.

### 1.4 cart_db
```mermaid
erDiagram
    CARTS ||--o{ CART_ITEMS : contains

    CARTS {
        uuid id PK
        uuid user_id UK
        timestamptz created_at
        timestamptz updated_at
    }
    CART_ITEMS {
        uuid id PK
        uuid cart_id FK
        uuid product_id
        int quantity
        numeric unit_price "snapshot"
        timestamptz added_at
    }
```

### 1.5 order_db
```mermaid
erDiagram
    ORDERS ||--o{ ORDER_ITEMS : contains
    ORDERS ||--o{ ORDER_STATUS_HISTORY : tracks

    ORDERS {
        uuid id PK
        uuid user_id
        string status "PENDING|INVENTORY_RESERVED|PAID|CONFIRMED|REJECTED|PAYMENT_FAILED|CANCELLED"
        numeric total_amount
        string currency
        int version
        timestamptz created_at
        timestamptz updated_at
    }
    ORDER_ITEMS {
        uuid id PK
        uuid order_id FK
        uuid product_id
        string product_name "snapshot"
        int quantity
        numeric unit_price
    }
    ORDER_STATUS_HISTORY {
        bigint id PK
        uuid order_id FK
        string status
        string note
        timestamptz changed_at
    }
```

### 1.6 payment_db
```mermaid
erDiagram
    PAYMENTS {
        uuid id PK
        uuid order_id UK
        uuid user_id
        numeric amount
        string currency
        string status "INITIATED | COMPLETED | FAILED"
        string failure_reason "nullable"
        string idempotency_key UK
        timestamptz created_at
        timestamptz updated_at
    }
```

### 1.7 notification_db
```mermaid
erDiagram
    NOTIFICATIONS {
        uuid id PK
        uuid reference_id "order_id / payment_id"
        string channel "EMAIL"
        string recipient
        string type "ORDER_CONFIRMATION | PAYMENT_SUCCESS"
        string status "SENT | FAILED"
        text payload
        timestamptz created_at
    }
```

---

## 2. Flyway Migration Strategy

| Rule | Detail |
|---|---|
| Location | `src/main/resources/db/migration` per service |
| Naming | `V<version>__<description>.sql` (e.g. `V1__init.sql`) |
| Baseline | `V1__init.sql` creates tables + indexes + seed roles |
| Forward-only | Never edit an applied migration; add a new one |
| Validation | `spring.flyway.validate-on-migrate=true` |
| Per-env | Same scripts across local/docker/k8s; data seeding via separate `R__seed.sql` repeatable scripts in dev only |

**Example — `auth_db` `V1__init.sql` (skeleton, full version delivered in Phase 4):**
```sql
CREATE TABLE roles (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(32) NOT NULL UNIQUE
);
INSERT INTO roles (name) VALUES ('ADMIN'), ('CUSTOMER');

CREATE TABLE users (
    id            UUID PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(255),
    enabled       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE user_roles (
    user_id UUID   NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id),
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE refresh_tokens (
    id         UUID PRIMARY KEY,
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
```

---

## 3. Indexing & Performance Notes

| Table | Index | Reason |
|---|---|---|
| `products` | `(category_id)`, `(name text_pattern_ops)`, `(active)` | search/filter |
| `inventory_items` | `(product_id)` unique | hot-path reservation lookup |
| `orders` | `(user_id, created_at desc)` | order history pagination |
| `payments` | `(order_id)` unique, `(idempotency_key)` unique | idempotency |
| `refresh_tokens` | `(token_hash)` unique, `(user_id)` | login/refresh |

---

## 4. PostgreSQL Topology

- **One PostgreSQL instance** per environment with **one database per service** (logical isolation), OR a StatefulSet per DB in K8s for stronger isolation.
- Local/docker: single Postgres container, multiple DBs created via init script.
- K8s: PostgreSQL StatefulSet + PVC; credentials from Kubernetes Secrets; each service points at its own DB via ConfigMap `SPRING_DATASOURCE_URL`.

See [04-kafka-topic-design.md](04-kafka-topic-design.md).
