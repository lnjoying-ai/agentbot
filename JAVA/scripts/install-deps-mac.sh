#!/usr/bin/env bash
set -euo pipefail

if ! command -v brew >/dev/null 2>&1; then
  echo "[agentbot] Homebrew not found. Install from https://brew.sh first."
  exit 1
fi

echo "[agentbot] install build dependencies (macOS)"
brew install openjdk@17 maven node python@3

echo "[agentbot] ensure npm"
if command -v npm >/dev/null 2>&1; then
  npm install -g npm@latest
else
  echo "[agentbot] npm not found, please re-open your terminal and run: npm install -g npm@latest"
fi

if [ -d "/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk" ]; then
  sudo ln -sfn /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-17.jdk
elif [ -d "/usr/local/opt/openjdk@17/libexec/openjdk.jdk" ]; then
  sudo ln -sfn /usr/local/opt/openjdk@17/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-17.jdk
fi

echo "[agentbot] done"
