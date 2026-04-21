#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

CLUSTER_NAME="microservices"

# ── Formatting helpers ────────────────────────────────────────────────────────
CYAN='\033[0;36m'; GREEN='\033[0;32m'; YELLOW='\033[0;33m'
BOLD='\033[1m'; DIM='\033[2m'; RESET='\033[0m'

header() { echo -e "\n${BOLD}${CYAN}$1 ──${RESET} $2"; }
done_()  { echo -e " ${GREEN}✔${RESET} $1"; }
warn()   { echo -e " ${YELLOW}⚠${RESET}  $1"; }
# ─────────────────────────────────────────────────────────────────────────────

header "1/3" "Deleting Kubernetes resources"
if kind get clusters 2>/dev/null | grep -q "^${CLUSTER_NAME}$"; then
  kubectl delete -f k8s/ --ignore-not-found
  done_ "Resources deleted."
else
  warn "No cluster found, skipping."
fi

header "2/3" "Deleting kind cluster"
if kind get clusters 2>/dev/null | grep -q "^${CLUSTER_NAME}$"; then
  kind delete cluster --name "$CLUSTER_NAME"
  done_ "Cluster '$CLUSTER_NAME' deleted."
else
  warn "Cluster already deleted, skipping."
fi

header "3/3" "Stopping Docker Compose"
read -rp "  Remove Postgres data volume? (y/N): " remove_volume
if [[ "$remove_volume" =~ ^[Yy]$ ]]; then
  docker compose down -v
  done_ "Infra stopped, volumes removed."
else
  docker compose down
  done_ "Infra stopped, volumes preserved."
fi

echo -e "\n${BOLD}${GREEN}── Teardown complete ──${RESET}\n"
