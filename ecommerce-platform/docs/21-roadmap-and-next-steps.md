# 21 — Roadmap & Next Steps

> Single source of truth for **what's done** and **what we still need to do**. Local-only project (no CI/CD). Update the checkboxes as we go.
> **Owner** column: **You** = needs your action/decision · **Claude** = I can do it · **Platform** = infra/ops config (Kafka/k8s).

---

## ✅ Done so far (Phase 14 — KYC/Compliance via Spring AI)

| # | Item | Evidence |
|---|---|---|
| D1 | kyc-service module (Clean Architecture, port 8088, `kyc_db`) | compiles, 30 unit tests green |
| D2 | auth-service emits `user.registered` (commit-before-publish) | 12 tests green |
| D3 | order-service gates checkout on KYC (`order.kyc.gating.enabled`, fail-closed → 403) | 21 tests green |
| D4 | Real OFAC SDN watchlist ingestion (scheduled, gated; fixture for offline) | included in D1 tests |
| D5 | k8s manifest `k8s/apps/kyc-service.yaml` + `kyc_db` in Postgres init | mirrors siblings |
| D6 | `kyc-service` in `docker-compose.apps.yml` + topics (`user.registered`, `kyc.*` + `.DLT`) | compose + k8s Job |
| D7 | Security review + hardening (H1 SSRF, H2 DoS, H3 upload, M1-M4) | 50 tests green, commit `63070f0` |
| D8 | Docs: design `docs/19`, local setup + AI roadmap `docs/20` | — |
| D9 | Git initialized, all work committed (`24e5d20`, `63070f0`, `62ed1f8`) | author `rajat6633@gmail.com` |
| D10 | Phase 13 (CI/CD) dropped — local-only | README/CLAUDE.md updated |

---

## 🎯 Definition of Done — Phase 14 Exit Criteria

Phase 14 is **"done" only when every criterion below is observed working end-to-end** (not just unit-tested in isolation). These are the acceptance assertions the **Stage A4** (no-container) or **Stage B3** (container) smoke test must demonstrate. Status today: all are *implemented + unit-tested*, none yet *demonstrated end-to-end* — so all boxes stay unchecked until the smoke run proves them.

| # | Exit criterion | Today | Proven by |
|---|---|---|---|
| ☐ | User registration emits `user.registered` | unit ✓ (D2) | A4/B3 on the wire |
| ☐ | KYC service receives the event | impl ✓ | A4/B3 |
| ☐ | OFAC screening executes (local embeddings + pgvector) | unit ✓ (D4) | A4/B3 |
| ☐ | KYC decision persisted (`kyc_cases`, status set) | unit ✓ (D1) | A4/B3 |
| ☐ | `kyc.approved` **or** `kyc.rejected` published | unit ✓ (D1) | A4/B3 |
| ☐ | User can login | existing (Phase 4) ✓ | A4/B3 |
| ☐ | Order service enforces the KYC gate | unit ✓ (D3) | A4/B3 |
| ☐ | Approved user **can** place an order | unit ✓ | A4/B3 |
| ☐ | Rejected/unknown user receives **403** (fail-closed) | unit ✓ (D3) | A4/B3 |
| ☐ | Smoke test **documented** (steps + observed result) | — | appended to this doc / `docs/19` §Verification after the run |

> **Note on the 3-row dev watchlist:** to demonstrate *both* outcomes locally, register one user whose name does **not** match the fixture (→ clean screen → `kyc.approved` → order allowed) and one whose name **does** match a fixture row (→ hit → `MANUAL_REVIEW`/`kyc.rejected` → order 403). Real OFAC data is Stage D-future-3.

When all ten are checked and the smoke is documented, mark Phase 14 fully complete (today it's "code-complete + security-hardened", per the README checklist caveat).

---

## 🔜 Stage A — Make the Spring AI / KYC path actually run locally

Goal: register a user and watch the full KYC saga work on your machine.

- [x] **A1 — Verify Spring AI deps resolve** ✅ *(2026-06-15: `mvn -pl services/kyc-service -am -DskipTests compile` → BUILD SUCCESS in ~3s; M1 artifacts cached in local `.m2`, so resolves even offline.)*
- [x] **A2 — Pick the LLM lane** ✅ *(2026-06-15: **Ollama** — free, local.)*
- [x] **A3 — Set up Ollama** ✅ *(2026-06-15: Ollama 0.30.6 installed (winget, `:11434`), `llama3.2:3b` pulled. kyc-service wired: `spring-ai-ollama` starter + profile-gated ChatModel — both vendor `*.chat.enabled` default `false`, exactly one enabled per profile: **test→stub, local/docker/k8s→Ollama, cloud→Anthropic** (new `application-cloud.yml`). `ollama` service added to apps compose. 50 tests green. Embeddings unchanged.)*
- [ ] **A4 — Prove the KYC path (no containers)** · *Claude* · run auth + kyc + order as `java -jar`, register a user → screening (local embeddings) → `kyc.approved` → order allowed. Embeddings are already free/local; no key needed for the clean-screen path.

---

## 🔜 Stage B — Full containerized end-to-end (optional but proves the whole platform)

Goal: the entire stack running in Docker, real saga, gating live.

- [ ] **B1 — Un-wedge Docker** · *You* · restart Docker Desktop (it's currently hung — known host issue).
- [ ] **B2 — Build the changed images** · *Claude* · rebuild `auth`, `order`, `kyc` images (needs `repo.spring.io` + HuggingFace egress for the embedding model on first boot).
- [ ] **B3 — Bring up stack + run smoke** · *Claude* · infra compose + apps compose, then `register → KYC approve → login → product → stock → cart → place order → CONFIRMED`. (For a non-KYC quick run, set `ORDER_KYC_GATING_ENABLED=false`.)

---

## 🔭 Stage B.5 — Architecture Review (after B; prevents drift)

Goal: catch architectural drift **now**, while there are 8 services, before more get built. · *Owner: Claude*

- [ ] **B5.1 — Clean Architecture conformance** · every service keeps `api → application → domain → infrastructure`; domain has zero framework deps; the dependency rule holds (no inward references to Spring/JPA/Kafka).
- [ ] **B5.2 — No port/adapter bypass** · no service reaches infrastructure directly; all I/O goes through application ports.
- [ ] **B5.3 — Remove duplicated DTOs** · de-dupe response/command shapes; shared event payloads live only in `common-events`.
- [ ] **B5.4 — Package & naming consistency** · base pkg `com.ecommerce.<svc>`, consistent layer package names across services.
- [ ] **B5.5 — Event naming conventions** · topics/payloads follow one convention (`<aggregate>.<event>`, `EventEnvelope<T>`); no drift between producers and consumers.

---

## 🔜 Stage C — Production-readiness: security, observability & contracts

From the security review plus the observability and testing gaps. Do before adding more AI features (Stage D).

- [N/A] **C1 — Kafka topic ACLs on `kyc.*`** · *Platform* · **N/A for this local-only project (no production).** Would restrict `kyc.approved`/`kyc.rejected` producers to kyc-service so a forged event can't approve a user. Revisit only if ever deployed to a shared/prod broker.
- [x] **C2 — JWT `aud` claim + audience validation** ✅ *(2026-06-15: auth mints `aud: ecommerce-services` on the access token; kyc + order decoders validate it via `DelegatingOAuth2TokenValidator(issuer, audience)`; `auth.jwt.audience` configurable; TestTokens updated. 83 tests green. Follow-up: extend issuer+audience validation to the other 5 resource servers.)*
- [x] **C3 — Per-user document-upload rate limit** ✅ *(2026-06-15: per-user Resilience4j `RateLimiterRegistry.rateLimiter("kyc-upload-"+userId)` on `POST /api/kyc/{userId}/documents`, default 5/min/user (`kyc.upload.rate-limit.per-user-per-minute`), behind a `UploadRateLimiterPort`; over-limit → 429. Independent per user; service-wide limiter retained. 56 tests green.)*
- [N/A] **C4 — Spring AI data-retention/DPA** · **N/A — using local Ollama (A2), nothing leaves the machine.** Only relevant if switched to the cloud Claude profile.
- [x] **C5 — Distributed tracing (end-to-end)** ✅ *(2026-06-15: Micrometer Tracing + OTLP export were already wired in all 8 services; the real gap was **Kafka observation** — added `spring.kafka.template/listener.observation-enabled: true` to the 6 Kafka services so trace context rides in Kafka headers and `auth → kyc → order` is ONE trace, not fragments. test profile sets sampling 0.0 (no collector needed). 133 tests green. See it in Stage B: Grafana → Explore → Tempo.)*
- [x] **C6 — Event contract tests** ✅ *(2026-06-15: 23 tests in `common-events` — schema-lock (committed JSON snapshots) + producer↔consumer round-trip + generic `EventEnvelope<T>` path, using a runtime-faithful ObjectMapper (`spring.json.add.type.headers=false`, ISO-8601 Instants). Covers the KYC trio + all saga events. A renamed/removed/retyped field now fails with a clear "contract DRIFT" message instead of silently breaking a consumer. Lightweight Jackson approach, no Spring Cloud Contract.)*

---

## 🔮 Stage D — Optional future work (ranked by ROI)

Ordered for **portfolio / interview impact** — the things a recruiter or interviewer notices first come first. Semantic search is the single most demo-able feature here; a live distributed trace is the other, now elevated to **C5** (do it before this stage).

- [x] **D1 — Semantic Product Search** ✅ *(2026-06-15: product-service mirrors the kyc AI stack — local Transformers `EmbeddingModel` + pgvector (Flyway V2), stub + in-memory `SimpleVectorStore` in test/local. `GET /api/products/search?q=&page=&size=` → `PageResponse<ScoredProductResponse>` (product + similarity score). Products indexed on create/update/delete, fail-soft (Resilience4j `product-ai`); admin `POST /api/products/reindex` + local startup reindex. 21 tests green, offline.)*
- [x] **D2 — Dependency Check (SCA)** ✅ *(2026-06-15: **OWASP Dependency-Check** `dependency-check-maven` 10.0.4 wired into the parent pom under an opt-in `owasp` profile — runs ONLY via `mvn -Powasp verify` (or `mvn -Powasp org.owasp:dependency-check-maven:aggregate`), never on a normal `mvn test`/`verify`. Uses the `aggregate` goal to scan every module into one combined report (HTML + JSON in the root `target/`). **Report-only by default** (`failBuildOnCVSS=11`, unreachable) so it never breaks the build. **First run downloads the full NVD DB — large/slow**; get a free [NVD API key](https://nvd.nist.gov/developers/request-an-api-key) and pass `-DnvdApiKey=YOUR_KEY` to make it fast. To turn it into a gate: `mvn -Powasp verify -Dformat=ALL -DfailBuildOnCVSS=7`. Relevant given the Spring AI `1.0.0-M1` milestone deps. Optional adjuncts: SpotBugs / PMD.)*
- [ ] **D3 — Real OFAC Feed** · flip `kyc.watchlist.ingestion.enabled=true` with network egress so screening uses the live OFAC SDN list instead of the 3-row dev fixture. Turns the KYC demo from toy to credible.
- [x] **D4 — Support Chatbot** ✅ *(2026-06-15: `POST /api/orders/support/chat` (authenticated, `{question}`→`{answer}`) in order-service. RAG over the user's OWN order history from `order_db` (local, no cross-service call; last 10 orders as context); `userId` from the JWT (owner-scoped — can't ask about others' orders). Profile-gated ChatModel like kyc (test→stub, local/docker→Ollama, cloud→Anthropic); Resilience4j `order-support-ai` fail-soft (outage→friendly message, never 500); question+context delimited (prompt-injection hygiene). 29 tests green, offline.)*
- [x] **D5 — Recommendations** ✅ *(2026-06-15: `GET /api/products/{id}/recommendations?limit=` → top-N most-similar products by vector similarity (excludes self), content-based via product embeddings — no cross-service call. Co-purchase/collaborative filtering noted as future (needs order data). Covered by the product-service tests above.)*

---

## Recommended order

**A1 → A4** (AI path working locally) → **C2** (JWT audience validation — implementation, not hardening) → **C5** (distributed tracing — so the smoke and every later saga debug is observable) → **B** (full container proof, now traceable) → **B.5** (architecture review — prevent drift while it's cheap) → **C1 + C3 + C6** (Kafka ACLs · per-user rate limit · event contract tests) → **D** (AI features, ranked by ROI — build **D1 Semantic Product Search** first; it's the standout portfolio feature).

> **Why C2 before B:** audience validation is a small, high-value correctness fix that belongs with the implementation — doing it first means the smoke exercises the *final* token contract, not one you'll change right after.
> **Why C5 before B:** with 8 services, a saga that misbehaves during the smoke is far easier to debug as one Grafana/Tempo trace than by tailing 8 logs — so wire tracing before you lean on the end-to-end run.
> **Why B.5 + C6 before D:** lock the architecture and event contracts before adding more event-driven AI features, so new work builds on a stable base instead of compounding drift.

**Right now the single next action is A1** (confirm the Spring AI deps download) — that unblocks everything else in Stage A.
