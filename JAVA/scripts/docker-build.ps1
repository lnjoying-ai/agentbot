$ErrorActionPreference = "Stop"

$Root = (Split-Path $PSScriptRoot -Parent)
Push-Location $Root

mvn -q -DskipTests package

$ConfigDir = Join-Path $Root "config"
$TargetConfigDir = Join-Path $Root "target\config"
New-Item -ItemType Directory -Force $TargetConfigDir | Out-Null
$ConfigFile = Join-Path $ConfigDir "agentbot.yml"
if (Test-Path $ConfigFile) {
  Copy-Item $ConfigFile $TargetConfigDir -Force
}

docker build -t agentbot:latest .

Pop-Location

