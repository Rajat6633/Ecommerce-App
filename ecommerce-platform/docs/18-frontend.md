# 18 — Frontend (Storefront + Admin Console)

Two React single-page apps live under [`frontend/`](../frontend/), talking to the
same `/api` front door the NGINX Ingress exposes:

| App | Path | Dev port | Audience | Purpose |
| --- | ---- | -------- | -------- | ------- |
| **Storefront** | [`frontend/storefront/`](../frontend/storefront/) | 5173 | Customers (`CUSTOMER`) | Browse → cart → checkout → live order tracking |
| **Admin console** | [`frontend/admin/`](../frontend/admin/) | 5174 | Operators (`ADMIN`) | Product / category CRUD + inventory management |

Both are standalone Vite apps (not a workspace yet — the admin lifted the
storefront's UI primitives, axios client, and token store; extracting a shared
`@ecommerce/ui` package is a natural future step).

## Stack & why

| Concern | Choice | Rationale |
| ------- | ------ | --------- |
| Build / dev | **Vite 5** | Fast; **pinned to 5** because Vite 6+ needs Node 20 and this env runs Node 18.16 |
| Language | **TypeScript (strict)** + React 18 | Matches the platform's production bar |
| Styling | **Tailwind v3** + shadcn/ui-style components | Pinned to v3 (v4 needs Node 20); components hand-rolled (Radix + cva) to avoid the shadcn CLI's node/network deps |
| Server state | **TanStack Query v5** | Caching, polling, mutations with cache updates |
| Routing | react-router-dom v6 | — |
| HTTP | axios | Bearer token + single-flight refresh interceptor |
| Toasts | sonner | — |

## Talking to the backend

The apps call a single `/api` base, mirroring the production NGINX Ingress.
In dev, each app's `vite.config.ts` proxies `/api`:

- **Default (per-service):** `/api/<ctx>` → the matching service on
  `localhost:8081–8087`. Use when running services individually
  (`mvn spring-boot:run`) or via `infra/docker-compose.yml` for infra + local services.
- **Single origin:** set `VITE_API_PROXY_TARGET=https://ecommerce.local` to send
  all `/api/*` to the real ingress (or a `kubectl port-forward`).

This means **no CORS handling is needed in the frontend** — the dev proxy makes
calls same-origin, and in production the apps are served behind the same ingress
host as the API (CORS is configured at the ingress edge — see [docs/05](05-api-gateway-design.md)).

## Authentication (both apps)

RS256 JWT, issued by auth-service ([docs/10](10-auth-service.md)):

- **Access token** kept in memory (not persisted) — minimises XSS blast radius.
- **Refresh token** persisted to `localStorage` — survives reload; on first load
  the app silently re-mints an access token via `POST /api/auth/refresh`.
- The axios response interceptor does a **single-flight refresh on 401** and
  replays the failed request; concurrent 401s share one refresh, not N.
- The **admin console additionally enforces the `ADMIN` role** at login and on
  session bootstrap — a valid non-admin login is refused (`NotAuthorizedError`).

There is no self-serve admin signup. Create a customer (storefront / auth API),
then promote it to `ADMIN` in `auth_db`.

## Storefront highlights

- **Async order saga as UX.** `POST /api/orders` returns `PENDING` immediately;
  the order page **polls `GET /api/orders/{id}` every 2s** and renders a 4-step
  timeline — Placed → Inventory reserved → Payment → Confirmed — stopping at
  terminal states (`CONFIRMED` / `PAYMENT_FAILED` / `REJECTED` / `CANCELLED`).
  This makes the choreographed Kafka saga ([docs/04](04-kafka-topic-design.md),
  [docs/14](14-order-service.md)) visible to a non-engineer.
- Guest browsing; auth gates only at cart/checkout/orders.
- Cart/order DTOs carry only `productId`; the UI resolves names via
  `GET /api/products/{id}` (cached, deduped) for human-readable lines.
- Product images are deterministic placeholders (picsum by SKU) — there is no
  image service in the backend.

## Admin console highlights

- **Products** — searchable, paginated table; create / edit / activate-deactivate
  / delete (`GET/POST/PUT/DELETE /api/products`).
- **Categories** — list + create with optional parent (`GET/POST /api/categories`).
- **Inventory** — the inventory-service has no list endpoint, so the screen lists
  products and opens a per-product adjust dialog: view on-hand/reserved/available,
  set absolute levels (`PUT /api/inventory/{id}`), and receive stock
  (`POST /api/inventory/{id}/receive`).

## Conventions

- **Money** stays a string end-to-end (backend `BigDecimal`), parsed only for
  display via `Intl.NumberFormat` — no float drift.
- Filters/pagination live in the URL (`useSearchParams`) — shareable, back-safe.
- Each app: `npm install`, then `npm run dev`. `npm run build` runs
  `tsc -b` (strict) then `vite build`.

## Running locally

```bash
# 1. Infra (Postgres, Kafka, …)
cd infra && cp .env.example .env && docker compose up -d

# 2. The 7 services (each in its own shell), JDK 21:
#    cd services/<svc> && ./mvnw spring-boot:run   # ports 8081–8087

# 3. Storefront
cd frontend/storefront && npm install && npm run dev   # http://localhost:5173

# 4. Admin (optional, separate shell)
cd frontend/admin && npm install && npm run dev        # http://localhost:5174
```

## Status

Both apps **build green** (`tsc -b` strict + `vite build`) and boot (dev server
HTTP 200) on Node 18.16. Not yet exercised end-to-end against a live running
backend; the API contract is built from the service DTOs documented in
[docs/10–16](10-auth-service.md). No automated frontend tests yet (a candidate
for a future phase: Vitest + Testing Library + Playwright e2e).
