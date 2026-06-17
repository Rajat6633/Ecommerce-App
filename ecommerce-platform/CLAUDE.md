# CLAUDE.md

Guidance for Claude Code when working in this repository.

## What this is

Production-grade, **cloud-native / Kubernetes-native** e-commerce platform.
Built incrementally in phases (see README checklist): Phases 1-12 + Phase 14 (KYC/Spring AI).
**Phase 13 (CI/CD) is dropped — this is a local-only project, no CI/CD pipeline.**
Java 21 · Spring Boot 3.x · PostgreSQL · Kafka · Docker · Kubernetes (Minikube) ·
full observability stack · Spring AI (kyc-service). Local setup + AI roadmap: `docs/20`.

Design docs live in [`docs/`](docs/) (`01`–`09`). **Read the relevant doc before
implementing a phase** — they are the source of truth for architecture, DB schemas,
Kafka topics, gateway routes, and security.

## Hard constraints — do NOT violate

- ❌ **No Eureka.** ❌ **No Spring Cloud Config Server.** ❌ **No Ribbon.** ❌ **No Hystrix.**
- ✅ Service discovery = **Kubernetes Services + DNS** (`<service>:<port>`, e.g. `product-service:8082`, `kafka:9092`).
- ✅ Resilience = **Resilience4j** (circuit breaker / retry / rate limiter / bulkhead / timeout).
- ✅ Distributed transactions = **choreographed Saga over Kafka** with compensating events.
- ✅ **Database-per-service** — no shared schema, no cross-service SQL joins. Integrate via REST/Kafka only.
- ✅ Config from **ConfigMaps + Spring profiles**; secrets from **Kubernetes Secrets**.

## Architecture conventions (apply to every service)

- **Clean Architecture layering:** `api → application → domain → infrastructure`. Domain has **zero** framework dependencies. Dependency rule must hold.
- **DDD:** one bounded context per service.
- Base package: `com.ecommerce.<service>`.
- Sync calls: **OpenFeign**, targets via K8s DNS, wrapped in Resilience4j. Async: **Kafka**.
- Migrations: **Flyway**, forward-only, `src/main/resources/db/migration/V<n>__*.sql`.
- Money: `NUMERIC(12,2)` + currency code — never float/double. IDs: UUID for externally-referenced aggregates.
- Profiles: `local`, `docker`, `k8s`. Kafka consumers must be **idempotent** (dedupe by `eventId`).
- **Spring AI (Phase 14, kyc-service only):** chat/vision = Claude `claude-opus-4-8` (ID-document extraction, risk narratives); embeddings = a **local Transformers `EmbeddingModel` + pgvector** for sanctions name screening, since **Anthropic has no embeddings API** (Claude is chat/vision only). Spring AI BOM pinned to `1.0.0-M1` (Spring Boot 3.3.4 compat). These deps are **confined to kyc-service** — do not add them elsewhere. New topics: `kyc.approved` / `kyc.rejected` (kyc-service consumes `user.registered`); order-service consumes `kyc.*` and **gates checkout** on KYC approval (`order.kyc.gating.enabled`, default true).

## Service ports

> Edge / API gateway = **Kubernetes NGINX Ingress Controller** (not a Spring service; no port/DB here). It terminates TLS and routes `/api/*` to the services below. JWT validation stays in each service. See `docs/05-api-gateway-design.md`.

| Service | Port | DB |
|---|---|---|
| auth-service | 8081 | auth_db |
| product-service | 8082 | product_db |
| inventory-service | 8083 | inventory_db |
| cart-service | 8084 | cart_db |
| order-service | 8085 | order_db |
| payment-service | 8086 | payment_db |
| notification-service | 8087 | notification_db |
| kyc-service | 8088 | kyc_db |

## Observability contract (every service)

- Metrics: Micrometer → `/actuator/prometheus`; tag `service=<name>`. Custom business counters (e.g. `orders_created_total`).
- Logs: structured **JSON** to stdout via `logback-spring.xml`, including `traceId`/`spanId`/`correlationId`.
- Traces: OpenTelemetry → OTLP to `otel-collector` (`http://otel-collector:4318/v1/traces`).
- Health: expose `health` with liveness/readiness probe groups.

## Layout

```
docs/        # design (source of truth)
infra/       # docker-compose + Prometheus/Grafana/Loki/Promtail/Tempo/OTel configs
k8s/         # namespace, secrets, infra/, apps/, ingress, deploy.sh/.ps1
services/    # microservice Maven modules (Phase 4+)
shared/      # common-events, common-observability (keep minimal)
```

## Build / run / test

> ⚠️ **Tooling not installed on this machine:** Docker, kubectl, minikube, python are
> absent. Cannot build images, run compose, deploy to k8s, or live-validate manifests
> here. Flag this when verification is requested; provide commands for the user to run.

Once tooling exists:
- Infra: `cd infra && cp .env.example .env && docker compose up -d`
- K8s: `minikube start --cpus=4 --memory=8192` → `minikube addons enable ingress` → `./k8s/deploy.sh` → `minikube tunnel`
- Build a service (Phase 4+): `cd services/<svc> && ./mvnw verify`
- Validate manifests: `kubectl apply --dry-run=client -R -f k8s/`

## Skills available in this repo (`.claude/skills/`)

Workflow skills (from github.com/obra/superpowers) — auto-discovered, loaded only when invoked:

- **test-driven-development** — write the failing test first (supports the 80%+ coverage goal).
- **systematic-debugging** — find root cause before fixing; no symptom patches.
- **verification-before-completion** — run verification and show evidence before claiming done.
- **writing-plans** / **executing-plans** — plan a multi-step phase, then execute against the plan.

Built-in skills also in play: `/code-review`, `/security-review` (run on auth + payment), `/verify`, `/run`.

## Working style for this repo

- Deliver **one phase at a time**; pause for confirmation before the next phase.
- Use **test-driven-development** when writing service code, and **verification-before-completion** before claiming a phase is done.
- Generate **production-ready** code, not tutorial snippets: validation, error handling, security, tests (JUnit5 + Mockito + Testcontainers, target 80%+), OpenAPI docs, multi-stage non-root Dockerfile.
- After completing a phase, update the README checklist and the relevant `docs/` entry.
- When unsure about a design choice, check `docs/` first; only ask if genuinely ambiguous.
