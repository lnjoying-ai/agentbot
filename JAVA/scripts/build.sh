#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

if [ -d "$ROOT_DIR/frontend" ]; then
  echo "[agentbot] build frontend"
  cd "$ROOT_DIR/frontend"
  if [ -f "package-lock.json" ]; then
    npm ci --no-audit --fund false
  else
    npm install --no-audit --fund false
  fi
  npm run build

  STATIC_DIR="$ROOT_DIR/src/main/resources/static"
  if [ -d "$ROOT_DIR/frontend/dist" ]; then
    echo "[agentbot] sync frontend dist to backend static"
    rm -rf "$STATIC_DIR"
    mkdir -p "$STATIC_DIR"
    cp -R "$ROOT_DIR/frontend/dist/." "$STATIC_DIR/"
  fi
fi

echo "[agentbot] build backend"
cd "$ROOT_DIR"
mvn -q -DskipTests package

CONFIG_DIR="$ROOT_DIR/config"
TARGET_CONFIG_DIR="$ROOT_DIR/target/config"
mkdir -p "$TARGET_CONFIG_DIR"
if [ -f "$CONFIG_DIR/agentbot.yml" ]; then
  cp "$CONFIG_DIR/agentbot.yml" "$TARGET_CONFIG_DIR/agentbot.yml"
fi

echo "[agentbot] package complete"
