# ============================================================================
#  Ordered deploy to Minikube (Windows / PowerShell). Idempotent.
#  Prereq:  minikube addons enable ingress
# ============================================================================
$ErrorActionPreference = "Stop"
$here = $PSScriptRoot

Write-Host "==> 1/6 Namespace"
kubectl apply -f "$here\namespace.yaml"

Write-Host "==> 2/6 Secrets + shared config"
kubectl apply -f "$here\secrets.yaml"
kubectl apply -f "$here\apps\_common-config.yaml"

Write-Host "==> 3/6 Infrastructure"
kubectl apply -f "$here\infra\"

Write-Host "==> Waiting for core infra..."
kubectl -n ecommerce rollout status statefulset/postgres --timeout=180s
kubectl -n ecommerce rollout status statefulset/kafka --timeout=180s

Write-Host "==> 4/6 Application services"
kubectl apply -f "$here\apps\"

Write-Host "==> 5/6 Edge TLS cert (dev self-signed) + NGINX controller hardening"
kubectl -n ecommerce get secret ecommerce-tls *> $null
if ($LASTEXITCODE -ne 0) {
    $tmp = New-Item -ItemType Directory -Path (Join-Path $env:TEMP ([guid]::NewGuid()))
    & openssl req -x509 -nodes -days 365 -newkey rsa:2048 `
        -keyout "$tmp\tls.key" -out "$tmp\tls.crt" `
        -subj "/CN=ecommerce.local/O=ecommerce" `
        -addext "subjectAltName=DNS:ecommerce.local"
    kubectl -n ecommerce create secret tls ecommerce-tls --cert="$tmp\tls.crt" --key="$tmp\tls.key"
    Remove-Item -Recurse -Force $tmp
}
# Controller-level hardening (ingress-nginx namespace must exist via the addon).
kubectl apply -f "$here\ingress-nginx-config.yaml"
if ($LASTEXITCODE -ne 0) { Write-Host "   (skipped controller config — is the ingress addon enabled?)" }

Write-Host "==> 6/6 Ingress (API gateway routes)"
kubectl apply -f "$here\ingress.yaml"

Write-Host "==> Done. Watch:  kubectl -n ecommerce get pods -w"
Write-Host "    Add to hosts file:  <minikube ip>  ecommerce.local grafana.local prometheus.local"
