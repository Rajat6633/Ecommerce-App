# ============================================================================
#  storefront — multi-stage: Node 20 build → nginx:alpine serve (non-root)
# ============================================================================

# ---- Stage 1: build ----
FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json* ./
RUN npm ci --prefer-offline
COPY . .
# VITE_API_BASE_URL is baked into the static bundle at build time.
# In K8s the app is served same-origin as the API (ecommerce.local), so /api is correct.
ARG VITE_API_BASE_URL=/api
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
RUN npm run build

# ---- Stage 2: runtime ----
# nginx-unprivileged runs as UID 101 (nginx user), port 8080 — no root required.
FROM nginxinc/nginx-unprivileged:1.27-alpine AS runtime

COPY --from=build /app/dist /usr/share/nginx/html
# conf.d/default.conf is the server block; the global nginx.conf is pre-configured for non-root.
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD wget -qO- http://localhost:8080/health || exit 1
