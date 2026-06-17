# Storefront — Customer Web App

The customer-facing React storefront for the e-commerce microservices platform.
Browse the catalog, manage a cart, check out, and **track orders live** as they
move through the asynchronous (Kafka saga) fulfillment pipeline.

> First frontend in the platform. Lives under `frontend/storefront/`, leaving
> room for an `frontend/admin/` console as a sibling app later.

## Stack

| Concern        | Choice                                            |
| -------------- | ------------------------------------------------- |
| Build / dev    | **Vite 5** (Node 18 compatible)                   |
| Language       | **TypeScript** (strict), React 18                 |
| Routing        | react-router-dom v6                               |
| Server state   | TanStack Query v5                                 |
| Styling        | Tailwind CSS v3 + shadcn/ui-style components      |
| HTTP           | axios (JWT bearer + silent refresh interceptor)   |
| Toasts         | sonner                                            |

> Pinned to Vite 5 / Tailwind 3 deliberately — Vite 6+ and Tailwind 4 require
> Node 20+, and this environment runs Node 18.16.

## Getting started

```bash
npm install
cp .env.example .env.local   # optional — defaults work with the dev proxy
npm run dev                  # http://localhost:5173
```

### Talking to the backend

The app calls a single `/api` front door, mirroring the production NGINX Ingress.
In dev, the Vite proxy (see `vite.config.ts`) routes `/api/*` to the services.
Two modes:

- **Default (per-service):** `/api/<ctx>` → the matching service on
  `localhost:8081–8087`. Use this when running the services individually
  (`mvn spring-boot:run`) or via `infra/docker-compose.yml`.
- **Single origin:** set `VITE_API_PROXY_TARGET=https://ecommerce.local` to send
  all `/api/*` to the real ingress (or a `kubectl port-forward`).

The app renders fine with the backend down — lists show loading/empty/error
states — but cart, checkout, and order tracking need the services running.

## Scripts

| Command             | What it does                          |
| ------------------- | ------------------------------------- |
| `npm run dev`       | Start the Vite dev server             |
| `npm run build`     | Type-check (`tsc -b`) + production build |
| `npm run preview`   | Preview the production build          |
| `npm run typecheck` | Type-check only                       |

## Architecture notes

- **Auth.** Access token kept in memory; refresh token in `localStorage`. The
  axios response interceptor does a single-flight refresh on `401` and replays
  the failed request. A persisted refresh token resurrects the session on reload.
- **Async orders.** `POST /api/orders` returns `PENDING` immediately. The order
  page polls `GET /api/orders/{id}` every 2s and renders a step timeline
  (Placed → Inventory reserved → Payment → Confirmed), stopping at terminal
  states (`CONFIRMED` / `PAYMENT_FAILED` / `REJECTED` / `CANCELLED`).
- **Money.** Amounts are kept as strings (backend `BigDecimal`) and parsed only
  for display via `Intl.NumberFormat`, avoiding float drift.

## Project layout

```
src/
  components/
    ui/         shadcn-style primitives (button, card, sheet, …)
    layout/     header, footer, app shell
    product/    product card + skeleton
    cart/       slide-over cart drawer
    order/      status badge + live timeline
  features/
    auth/       AuthProvider, useAuth
    cart/       cart drawer open/close context
  hooks/        TanStack Query hooks (products, cart, orders)
  lib/api/      typed API client (axios) + DTO types + token store
  pages/        route components
  router.tsx    route table
  app.tsx       providers (query, auth, cart UI, toaster)
```
