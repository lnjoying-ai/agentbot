$ErrorActionPreference = "Stop"

$Root = (Split-Path $PSScriptRoot -Parent)
Push-Location $Root

if (Test-Path "frontend") {
  Write-Host "[agentbot] build frontend"
  Push-Location "frontend"
  npm install
  npm run build
  Pop-Location
  
  $StaticDir = Join-Path $Root "src\main\resources\static"
  if (-not (Test-Path $StaticDir)) {
    New-Item -ItemType Directory -Force $StaticDir | Out-Null
  }
  Write-Host "[agentbot] copy frontend assets to $StaticDir"
  Copy-Item -Path "frontend\dist\*" -Destination $StaticDir -Recurse -Force
}

Write-Host "[agentbot] build backend"
mvn -q -DskipTests package


$ConfigDir = Join-Path $Root "config"
$TargetConfigDir = Join-Path $Root "target\config"
New-Item -ItemType Directory -Force $TargetConfigDir | Out-Null
$ConfigFile = Join-Path $ConfigDir "agentbot.yml"
if (Test-Path $ConfigFile) {
  Copy-Item $ConfigFile $TargetConfigDir -Force
}

Write-Host "[agentbot] package complete"
Pop-Location

