$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
& (Join-Path $root "scripts\clean.bat")
[xml]$pom = Get-Content (Join-Path $root "pom.xml")

$version = $pom.project.version
$upgradeUuid = "8c0a37b2-8d46-4f2e-8c6d-7ef00b4e2c8a"
$jar = "agentbot-$version.jar"
$targetJar = Join-Path $root ("target\" + $jar)

Write-Host "[agentbot] build artifacts"
& (Join-Path $root "scripts\build.ps1")

if (!(Test-Path $targetJar)) {
  throw "Missing jar after build: $targetJar"
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
$frontendDist = Join-Path $root "frontend\dist"
if (Test-Path $frontendDist) {
  $frontendStage = Join-Path $stage "frontend"
  New-Item -ItemType Directory -Path $frontendStage -Force | Out-Null
  Copy-Item $frontendDist (Join-Path $frontendStage "dist") -Recurse -Force
}


$logoSource = Join-Path $root "frontend\public\dragon-logo.png"
$logoPngSource = Join-Path $root "dragon-logo.png"
$logoIcoSource = Join-Path $root "dragon-logo.ico"

if (Test-Path $logoSource) {
  $logoTarget = Join-Path $stage "dragon-logo.png"
  Copy-Item $logoSource $logoTarget -Force
}
if (Test-Path $logoPngSource) {
  Copy-Item $logoPngSource (Join-Path $stage "dragon-logo.png") -Force
}
if (Test-Path $logoIcoSource) {
  Copy-Item $logoIcoSource (Join-Path $stage "dragon-logo.ico") -Force
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

$tempDir = Join-Path $root "build\jpackage\tmp"
New-Item -ItemType Directory -Path $tempDir -Force | Out-Null

$iconIco = Join-Path $root "dragon-logo.ico"

$jpackageArgs = @(

  "--type", "msi",
  "--name", "agentbot",
  "--app-version", $version,
  "--input", $stage,
  "--main-jar", $jar,
  "--dest", $dest,
  "--temp", $tempDir,
  "--verbose",
  "--vendor", "agentbot",
  "--win-upgrade-uuid", $upgradeUuid,
  "--java-options", "-Dagentbot.userDataRoot=%USERPROFILE%\agentbot",
  "--win-menu",
  "--win-shortcut",
  "--win-menu-group", "agentbot"
)
if (Test-Path $iconIco) {
  $jpackageArgs += @("--icon", $iconIco)
}

& jpackage @jpackageArgs




if ($LASTEXITCODE -ne 0) {
  throw "jpackage failed with exit code $LASTEXITCODE"
}

$msi = Get-ChildItem $dest -Filter "*.msi" | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $msi) {
  throw "MSI not generated. Check jpackage output above."
}
Write-Host "MSI generated: $($msi.FullName)"

