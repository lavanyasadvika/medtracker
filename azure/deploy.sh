#!/usr/bin/env bash
# ═══════════════════════════════════════════════════════════════════
# MedTracker – redeploy to Azure
#
#   edit code → docker build + push (this script, run from WSL) →
#   ACR → az deployment group create → same FQDN, fresh containers,
#   Postgres untouched
#
# Run this every time you want to ship a code change:
#   cd azure && ./deploy.sh
# ═══════════════════════════════════════════════════════════════════
set -euo pipefail
cd "$(dirname "$0")"
REPO_ROOT="$(cd .. && pwd)"

if [ ! -f azure.env ]; then
  echo "azure/azure.env not found — copy azure.env.example to azure.env and fill it in first." >&2
  exit 1
fi
set -a
source azure.env
set +a

ACR_LOGIN_SERVER="${ACR_NAME}.azurecr.io"
APP_URL="http://${DNS_LABEL}.${LOCATION}.azurecontainer.io"
IMAGE_TAG="$(date +%Y%m%d%H%M%S)"

echo "==> Logging in to Azure / ACR"
az account show >/dev/null 2>&1 || az login
az acr login --name "$ACR_NAME"

echo "==> Building images (tag: $IMAGE_TAG)"
docker build -t "$ACR_LOGIN_SERVER/medtracker-backend:$IMAGE_TAG" \
             -t "$ACR_LOGIN_SERVER/medtracker-backend:latest" \
             "$REPO_ROOT/server"

docker build --build-arg "VITE_API_URL=${APP_URL}:8080/api" \
             -t "$ACR_LOGIN_SERVER/medtracker-frontend:$IMAGE_TAG" \
             -t "$ACR_LOGIN_SERVER/medtracker-frontend:latest" \
             "$REPO_ROOT/client"

docker build -f prometheus.Dockerfile \
             -t "$ACR_LOGIN_SERVER/medtracker-prometheus:$IMAGE_TAG" \
             -t "$ACR_LOGIN_SERVER/medtracker-prometheus:latest" \
             .

docker build -f grafana.Dockerfile \
             -t "$ACR_LOGIN_SERVER/medtracker-grafana:$IMAGE_TAG" \
             -t "$ACR_LOGIN_SERVER/medtracker-grafana:latest" \
             .

echo "==> Pushing images to ACR"
for img in backend frontend prometheus grafana; do
  docker push "$ACR_LOGIN_SERVER/medtracker-$img:$IMAGE_TAG"
  docker push "$ACR_LOGIN_SERVER/medtracker-$img:latest"
done

echo "==> Fetching ACR admin credentials"
ACR_USERNAME="$(az acr credential show --name "$ACR_NAME" --query username -o tsv)"
ACR_PASSWORD="$(az acr credential show --name "$ACR_NAME" --query "passwords[0].value" -o tsv)"

echo "==> Deploying container group (image tag: $IMAGE_TAG)"
az deployment group create \
  --resource-group "$RESOURCE_GROUP" \
  --template-file app.bicep \
  --parameters \
    location="$LOCATION" \
    dnsNameLabel="$DNS_LABEL" \
    acrLoginServer="$ACR_LOGIN_SERVER" \
    acrUsername="$ACR_USERNAME" \
    acrPassword="$ACR_PASSWORD" \
    imageTag="$IMAGE_TAG" \
    dbName="$DB_NAME" \
    dbUsername="$DB_USERNAME" \
    dbPassword="$DB_PASSWORD" \
    jwtSecret="$JWT_SECRET" \
    actuatorPassword="$ACTUATOR_PASSWORD" \
    groqApiKey="$GROQ_API_KEY" \
    groqMockEnabled="$GROQ_MOCK_ENABLED" \
    smtpUsername="$SMTP_USERNAME" \
    smtpPassword="$SMTP_PASSWORD" \
    grafanaAdminPassword="$GRAFANA_ADMIN_PASSWORD" \
  --output table

echo ""
echo "==> Done. Same URLs every redeploy:"
echo "    Frontend:   $APP_URL"
echo "    Backend:    $APP_URL:8080/api"
echo "    Grafana:    $APP_URL:3000"
echo "    Prometheus: $APP_URL:9090"
echo "    Zipkin:     $APP_URL:9411"
