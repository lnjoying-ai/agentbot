#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

cd "$ROOT_DIR"
CONFIG_FILE="${AGENTBOT_CONFIG:-$ROOT_DIR/config/agentbot.yml}"
if [ ! -f "$CONFIG_FILE" ]; then
  echo "[agentbot] config missing: $CONFIG_FILE"
  exit 1
fi
export AGENTBOT_CONFIG="$CONFIG_FILE"
mvn -q -DskipTests package
java -jar target/agentbot-0.1.0-SNAPSHOT.jar
