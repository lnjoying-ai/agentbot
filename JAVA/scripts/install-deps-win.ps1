$ErrorActionPreference = "Stop"

if (-not (Get-Command winget -ErrorAction SilentlyContinue)) {
  Write-Host "winget 未安装，请先安装 Windows 应用安装器。"
  exit 1
}

Write-Host "[agentbot] install build dependencies (Windows)"
winget install -e --id EclipseAdoptium.Temurin.17.JDK
winget install -e --id Apache.Maven
winget install -e --id OpenJS.NodeJS.LTS
winget install -e --id Python.Python.3.12
winget install -e --id WiX.Toolset

Write-Host "[agentbot] ensure npm"
if (Get-Command npm -ErrorAction SilentlyContinue) {
  npm install -g npm@latest
} else {
  Write-Host "npm 未找到，请重新打开终端后重试 npm install -g npm@latest。"
}

Write-Host "[agentbot] done. 请重新打开终端验证 java/mvn/node/npm/python/jpackage/light/candle。"

