#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

cd "$ROOT_DIR"
ENCODING_FLAGS="-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8"
if [ -z "${JAVA_TOOL_OPTIONS:-}" ]; then
  export JAVA_TOOL_OPTIONS="$ENCODING_FLAGS"
elif [[ "$JAVA_TOOL_OPTIONS" != *"-Dfile.encoding=UTF-8"* && "$JAVA_TOOL_OPTIONS" != *"-Dsun.jnu.encoding=UTF-8"* ]]; then
  export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS} ${ENCODING_FLAGS}"
fi
CONFIG_FILE="${AGENTBOT_CONFIG:-$ROOT_DIR/config/agentbot.yml}"

if [ ! -f "$CONFIG_FILE" ]; then
  echo "[agentbot] config missing: $CONFIG_FILE"
  exit 1
fi
export AGENTBOT_CONFIG="$CONFIG_FILE"
mvn -q -DskipTests package

JAR_PATH=$(ls -1t "$ROOT_DIR"/target/agentbot-*.jar 2>/dev/null | grep -vE '(sources|javadoc)\.jar$' | head -n 1 || true)
if [ -z "$JAR_PATH" ]; then
  echo "[agentbot] jar not found in $ROOT_DIR/target"
  exit 1
fi

java -jar "$JAR_PATH"
