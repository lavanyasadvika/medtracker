#!/usr/bin/env bash
# ═══════════════════════════════════════════════════════════════════
# MedTracker – ONE-TIME Azure infrastructure bootstrap
#
# Creates the resource group, Azure Container Registry, and managed
# Postgres server. Run this ONCE per environment. Re-running it is
# safe (idempotent) but it should never be part of your normal
# redeploy loop — that's azure/deploy.sh.
#
# Usage:
#   cd azure && ./setup-infra.sh
# ═══════════════════════════════════════════════════════════════════
set -euo pipefail
cd "$(dirname "$0")"

if [ ! -f azure.env ]; then
  echo "azure/azure.env not found — copy azure.env.example to azure.env and fill it in first." >&2
  exit 1
fi
set -a
source azure.env
set +a

echo "==> Logging in to Azure (opens a browser)"
az account show >/dev/null 2>&1 || az login

echo "==> Creating resource group: $RESOURCE_GROUP ($LOCATION)"
az group create --name "$RESOURCE_GROUP" --location "$LOCATION" --output none

echo "==> Deploying ACR"
az deployment group create \
  --resource-group "$RESOURCE_GROUP" \
  --template-file infra.bicep \
  --parameters \
    location="$LOCATION" \
    acrName="$ACR_NAME" \
  --output table

echo ""
echo "==> Infra ready. Outputs:"
az deployment group show \
  --resource-group "$RESOURCE_GROUP" \
  --name infra \
  --query properties.outputs \
  --output json
