# Phase 1.7 — Folder Structure

**Layout:** Polyglot-friendly **monorepo**. Each microservice is an independent Maven module (own `pom.xml`, own Dockerfile, own Flyway scripts). Infra, observability, and deployment manifests live at the root.

---

## 1. Repository Top-Level

```
ecommerce-platform/
├── README.md
├── pom.xml                       # parent (BOM, plugin mgmt, module list) — Phase 4+
├── .github/
│   └── workflows/
│       └── ci-cd.yml             # Build → Test → IT → Sonar → Docker build/push (Phase 13)
├── docs/                         # ← Phase 1 (this phase)
│   ├── 01-high-level-architecture.md
│   ├── 02-service-responsibilities.md
│   ├── 03-database-design.md
│   ├── 04-kafka-topic-design.md
│   ├── 05-api-gateway-design.md
│   ├── 06-security-architecture.md
│   └── 07-folder-structure.md
│
├── services/                     # business microservices (Phase 4–10)
│   ├── auth-service/
│   ├── product-service/
│   ├── inventory-service/
│   ├── cart-service/
│   ├── order-service/
│   ├── payment-service/
│   └── notification-service/
│                                  # (no api-gateway: the edge is NGINX Ingress, Phase 11)
│
├── shared/                       # shared libraries (kept minimal — avoid coupling)
│   ├── common-events/            # Kafka event envelope + DTO contracts
│   └── common-observability/     # logging, tracing, metrics auto-config starter
│
├── infra/                        # local infra + observability (Phase 2)
│   ├── docker-compose.yml
│   ├── postgres/
│   │   └── init-multiple-dbs.sh
│   ├── prometheus/
│   │   └── prometheus.yml
│   ├── grafana/
│   │   ├── provisioning/{datasources,dashboards}/
│   │   └── dashboards/*.json
│   ├── loki/
│   │   └── loki-config.yml
│   ├── promtail/
│   │   └── promtail-config.yml
│   ├── tempo/
│   │   └── tempo.yml
│   └── otel/
│       └── otel-collector-config.yml
│
└── k8s/                          # Kubernetes manifests (Phase 3)
    ├── namespace.yaml
    ├── ingress.yaml               # API gateway routes (Phase 11)
    ├── ingress-nginx-config.yaml  # NGINX controller hardening (Phase 11)
    ├── infra/
    │   ├── postgres/{statefulset,service,pvc,secret}.yaml
    │   ├── kafka/{statefulset,service}.yaml
    │   ├── prometheus/{deployment,service,configmap}.yaml
    │   ├── grafana/{deployment,service,configmap}.yaml
    │   ├── loki/…  ├── promtail/…  ├── tempo/…  └── otel/…
    └── apps/
        ├── auth-service/{deployment,service,configmap,secret}.yaml
        ├── product-service/…
        ├── inventory-service/…
        ├── cart-service/…
        ├── order-service/…
        ├── payment-service/…
        └── notification-service/…
```

---

## 2. Per-Service Structure (Clean Architecture / DDD)

Every business service follows the same shape. Example — `order-service`:

```
order-service/
├── Dockerfile                    # multi-stage, non-root, healthcheck
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/ecommerce/order/
    │   │   ├── OrderServiceApplication.java
    │   │   │
    │   │   ├── api/                       # adapters-in (web)
    │   │   │   ├── controller/OrderController.java
    │   │   │   ├── dto/{CreateOrderRequest, OrderResponse}.java
    │   │   │   └── exception/GlobalExceptionHandler.java
    │   │   │
    │   │   ├── application/               # use cases / ports
    │   │   │   ├── command/{PlaceOrderHandler, CancelOrderHandler}.java
    │   │   │   ├── query/{GetOrderHandler, OrderHistoryHandler}.java
    │   │   │   └── port/
    │   │   │       ├── in/PlaceOrderUseCase.java
    │   │   │       └── out/{OrderRepository, EventPublisher, CartClient}.java
    │   │   │
    │   │   ├── domain/                    # pure domain (no Spring)
    │   │   │   ├── model/{Order, OrderItem, OrderStatus}.java
    │   │   │   ├── event/{OrderCreated, OrderConfirmed}.java
    │   │   │   └── service/OrderPricingService.java
    │   │   │
    │   │   ├── infrastructure/            # adapters-out
    │   │   │   ├── persistence/{OrderJpaEntity, OrderJpaRepository, OrderRepositoryAdapter}.java
    │   │   │   ├── messaging/{OrderEventProducer, PaymentEventConsumer}.java
    │   │   │   └── client/{CartFeignClient, ProductFeignClient}.java
    │   │   │
    │   │   └── config/                    # framework wiring
    │   │       ├── SecurityConfig.java
    │   │       ├── KafkaConfig.java
    │   │       ├── Resilience4jConfig.java
    │   │       ├── OpenApiConfig.java
    │   │       └── ObservabilityConfig.java
    │   │
    │   └── resources/
    │       ├── application.yml            # base
    │       ├── application-local.yml
    │       ├── application-docker.yml
    │       ├── application-k8s.yml
    │       ├── logback-spring.xml         # JSON structured logging
    │       └── db/migration/V1__init.sql  # Flyway
    │
    └── test/
        └── java/com/ecommerce/order/
            ├── unit/                      # JUnit5 + Mockito (domain/app)
            ├── integration/               # @SpringBootTest + Testcontainers (Postgres, Kafka)
            └── contract/                  # API + event contract tests
```

**Package-by-feature within layers** keeps each bounded context cohesive while honoring the Clean Architecture dependency rule (`api → application → domain`, `infrastructure → application/domain`).

---

## 3. API Gateway — NGINX Ingress (no code module)

The edge / API gateway is the **Kubernetes NGINX Ingress Controller**, configured declaratively — there is no `api-gateway` Maven module or Dockerfile.

```
k8s/
├── ingress.yaml                # routes (/api/<ctx> → service:port), TLS, rate limit, CORS
└── ingress-nginx-config.yaml   # controller ConfigMap: security headers, HSTS, TLS posture
```

Routing, TLS, rate limiting, CORS and security headers are expressed as Ingress rules + annotations. JWT validation stays in each service. See [05-api-gateway-design.md](05-api-gateway-design.md).

---

## 4. Conventions

| Convention | Value |
|---|---|
| Base package | `com.ecommerce.<service>` |
| Java version | 21 (records, pattern matching, virtual threads where useful) |
| Build | Maven multi-module; parent BOM pins Spring Boot 3.x + Spring Cloud |
| API prefix | `/api/<resource>` (Ingress forwards as-is — no strip/rewrite) |
| Config profiles | `local`, `docker`, `k8s` |
| Image naming | `ghcr.io/<org>/<service>:<git-sha>` |
| Migrations | Flyway, forward-only, per service |
| Tests | `unit/`, `integration/` (Testcontainers), `contract/` |

---

## Phase 1 — Complete ✅

Delivered design artifacts:
1. High-level architecture + C4 + saga + observability diagrams
2. Service responsibilities + per-service contracts
3. Database design (ER diagrams, schemas, Flyway strategy)
4. Kafka topic design (envelope, saga events, reliability)
5. API Gateway design (routes, filters, rate limiting, errors)
6. Security architecture (JWT/RS256, RBAC, secrets, TLS)
7. Folder structure (monorepo + Clean Architecture per service)

**Next:** Phase 2 — Infrastructure (`docker-compose.yml` + Prometheus/Grafana/Loki/Tempo/OTel configs).
