# Admin Console

The internal admin console for the e-commerce microservices platform. Manage
the product catalog, categories, and inventory. **ROLE_ADMIN only** — a valid
non-admin login is refused at the door.

Sibling app to `frontend/storefront/`; shares the same stack and API-client
patterns (UI primitives, axios client, token store were lifted from it).

## Stack

Vite 5 · React 18 · TypeScript (strict) · Tailwind v3 · shadcn/ui-style
components · TanStack Query v5 · react-router v6 · axios · sonner.

> Pinned to Vite 5 / Tailwind 3 (Node 18.16 here; Vite 6 / Tailwind 4 need Node 20).

## Getting started

```bash
npm install
npm run dev        # http://localhost:5174  (storefront uses 5173)
```

Talks to the same `/api` front door as the storefront via the Vite proxy — see
`vite.config.ts`. Default fans `/api/<ctx>` to services on `localhost:8081–8087`;
set `VITE_API_PROXY_TARGET=https://ecommerce.local` to hit the real ingress.

You'll need an **ADMIN** user. Register a customer via the storefront / auth API,
then promote it to `ADMIN` in `auth_db` (no self-serve admin signup by design).

## Features

| Area       | Capabilities                                                       | Endpoints |
| ---------- | ------------------------------------------------------------------ | --------- |
| Products   | List (search + paginate), create, edit, activate/deactivate, delete | `GET/POST/PUT/DELETE /api/products` |
| Categories | List, create (with optional parent)                                | `GET/POST /api/categories` |
| Inventory  | Per-product view (on hand / reserved / available), set levels, receive stock | `GET/PUT /api/inventory/{id}`, `POST …/receive` |

Inventory is managed per-product (the inventory-service has no list endpoint),
so the Inventory screen lists products and opens an adjust dialog per row.

## Auth model

Same as the storefront: access token in memory, refresh token in `localStorage`,
single-flight `401→refresh` interceptor. **Additionally**, `login()` and the
session-bootstrap both verify the `ADMIN` role and tear down the session if it's
missing (`NotAuthorizedError`).

## Scripts

`npm run dev` · `npm run build` (tsc -b + vite build) · `npm run preview` · `npm run typecheck`
