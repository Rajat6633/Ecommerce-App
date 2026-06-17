# Phase 1.1 — High-Level Architecture

> **Platform:** Cloud-native, Kubernetes-native E-Commerce Platform
> **Runtime:** Java 21 · Spring Boot 3.x · NGINX Ingress (edge) · PostgreSQL · Kafka
> **Principles:** Microservices · DDD · Clean Architecture · SOLID · Event-Driven · CQRS (selective) · Saga

---

## 1. Architectural Style

This platform is a set of **independently deployable microservices**, each owning its data and exposing a bounded context. There is **no Eureka, no Config Server, no Ribbon, no Hystrix** — by design.

| Concern | Cloud-native / K8s-native choice |
|---|---|
| Service discovery | **Kubernetes Services + Kubernetes DNS** (`<service>.<namespace>.svc.cluster.local`) |
| Client-side load balancing | Kubernetes Service (`ClusterIP`) + kube-proxy / IPVS |
| Configuration | **ConfigMaps** + Spring `application.yml` profiles (no Config Server) |
| Secrets | **Kubernetes Secrets** (mounted as env / volumes) |
| Resilience | **Resilience4j** (Circuit Breaker, Retry, Rate Limiter, Bulkhead, Timeout) |
| Edge / routing | **Kubernetes NGINX Ingress Controller** (TLS, routing, rate limit, CORS) — *is* the API gateway |
| Sync comms | REST + **OpenFeign** |
| Async comms | **Apache Kafka** |
| Distributed txns | **Saga (choreography)** over Kafka with compensating events |

---

## 2. System Context (C4 — Level 1)

```mermaid
graph TB
    subgraph Clients
        WEB[Web / SPA]
        MOB[Mobile App]
        ADM[Admin Console]
    end

    INGRESS[NGINX Ingress Controller<br/>API Gateway: TLS · routing · rate limit · CORS]

    WEB --> INGRESS
    MOB --> INGRESS
    ADM --> INGRESS

    INGRESS --> AUTH[Auth Service]
    INGRESS --> PROD[Product Service]
    INGRESS --> INV[Inventory Service]
    INGRESS --> CART[Cart Service]
    INGRESS --> ORD[Order Service]
    INGRESS --> PAY[Payment Service]
    INGRESS --> NOTIF[Notification Service]

    classDef edge fill:#1f6feb,color:#fff;
    classDef svc fill:#238636,color:#fff;
    class INGRESS edge;
    class AUTH,PROD,INV,CART,ORD,PAY,NOTIF svc;
```

---

## 3. Container Diagram (C4 — Level 2)

```mermaid
graph TB
    GW[NGINX Ingress<br/>routing · rate limit · TLS · CORS]

    subgraph BusinessServices[Business Microservices]
        AUTH[Auth Service]
        PROD[Product Service]
        INV[Inventory Service]
        CART[Cart Service]
        ORD[Order Service]
        PAY[Payment Service]
        NOTIF[Notification Service]
    end

    subgraph Datastores[Per-Service Databases]
        AUTHDB[(auth_db)]
        PRODDB[(product_db)]
        INVDB[(inventory_db)]
        CARTDB[(cart_db)]
        ORDDB[(order_db)]
        PAYDB[(payment_db)]
        NOTIFDB[(notification_db)]
    end

    KAFKA{{Apache Kafka}}

    GW --> AUTH & PROD & INV & CART & ORD & PAY & NOTIF

    AUTH --> AUTHDB
    PROD --> PRODDB
    INV --> INVDB
    CART --> CARTDB
    ORD --> ORDDB
    PAY --> PAYDB
    NOTIF --> NOTIFDB

    CART -. OpenFeign .-> PROD
    ORD  -. OpenFeign .-> CART
    ORD  -. OpenFeign .-> PROD

    ORD  -- publishes --> KAFKA
    INV  -- consumes/publishes --> KAFKA
    PAY  -- consumes/publishes --> KAFKA
    NOTIF -- consumes --> KAFKA
    ORD  -- consumes --> KAFKA

    classDef svc fill:#238636,color:#fff;
    classDef db fill:#8957e5,color:#fff;
    classDef bus fill:#bf8700,color:#fff;
    class AUTH,PROD,INV,CART,ORD,PAY,NOTIF svc;
    class AUTHDB,PRODDB,INVDB,CARTDB,ORDDB,PAYDB,NOTIFDB db;
    class KAFKA bus;
```

---

## 4. Observability Plane

Every service emits **metrics, logs, and traces** through a single correlated pipeline.

```mermaid
graph LR
    subgraph Services
        S1[Service A]
        S2[Service B]
    end

    subgraph Metrics
        S1 -- /actuator/prometheus --> PROM[Prometheus]
        S2 -- /actuator/prometheus --> PROM
        PROM --> GRAF[Grafana]
    end

    subgraph Logs
        S1 -- JSON stdout --> PT[Promtail]
        S2 -- JSON stdout --> PT
        PT --> LOKI[Loki]
        LOKI --> GRAF
    end

    subgraph Traces
        S1 -- OTLP --> OTEL[OTel Collector]
        S2 -- OTLP --> OTEL
        OTEL --> TEMPO[Tempo]
        TEMPO --> GRAF
    end

    classDef obs fill:#db6d28,color:#fff;
    class PROM,GRAF,PT,LOKI,OTEL,TEMPO obs;
```

- **Metrics:** Micrometer → Prometheus → Grafana
- **Logs:** Logback JSON (with `traceId`/`spanId`/`correlationId`) → Promtail → Loki → Grafana
- **Traces:** OpenTelemetry SDK (auto-instrumentation) → OTLP → OTel Collector → Tempo → Grafana
- **Correlation:** A single `traceId` links a Grafana metric spike → its logs → its distributed trace.

---

## 5. Distributed Transaction — Order Saga (Choreography)

The critical "place order" flow is a **choreographed saga** over Kafka. No central orchestrator; each service reacts to events and emits compensating events on failure.

```mermaid
sequenceDiagram
    autonumber
    participant C as Client
    participant GW as NGINX Ingress
    participant O as Order Service
    participant K as Kafka
    participant I as Inventory Service
    participant P as Payment Service
    participant N as Notification Service

    C->>GW: POST /orders (Bearer JWT)
    GW->>O: route /api/orders (TLS, rate limit)
    O->>O: validate JWT + create Order(PENDING)
    O->>K: publish order.created
    K->>I: order.created
    I->>I: reserve stock
    alt stock available
        I->>K: publish inventory.reserved
        K->>P: inventory.reserved
        P->>P: process payment (simulated)
        alt payment success
            P->>K: publish payment.completed
            K->>O: payment.completed
            O->>O: Order = CONFIRMED
            O->>K: publish order.confirmed
            K->>N: order.confirmed + payment.completed
            N->>N: send confirmation email
        else payment failed
            P->>K: publish payment.failed
            K->>O: payment.failed
            O->>O: Order = PAYMENT_FAILED
            K->>I: payment.failed
            I->>I: release reserved stock (compensation)
            I->>K: publish inventory.released
        end
    else out of stock
        I->>K: publish inventory.reservation-failed
        K->>O: inventory.reservation-failed
        O->>O: Order = REJECTED
    end
```

**Compensation summary**

| Failure point | Compensating action |
|---|---|
| Stock unavailable | Order → `REJECTED` (no payment attempted) |
| Payment failed | Order → `PAYMENT_FAILED`; Inventory releases reservation |
| Downstream consumer error | Kafka retry + DLT (dead-letter topic); idempotent consumers |

---

## 6. Cross-Cutting Concerns

| Concern | Where it lives |
|---|---|
| **AuthN** | Auth Service issues JWT; **each service** validates signature + expiry (RS256 resource server). Edge does TLS + rate limit, not token checks (see [05](05-api-gateway-design.md) §6) |
| **AuthZ** | RBAC (`ADMIN`, `CUSTOMER`) enforced per service (method-level `@PreAuthorize`) |
| **Idempotency** | Kafka consumers keyed by event id; DB unique constraints on saga keys |
| **Resilience** | Resilience4j on all Feign clients + Kafka producers |
| **Config** | ConfigMaps + Spring profiles (`local`, `docker`, `k8s`) |
| **Secrets** | Kubernetes Secrets (JWT keys, DB creds, Kafka SASL) |
| **Tracing context** | W3C `traceparent` propagated across REST (Feign interceptor) and Kafka headers |

---

## 7. Environments

| Env | How it runs | Discovery | Config | Secrets |
|---|---|---|---|---|
| **local** | IDE + `docker compose` for infra | localhost ports | `application-local.yml` | `.env` (dev only) |
| **docker** | Full `docker compose up -d` | compose service names | `application-docker.yml` | compose env |
| **k8s (Minikube)** | `kubectl apply -f k8s/` | K8s DNS | ConfigMaps | K8s Secrets |

See [02-service-responsibilities.md](02-service-responsibilities.md) for per-service detail.
