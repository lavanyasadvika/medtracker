// ═══════════════════════════════════════════════════════════════════
// MedTracker – REDEPLOYABLE application unit
//
// One Azure Container Instances group holding all 6 services (backend,
// frontend, prometheus, grafana, zipkin, postgres) sharing a single
// public IP + DNS label, so the app's URL never changes across
// redeploys — same pattern as local docker-compose, just in the cloud.
//
// Only the frontend (80) and the backend API (8080) are exposed on the
// public IP. Postgres, Prometheus, Grafana and Zipkin still listen on
// their ports (so containers reach each other over localhost, e.g.
// Grafana → Prometheus/Zipkin, backend → Postgres) but are unreachable
// from the internet since they're not in ipAddress.ports.
//
// Postgres runs as a plain container with no external volume, so its
// data resets on every redeploy — see azure/infra.bicep for why managed
// Postgres isn't used here.
//
// Run this on every code change:
//   az deployment group create -g <rg> -f azure/app.bicep -p ...
// ═══════════════════════════════════════════════════════════════════

@description('Azure region — must match the region used for infra.bicep')
param location string = resourceGroup().location

@description('Container group name')
param containerGroupName string = 'medtracker-app'

@description('Globally-unique DNS label — the app URL is http://<label>.<region>.azurecontainer.io')
param dnsNameLabel string

@description('ACR login server, e.g. medtrackeracr.azurecr.io')
param acrLoginServer string

@description('ACR admin username')
param acrUsername string

@secure()
param acrPassword string

@description('Unique tag to force a fresh pull on every redeploy (e.g. a timestamp or git SHA). Do not reuse "latest" here.')
param imageTag string

@description('Application database name')
param dbName string = 'medtracker'

@description('Postgres username for the in-group container')
param dbUsername string = 'postgres'

@secure()
param dbPassword string

@secure()
param jwtSecret string

@secure()
param actuatorPassword string

@secure()
param groqApiKey string = ''

param groqBaseUrl string = 'https://api.groq.com/openai/v1'
param groqModel string = 'qwen/qwen3.6-27b'
param groqMockEnabled string = 'true'
param openfdaBaseUrl string = 'https://api.fda.gov'

param smtpHost string = 'sandbox.smtp.mailtrap.io'
param smtpPort string = '2525'
param smtpUsername string = ''
@secure()
param smtpPassword string = ''
param smtpSslTrust string = 'sandbox.smtp.mailtrap.io'
param mailFrom string = 'noreply@medtracker.local'

@secure()
param grafanaAdminPassword string = 'admin'

var appUrl = 'http://${dnsNameLabel}.${location}.azurecontainer.io'

resource containerGroup 'Microsoft.ContainerInstance/containerGroups@2023-05-01' = {
  name: containerGroupName
  location: location
  properties: {
    osType: 'Linux'
    restartPolicy: 'Always'
    imageRegistryCredentials: [
      {
        server: acrLoginServer
        username: acrUsername
        password: acrPassword
      }
    ]
    ipAddress: {
      type: 'Public'
      dnsNameLabel: dnsNameLabel
      // Only the frontend and the public API are reachable from the
      // internet. Postgres, Prometheus, Grafana and Zipkin still listen
      // on their ports below (so containers in this group can reach
      // them over localhost), but since those ports aren't listed here,
      // nothing outside the group can reach them.
      ports: [
        { protocol: 'tcp', port: 80 }
        { protocol: 'tcp', port: 8080 }
      ]
    }
    containers: [
      {
        name: 'postgres'
        properties: {
          image: 'postgres:15-alpine'
          resources: {
            requests: { cpu: json('0.5'), memoryInGB: json('0.5') }
          }
          ports: [ { port: 5432 } ]
          environmentVariables: [
            { name: 'POSTGRES_DB', value: dbName }
            { name: 'POSTGRES_USER', value: dbUsername }
            { name: 'POSTGRES_PASSWORD', secureValue: dbPassword }
          ]
        }
      }
      {
        name: 'backend'
        properties: {
          image: '${acrLoginServer}/medtracker-backend:${imageTag}'
          resources: {
            requests: { cpu: 1, memoryInGB: json('1.5') }
          }
          ports: [ { port: 8080 } ]
          environmentVariables: [
            { name: 'SPRING_PROFILES_ACTIVE', value: 'docker' }
            { name: 'DB_HOST', value: 'localhost' }
            { name: 'DB_PORT', value: '5432' }
            { name: 'DB_NAME', value: dbName }
            { name: 'DB_USERNAME', value: dbUsername }
            { name: 'DB_PASSWORD', secureValue: dbPassword }
            { name: 'JWT_SECRET', secureValue: jwtSecret }
            { name: 'REACT_ALLOWED_ORIGINS', value: appUrl }
            { name: 'GROQ_API_KEY', secureValue: groqApiKey }
            { name: 'GROQ_BASE_URL', value: groqBaseUrl }
            { name: 'GROQ_MODEL', value: groqModel }
            { name: 'GROQ_MOCK_ENABLED', value: groqMockEnabled }
            { name: 'OPENFDA_BASE_URL', value: openfdaBaseUrl }
            { name: 'SMTP_HOST', value: smtpHost }
            { name: 'SMTP_PORT', value: smtpPort }
            { name: 'SMTP_USERNAME', value: smtpUsername }
            { name: 'SMTP_PASSWORD', secureValue: smtpPassword }
            { name: 'SMTP_SSL_TRUST', value: smtpSslTrust }
            { name: 'MAIL_FROM', value: mailFrom }
            { name: 'ACTUATOR_PASSWORD', secureValue: actuatorPassword }
            { name: 'ZIPKIN_ENDPOINT', value: 'http://localhost:9411/api/v2/spans' }
          ]
        }
      }
      {
        name: 'frontend'
        properties: {
          // VITE_API_URL is baked in at build time (see azure/deploy.sh) —
          // no runtime env vars needed here.
          image: '${acrLoginServer}/medtracker-frontend:${imageTag}'
          resources: {
            requests: { cpu: json('0.5'), memoryInGB: json('0.5') }
          }
          ports: [ { port: 80 } ]
        }
      }
      {
        name: 'prometheus'
        properties: {
          image: '${acrLoginServer}/medtracker-prometheus:${imageTag}'
          resources: {
            requests: { cpu: json('0.5'), memoryInGB: json('0.5') }
          }
          ports: [ { port: 9090 } ]
        }
      }
      {
        name: 'grafana'
        properties: {
          image: '${acrLoginServer}/medtracker-grafana:${imageTag}'
          resources: {
            requests: { cpu: json('0.5'), memoryInGB: json('0.5') }
          }
          ports: [ { port: 3000 } ]
          environmentVariables: [
            { name: 'GF_SECURITY_ADMIN_PASSWORD', secureValue: grafanaAdminPassword }
          ]
        }
      }
      {
        name: 'zipkin'
        properties: {
          // Pulled straight from Docker Hub — no custom config needed,
          // so it never goes through ACR. Pin below :3: that tag's
          // Alpine/JDK25 build is broken under some container runtimes.
          image: 'openzipkin/zipkin:2.24'
          resources: {
            requests: { cpu: json('0.5'), memoryInGB: json('0.5') }
          }
          ports: [ { port: 9411 } ]
        }
      }
    ]
  }
}

output fqdn string = containerGroup.properties.ipAddress.fqdn
output frontendUrl string = 'http://${containerGroup.properties.ipAddress.fqdn}'
output backendUrl string = 'http://${containerGroup.properties.ipAddress.fqdn}:8080/api'
// Postgres/Prometheus/Grafana/Zipkin are internal-only (see ipAddress.ports
// above) — not reachable at these addresses from outside the container
// group. Listed here only as a reminder of which port each uses internally.
output postgresInternalPort string = 'localhost:5432 (from within the container group only)'
output grafanaInternalPort string = 'localhost:3000 (from within the container group only)'
output prometheusInternalPort string = 'localhost:9090 (from within the container group only)'
output zipkinInternalPort string = 'localhost:9411 (from within the container group only)'
