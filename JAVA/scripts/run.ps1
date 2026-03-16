$ErrorActionPreference = "Stop"

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
$encodingFlags = "-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8"
if (-not $env:JAVA_TOOL_OPTIONS) {
  $env:JAVA_TOOL_OPTIONS = $encodingFlags
} elseif ($env:JAVA_TOOL_OPTIONS -notmatch "-Dfile\.encoding=UTF-8" -and $env:JAVA_TOOL_OPTIONS -notmatch "-Dsun\.jnu\.encoding=UTF-8") {
  $env:JAVA_TOOL_OPTIONS = ($env:JAVA_TOOL_OPTIONS + " " + $encodingFlags).Trim()
}


$Root = (Split-Path $PSScriptRoot -Parent)
Push-Location $Root

if (-not $env:AGENTBOT_CONFIG) {
  $env:AGENTBOT_CONFIG = Join-Path $Root "config\agentbot.yml"
}
if (-not (Test-Path $env:AGENTBOT_CONFIG)) {
  Write-Host "[agentbot] config missing: $env:AGENTBOT_CONFIG"
  exit 1
}

[xml]$pom = Get-Content (Join-Path $Root "pom.xml")
$version = $pom.project.version
$jar = "agentbot-$version.jar"

mvn -q -DskipTests package
if ($LASTEXITCODE -ne 0) {
  throw "mvn package failed with exit code $LASTEXITCODE"
}

$jarPath = Join-Path $Root ("target\\" + $jar)
if (-not (Test-Path $jarPath)) {
  throw "Jar not found: $jarPath"
}

$runDir = Join-Path $Root "build\run"
New-Item -ItemType Directory -Path $runDir -Force | Out-Null
$runJar = Join-Path $runDir $jar
Copy-Item $jarPath $runJar -Force

java -jar $runJar



Pop-Location

