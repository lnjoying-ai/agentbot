$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
[xml]$pom = Get-Content (Join-Path $root "pom.xml")
$version = $pom.project.version
$upgradeUuid = "8c0a37b2-8d46-4f2e-8c6d-7ef00b4e2c8a"
$jar = "agentbot-$version.jar"
$targetJar = Join-Path $root ("target\" + $jar)

if (!(Test-Path $targetJar)) {
  Write-Host "Building jar..."
  mvn -q -DskipTests package -f (Join-Path $root "pom.xml")
}

$stage = Join-Path $root "build\package\input"
if (Test-Path $stage) { Remove-Item $stage -Recurse -Force }
New-Item -ItemType Directory -Path $stage | Out-Null
Copy-Item $targetJar $stage
Copy-Item (Join-Path $root "config") (Join-Path $stage "config") -Recurse -Force
if (Test-Path (Join-Path $root "workspace")) {
  Copy-Item (Join-Path $root "workspace") (Join-Path $stage "workspace") -Recurse -Force
} else {
  New-Item -ItemType Directory -Path (Join-Path $stage "workspace") -Force | Out-Null
}
if (Test-Path (Join-Path $root "frontend")) {
  Copy-Item (Join-Path $root "frontend") (Join-Path $stage "frontend") -Recurse -Force
}

$logoSource = Join-Path $root "frontend\public\blue-logo.png"
if (Test-Path $logoSource) {
  $logoTarget = Join-Path $stage "blue-logo.png"  
  Copy-Item $logoSource $logoTarget -Force
}


$uninstallScript = Join-Path $stage "uninstall-keep-data.bat"
@'
@echo off
setlocal
set "BASE=%~dp0"
echo [INFO] Removing files except workspace and config...
for /d %%D in ("%BASE%*") do (
  if /I not "%%~nxD"=="workspace" if /I not "%%~nxD"=="config" (
    rd /s /q "%%D"
  )
)
for %%F in ("%BASE%*") do (
  if /I not "%%~nxF"=="uninstall-keep-data.bat" (
    del /f /q "%%F"
  )
)
echo [INFO] Done.
pause
'@ | Set-Content -Path $uninstallScript -Encoding ASCII

if (-not (Get-Command jpackage -ErrorAction SilentlyContinue)) {
  throw "jpackage not found in PATH (JDK 17+ required)."
}
if (-not (Get-Command light.exe -ErrorAction SilentlyContinue) -or -not (Get-Command candle.exe -ErrorAction SilentlyContinue)) {
  throw "WiX Toolset not found (light.exe/candle.exe). Install WiX 3.x+ and add it to PATH."
}

$dest = Join-Path $root "dist"
New-Item -ItemType Directory -Path $dest -Force | Out-Null

& jpackage `
  --type msi `
  --name agentbot `
  --app-version $version `
  --input $stage `
  --main-jar $jar `
  --dest $dest `
  --vendor "agentbot" `
  --win-upgrade-uuid $upgradeUuid `
  --java-options "-Dagentbot.userDataRoot=%USERPROFILE%\agentbot" `
  --win-menu `
  --win-shortcut `
  --win-menu-group "agentbot"


if ($LASTEXITCODE -ne 0) {
  throw "jpackage failed with exit code $LASTEXITCODE"
}

$msi = Get-ChildItem $dest -Filter "*.msi" | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $msi) {
  throw "MSI not generated. Check jpackage output above."
}
Write-Host "MSI generated: $($msi.FullName)"

