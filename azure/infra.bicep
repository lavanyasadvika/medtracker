// ═══════════════════════════════════════════════════════════════════
// MedTracker – ONE-TIME infrastructure bootstrap
//
// Creates the Azure Container Registry every MedTracker deployment
// pushes images to. Run this ONCE per environment
// (`az deployment group create -f azure/infra.bicep ...`) — do NOT
// include it in the repeatable redeploy loop.
//
// Postgres runs as a plain container inside the app's container group
// (see azure/app.bicep) rather than as a managed Azure Database —
// managed Postgres (Flexible Server) needs public network access +
// password auth, which many org subscriptions block by policy
// (Entra-only auth required), and fixing that properly needs a private
// VNet + Entra-token DB auth, out of scope here. Trade-off: the
// database resets on every redeploy since it isn't backed by a
// persistent volume (Azure Files is incompatible with Postgres's data
// directory ownership requirements over SMB).
// ═══════════════════════════════════════════════════════════════════

@description('Azure region for all resources')
param location string = resourceGroup().location

@description('Globally-unique Azure Container Registry name (letters/numbers only)')
param acrName string

resource acr 'Microsoft.ContainerRegistry/registries@2023-07-01' = {
  name: acrName
  location: location
  sku: {
    name: 'Basic'
  }
  properties: {
    adminUserEnabled: true
  }
}

output acrLoginServer string = acr.properties.loginServer
