#!/usr/bin/env bash
# ============================================================================
#  Ordered deploy to Minikube. Namespace + secrets/config first, then infra,
#  then apps, then the NGINX Ingress gateway. Idempotent — safe to re-run.
#  Prereq:  minikube addons enable ingress
# ============================================================================
set -euo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "==> 1/6 Namespace"
kubectl apply -f "$HERE/namespace.yaml"

echo "==> 2/6 Secrets + shared config"
kubectl apply -f "$HERE/secrets.yaml"
kubectl apply -f "$HERE/apps/_common-config.yaml"

echo "==> 3/6 Infrastructure (postgres, kafka, redis, observability)"
kubectl apply -f "$HERE/infra/"

echo "==> Waiting for core infra to be ready..."
kubectl -n ecommerce rollout status statefulset/postgres --timeout=180s
kubectl -n ecommerce rollout status statefulset/kafka --timeout=180s || true

echo "==> 4/6 Application services"
kubectl apply -f "$HERE/apps/"

echo "==> 5/6 Edge TLS cert (dev self-signed) + NGINX controller hardening"
if ! kubectl -n ecommerce get secret ecommerce-tls >/dev/null 2>&1; then
  TMP="$(mktemp -d)"
  openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
    -keyout "$TMP/tls.key" -out "$TMP/tls.crt" \
    -subj "/CN=ecommerce.local/O=ecommerce" \
    -addext "subjectAltName=DNS:ecommerce.local"
  kubectl -n ecommerce create secret tls ecommerce-tls \
    --cert="$TMP/tls.crt" --key="$TMP/tls.key"
  rm -rf "$TMP"
fi
# Controller-level hardening (ingress-nginx namespace must exist via the addon).
kubectl apply -f "$HERE/ingress-nginx-config.yaml" || \
  echo "   (skipped controller config — is the ingress addon enabled?)"

echo "==> 6/6 Ingress (API gateway routes)"
kubectl apply -f "$HERE/ingress.yaml"

echo "==> Done. Watch rollout with:  kubectl -n ecommerce get pods -w"
echo "    Add to hosts file:  <minikube ip>  ecommerce.local grafana.local prometheus.local"
