#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

# ── Formatting helpers ────────────────────────────────────────────────────────
CYAN='\033[0;36m'; GREEN='\033[0;32m'; YELLOW='\033[0;33m'; RED='\033[0;31m'
BOLD='\033[1m'; DIM='\033[2m'; RESET='\033[0m'

header() { echo -e "\n${BOLD}${CYAN}$1 ── ${RESET}$2"; }
step()   { echo -e " ${GREEN}=>${RESET} $1"; }
info()   { echo -e " ${DIM}$1${RESET}"; }
done_()  { echo -e " ${GREEN}✔${RESET} $1"; }
warn()   { echo -e " ${YELLOW}⚠${RESET}  $1"; }
fail()   { echo -e " ${RED}✘${RESET}  $1"; }
# ─────────────────────────────────────────────────────────────────────────────

TIMEOUT=120

wait_for_ready() {
  local label=$1
  local name=$2
  step "Waiting for $name to be ready..."
  if kubectl wait --for=condition=ready pod -l "app=$label" --timeout="${TIMEOUT}s"; then
    done_ "$name is ready"
  else
    fail "$name failed to become ready within ${TIMEOUT}s"
    kubectl logs deploy/"$label" --tail=20
    exit 1
  fi
}

header "1/3" "Checking prerequisites"
if ! kind get clusters 2>/dev/null | grep -q "^microservices$"; then
  fail "Kind cluster 'microservices' not found. Use ./run build first."
  exit 1
fi

if [ ! -f k8s/secrets.yaml ]; then
  fail "k8s/secrets.yaml not found."
  info "Run: cp secrets.yaml.example k8s/secrets.yaml and fill in the values."
  exit 1
fi

if ! docker ps --format '{{.Names}}' | grep -q "postgres"; then
  step "Starting Docker Compose infrastructure..."
  docker compose up -d postgres kafka schema-registry keycloak redis
  until docker exec schema-registry curl -sf http://localhost:8085/ > /dev/null 2>&1; do
    info "Schema Registry not ready yet, retrying in 2s..."
    sleep 2
  done
fi
done_ "Cluster and infrastructure detected"

header "2/3" "Deploying services (staggered)"

step "Applying secrets..."
kubectl apply -f k8s/secrets.yaml

step "Deploying ingest-service..."
kubectl apply -f k8s/ingest-service.yaml
wait_for_ready "ingest-service" "Ingest Service"

step "Deploying processing-service..."
kubectl apply -f k8s/processing-service.yaml
wait_for_ready "processing-service" "Processing Service"

step "Deploying report-service..."
kubectl apply -f k8s/report-service.yaml
wait_for_ready "report-service" "Report Service"

step "Deploying gateway-service..."
kubectl apply -f k8s/gateway-service.yaml
wait_for_ready "gateway-service" "Gateway Service"

header "3/3" "Status"
echo ""
kubectl get pods
echo -e "\n${BOLD}${GREEN}══ All services running! ══${RESET}\n"