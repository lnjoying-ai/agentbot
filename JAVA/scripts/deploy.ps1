$ErrorActionPreference = "Stop"

$Root = (Split-Path $PSScriptRoot -Parent)
Push-Location $Root

if (-not $env:AGENTBOT_CONFIG) {
  $env:AGENTBOT_CONFIG = Join-Path $Root "config\agentbot.yml"
}
if (-not (Test-Path $env:AGENTBOT_CONFIG)) {
  Write-Host "[agentbot] config missing: $env:AGENTBOT_CONFIG"
  exit 1
}

Write-Host "[agentbot] docker compose deploy"
docker compose up -d --build

Pop-Location

