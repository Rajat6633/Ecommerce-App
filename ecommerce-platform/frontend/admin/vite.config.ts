import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'node:path'

// Dev proxy mirrors the production NGINX Ingress (single /api front door).
//
// Two modes, controlled by VITE_API_PROXY_TARGET:
//  - UNSET (default): route each /api/<ctx> to its service on localhost
//    (matches running the services individually or via docker-compose).
//  - SET (e.g. https://ecommerce.local): route ALL /api/* to that one
//    origin (matches hitting the real ingress / a port-forward).
const SERVICE_PORTS: Record<string, number> = {
  '/api/auth': 8081,
  '/api/products': 8082,
  '/api/categories': 8082,
  '/api/inventory': 8083,
  '/api/cart': 8084,
  '/api/orders': 8085,
  '/api/payments': 8086,
  '/api/notifications': 8087,
}

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const singleTarget = env.VITE_API_PROXY_TARGET

  const proxy = singleTarget
    ? {
        '/api': {
          target: singleTarget,
          changeOrigin: true,
          secure: false, // dev ingress uses a self-signed cert
        },
      }
    : Object.fromEntries(
        Object.entries(SERVICE_PORTS).map(([ctx, port]) => [
          ctx,
          { target: `http://localhost:${port}`, changeOrigin: true },
        ]),
      )

  return {
    plugins: [react()],
    resolve: {
      alias: { '@': path.resolve(__dirname, './src') },
    },
    server: {
      port: 5174, // storefront uses 5173; admin runs alongside on 5174
      proxy,
    },
  }
})
