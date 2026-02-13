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

mvn -q -DskipTests package
java -jar target\agentbot-0.1.0-SNAPSHOT.jar

Pop-Location

