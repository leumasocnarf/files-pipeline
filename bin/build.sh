#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

CLUSTER_NAME="microservices"
COMPOSE_NETWORK="files-pipeline_default"
SERVICES=("gateway-service" "ingest-service" "processing-service" "report-service")

# ── Formatting helpers ────────────────────────────────────────────────────────
CYAN='\033[0;36m'; GREEN='\033[0;32m'; YELLOW='\033[0;33m'
BOLD='\033[1m'; DIM='\033[2m'; RESET='\033[0m'

header() { echo -e "\n${BOLD}${CYAN}$1 ── ${RESET}$2"; }
step()   { echo -e " ${GREEN}=>${RESET} $1"; }
info()   { echo -e " ${DIM}$1${RESET}"; }
done_()  { echo -e " ${GREEN}✔${RESET} $1"; }
warn()   { echo -e " ${YELLOW}⚠${RESET}  $1"; }
# ─────────────────────────────────────────────────────────────────────────────

header "1/5" "Building Docker images"
for svc in "${SERVICES[@]}"; do
  step "Building $svc..."
  docker build -t "files-pipeline-${svc}:latest" "./${svc}"
done

header "2/5" "Starting infrastructure"
docker compose up -d postgres kafka schema-registry keycloak redis

header "3/5" "Waiting for Schema Registry"
until docker exec schema-registry curl -sf http://localhost:8085/ > /dev/null 2>&1; do
  info "Schema Registry not ready yet, retrying in 2s..."
  sleep 2
done
done_ "Schema Registry ready"

header "4/5" "Configuring Schema Registry"
docker exec schema-registry curl -s -X PUT http://localhost:8085/config \
  -H "Content-Type: application/json" \
  -d '{"compatibility": "BACKWARD"}' > /dev/null
done_ "BACKWARD compatibility set"

header "5/5" "Creating kind cluster"
if kind get clusters 2>/dev/null | grep -q "^${CLUSTER_NAME}$"; then
  warn "Cluster '$CLUSTER_NAME' already exists, skipping creation."
else
  kind create cluster --name "$CLUSTER_NAME" --config kind-config.yaml
fi

step "Connecting kind to Docker Compose network..."
if docker inspect "$CLUSTER_NAME-control-plane" \
     --format '{{range $k,$v := .NetworkSettings.Networks}}{{$k}} {{end}}' \
   | grep -q "$COMPOSE_NETWORK"; then
  warn "Already connected to $COMPOSE_NETWORK."
else
  docker network connect "$COMPOSE_NETWORK" "$CLUSTER_NAME-control-plane"
  done_ "Connected to $COMPOSE_NETWORK"
fi

step "Loading images into kind..."
for svc in "${SERVICES[@]}"; do
  IMAGE="files-pipeline-${svc}:latest"
  info "Loading $IMAGE..."
  kind load docker-image "$IMAGE" --name "$CLUSTER_NAME"
done

echo -e "\n${BOLD}${GREEN}══ Build complete! Use./run start to deploy services ══${RESET}\n"
