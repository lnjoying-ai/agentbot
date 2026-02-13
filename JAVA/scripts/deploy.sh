#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

cd "$ROOT_DIR"
CONFIG_FILE="${AGENTBOT_CONFIG:-$ROOT_DIR/config/agentbot.yml}"
if [ ! -f "$CONFIG_FILE" ]; then
  echo "[agentbot] config missing: $CONFIG_FILE"
  exit 1
fi

echo "[agentbot] docker compose deploy"
docker compose up -d --build
