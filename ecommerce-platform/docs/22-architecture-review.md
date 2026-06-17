# 22 — Architecture Review (Roadmap B.5)

**Date:** 2026-06-15
**Scope:** Whole monorepo — 8 services (`auth`, `product`, `inventory`, `cart`,
`order`, `payment`, `notification`, `kyc`) + `shared/common-events`.
**Stack:** Java 21 · Spring Boot 3.3.4 · Clean Architecture + DDD per service ·
choreographed Kafka saga · Spring AI (kyc / product / order).
**Trigger:** Catch architectural drift after the wave-1/2 Spring AI additions in
`kyc-service`, `product-service`, and `order-service`.

This review evaluates the codebase against the B5.1–B5.5 checklist. Method: static
analysis via package-tree inspection plus targeted greps (framework imports under
`**/domain/**`; repository/Kafka/RestClient/Spring-AI usage in controllers and
application services; `record *Event` declarations outside `common-events`; topic
and envelope conventions), cross-checked against the C6 contract tests.

---

## Overall architecture health: **EXCELLENT — conformant on all five dimensions**

No architectural drift was detected. The Spring AI additions were integrated
**without violating** Clean Architecture, the ports/adapters boundary, the event
ownership rule, the package conventions, or the event-naming conventions. **No
fixes were required** — every dimension passed as-is. The findings below are
advisory (low-severity hygiene notes), not violations.

---

## B5.1 — Clean Architecture conformance: **PASS**

For every service the layering `api → application → domain → infrastructure` is
present, and the **domain has zero framework dependencies**.

- Aggregated import scan of all 8 `domain/` packages yields **only**
  `java.util.*`, `java.time.Instant`, `java.math.BigDecimal`, and intra-domain
  references (e.g. `com.ecommerce.order.domain.model.OrderStatus`). No imports of
  `org.springframework`, `jakarta.persistence`/`javax.persistence`,
  `org.apache.kafka`, `com.fasterxml.jackson`, `org.hibernate`, `lombok`, or
  `org.springframework.ai` anywhere under `domain/`.
- **Dependency rule holds:** no `domain/` file imports `application` or
  `infrastructure`; no `application/` main file imports `infrastructure`; no
  controller imports `infrastructure` or a concrete `application.service`.
- The new AI types (`ChatModel`, `EmbeddingModel`, `VectorStore`, the Anthropic
  client) appear **only** under `infrastructure/ai` and `infrastructure/config`,
  never in `api`/`application`/`domain` main code. The single
  `org.springframework.ai` import outside `infrastructure` is in a **test**
  (`product-service` `SemanticSearchServiceTest`), which is expected.

## B5.2 — No port/adapter bypass: **PASS**

- **Controllers → in-ports only.** No controller references a JPA repository,
  `KafkaTemplate`, `RestClient`/`RestTemplate`/`WebClient`, `@FeignClient`, or any
  Spring AI type. They depend on `application.port.in` use-cases.
- **Application services → out-ports only.** Every collaborator field in every
  application service is a `*Port` interface: `*RepositoryPort`,
  `ProcessedEventPort`, `*EventPublisherPort`, `PaymentGatewayPort`,
  `CartClientPort`, `ProductCatalogPort`, `CustomerKycStatusPort`, and the AI
  ports `ChatAssistantPort`, `ScreeningPort`, `RiskNarrativePort`,
  `DocumentExtractionPort`, `ProductIndexPort`, `WatchlistStorePort`,
  `IdentityVendorPort`, `WatchlistFeedPort`, `UploadRateLimiterPort`. (Variable
  names ending in `...Repository` are ports, not concrete repos.)
- **Infrastructure adapters implement the out-ports.** The AI work was wired
  correctly: `ChatAssistantAdapter` ⊳ `ChatAssistantPort`, `ScreeningAdapter` ⊳
  `ScreeningPort`, `RiskNarrativeAdapter` ⊳ `RiskNarrativePort`,
  `DocumentExtractionAdapter` ⊳ `DocumentExtractionPort`,
  `VectorStoreProductIndexAdapter` ⊳ `ProductIndexPort`, and the kyc watchlist
  adapter ⊳ `WatchlistStorePort`. Spring AI never leaks across the port boundary.

## B5.3 — Duplicate DTOs / shared shapes: **PASS**

- **Event-payload ownership invariant holds.** A repo-wide search for
  `record *Event` outside `common-events` returns **zero** matches. All 11 event
  payloads live solely in
  `shared/common-events/.../events/payload/` (`UserRegisteredEvent`,
  `KycApprovedEvent`, `KycRejectedEvent`, `OrderCreatedEvent`,
  `OrderConfirmedEvent`, `InventoryReservedEvent`,
  `InventoryReservationFailedEvent`, `InventoryReleasedEvent`,
  `PaymentCompletedEvent`, `PaymentFailedEvent`). No service redefines an event.
- **Cross-service API DTOs:** `ErrorResponse` exists once per service (8 copies).
  Per the microservices stance, this is **acceptable** independent ownership — not
  a violation, and intentionally **not** extracted into a shared module (that would
  re-introduce coupling). `PageResponse` exists only in `product-service` (no dup).
- **Within-service redundancy:** none. The only same-named pair,
  `order-service`'s two nested `record Item` types
  (`api/dto/OrderResponse.Item` vs `infrastructure/client/CartDto.Item`), are
  **distinct shapes in distinct layers** (API response line vs cart-client line,
  different fields) — correctly separated, not a redundant duplicate.

## B5.4 — Package & naming consistency: **PASS**

- Base package `com.ecommerce.<svc>` for all 8 services.
- Identical layer sub-packages everywhere:
  `api/{controller,dto,exception}`, `application/{port/in,port/out,service}`,
  `domain/{model,exception}`, `infrastructure/{config,persistence,security,...}`.
- The new AI code follows the same layout consistently across the three AI
  services: **adapters** under `infrastructure/ai/` and **wiring/stubs** under
  `infrastructure/config/` (`AiConfig`, `VectorStoreConfig`, `StubChatModel`,
  `StubEmbeddingModel`). `kyc-service` adds context-appropriate extra adapters
  (`infrastructure/{vendor,watchlist,ratelimit}`) without breaking the convention.

## B5.5 — Event naming conventions: **PASS**

- **Topics** are `<aggregate>.<event>` constants in the single-source
  `Topics` class (`order.created`, `inventory.reserved`,
  `inventory.reservation-failed`, `inventory.released`, `payment.completed`,
  `payment.failed`, `order.confirmed`, `user.registered`, `kyc.approved`,
  `kyc.rejected`). No raw topic-string literals: every `@KafkaListener` and every
  `kafkaTemplate.send(...)` references a `Topics.*` constant.
- **`EventEnvelope<T>`** wraps every payload (eventId, eventType, version,
  occurredAt, traceId, correlationId, payload) — enabling consumer idempotency by
  `eventId`. Payload records are uniformly named `*Event`.
- **Producer↔consumer agreement** is enforced by the **C6 contract tests**
  (`common-events` `EventContractTest`): schema-lock + round-trip for all 11
  payloads, plus generic-envelope round-trip for the
  `ByteArrayJsonMessageConverter` path. Consumers (`inventory`, `payment`,
  `order`, `notification`, `kyc`) and producers (`auth`, `order`, `inventory`,
  `payment`, `kyc`) all bind through the same `Topics` + `common-events` records.

---

## Findings table

| # | Dimension | Service | Issue | Severity | Status |
|---|-----------|---------|-------|----------|--------|
| 1 | B5.3 | all 8 | `ErrorResponse` DTO duplicated per service | Low | Recommended (intentional — do NOT extract; acceptable per-service ownership) |
| 2 | B5.4 | kyc/product/order | New AI adapters/config added — confirm layout stays consistent as AI grows | Low | Recommended (monitor; currently conformant) |

**Counts by severity:** High 0 · Medium 0 · Low 2.
**Fixed now:** 0 (no violations found that warranted a code change).
**Recommended (advisory only):** 2 — see above.

---

## Safe fixes applied

**None.** The review found no stray framework import in `domain/`, no misplaced
class, no port/adapter bypass, and no within-service redundant DTO — so there was
nothing low-risk to remove or relocate. The codebase is already conformant.

---

## Top recommended (not-yet-done) improvements

1. **Keep `ErrorResponse` per-service (do not extract).** Tracked here only so a
   future reviewer doesn't mistake the 8 copies for drift. If error-shape
   consistency ever matters for the gateway/clients, document the contract in
   `docs/` rather than introducing a shared DTO module.
2. **Guard the conventions automatically.** Consider an ArchUnit test module (or a
   lightweight rule in the build) asserting: domain has zero framework imports,
   controllers depend only on `port.in`, application depends only on `port.out`,
   and no `record *Event` exists outside `common-events`. This would turn this
   manual B.5 review into a permanent, CI-enforceable guardrail.
3. **Document the AI port boundary in `docs/19`/`docs/20`.** The AI integration is
   exemplary (all Spring AI confined behind out-ports in `infrastructure/ai`); a
   short note codifying "Spring AI types never cross the port boundary" will keep
   future AI additions on the same path.

---

*Result of this review: no code changes needed; full reactor remains green.*
