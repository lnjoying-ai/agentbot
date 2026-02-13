#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "[agentbot] build backend"
cd "$ROOT_DIR"
mvn -q -DskipTests package

if [ -d "$ROOT_DIR/frontend" ]; then
  echo "[agentbot] build frontend"
  cd "$ROOT_DIR/frontend"
  npm install
  npm run build
fi

CONFIG_DIR="$ROOT_DIR/config"
TARGET_CONFIG_DIR="$ROOT_DIR/target/config"
mkdir -p "$TARGET_CONFIG_DIR"
if [ -f "$CONFIG_DIR/agentbot.yml" ]; then
  cp "$CONFIG_DIR/agentbot.yml" "$TARGET_CONFIG_DIR/agentbot.yml"
fi

echo "[agentbot] package complete"
