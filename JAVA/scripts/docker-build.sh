#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

cd "$ROOT_DIR"
mvn -q -DskipTests package

CONFIG_DIR="$ROOT_DIR/config"
TARGET_CONFIG_DIR="$ROOT_DIR/target/config"
mkdir -p "$TARGET_CONFIG_DIR"
if [ -f "$CONFIG_DIR/agentbot.yml" ]; then
  cp "$CONFIG_DIR/agentbot.yml" "$TARGET_CONFIG_DIR/agentbot.yml"
fi

docker build -t agentbot:latest .
