#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

# ── Formatting helpers ────────────────────────────────────────────────────────
CYAN='\033[0;36m'; GREEN='\033[0;32m'; YELLOW='\033[0;33m'
BOLD='\033[1m'; DIM='\033[2m'; RESET='\033[0m'

header() { echo -e "\n${BOLD}${CYAN}$1 ── ${RESET}$2"; }
done_()  { echo -e " ${GREEN}✔${RESET} $1"; }
warn()   { echo -e " ${YELLOW}⚠${RESET}  $1"; }
# ─────────────────────────────────────────────────────────────────────────────

SERVICES=("gateway-service" "report-service" "processing-service" "ingest-service")

header "1/2" "Scaling down Kubernetes deployments"
for svc in "${SERVICES[@]}"; do
  if kubectl get deploy "$svc" > /dev/null 2>&1; then
    kubectl scale deploy "$svc" --replicas=0
    done_ "Scaled down $svc"
  else
    warn "$svc deployment not found, skipping"
  fi
done

header "2/2" "Stopping Docker Compose infrastructure"
docker compose stop
done_ "Infrastructure stopped"

echo -e "\n${BOLD}${GREEN}══ All services stopped ══${RESET}"
echo -e "${DIM}Data and cluster preserved. Use ./run start to bring everything back up.${RESET}\n"
