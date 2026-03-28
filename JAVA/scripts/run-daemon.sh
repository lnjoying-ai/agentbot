#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PID_FILE="${AGENTBOT_PID:-$ROOT_DIR/agentbot.pid}"
LOG_DIR="${AGENTBOT_LOG_DIR:-$ROOT_DIR/logs}"
LOG_FILE="${AGENTBOT_LOG:-$LOG_DIR/agentbot.log}"
CONFIG_FILE="${AGENTBOT_CONFIG:-$ROOT_DIR/config/agentbot.yml}"

mkdir -p "$LOG_DIR"

is_running() {
  if [[ -f "$PID_FILE" ]]; then
    local pid
    pid=$(cat "$PID_FILE")
    if [[ -n "$pid" ]] && kill -0 "$pid" >/dev/null 2>&1; then
      return 0
    fi
  fi
  return 1
}

resolve_server_port() {
  if [[ ! -f "$CONFIG_FILE" ]]; then
    echo ""
    return
  fi
  awk '
    function indent(s){ match(s,/^[ ]*/); return RLENGTH }
    {
      ind = indent($0)
      if (ind == 0 && $1 == "server:") { in_server = 1; next }
      if (ind == 0) { in_server = 0 }
      if (in_server && $1 == "port:") {
        gsub("\"", "", $2)
        print $2
        exit
      }
    }
  ' "$CONFIG_FILE"
}

resolve_control_port() {
  if [[ ! -f "$CONFIG_FILE" ]]; then
    echo ""
    return
  fi
  awk '
    function indent(s){ match(s,/^[ ]*/); return RLENGTH }
    {
      ind = indent($0)
      if (ind == 0 && $1 == "agentbot:") { in_agentbot = 1; in_browser = 0; next }
      if (ind == 0) { in_agentbot = 0; in_browser = 0 }
      if (in_agentbot && ind == 2 && $1 == "browser:") { in_browser = 1; next }
      if (in_agentbot && ind == 2 && $1 != "browser:") { in_browser = 0 }
      if (in_browser && $1 == "controlPort:") {
        gsub("\"", "", $2)
        print $2
        exit
      }
    }
  ' "$CONFIG_FILE"
}

stop_browser_control() {
  local control_port server_port
  control_port=$(resolve_control_port)
  if [[ -z "$control_port" || ! "$control_port" =~ ^[0-9]+$ || "$control_port" -le 0 ]]; then
    server_port=$(resolve_server_port)
    if [[ -n "$server_port" && "$server_port" =~ ^[0-9]+$ && "$server_port" -gt 0 ]]; then
      control_port=$((server_port + 2))
    else
      return
    fi
  fi

  local pids=""
  if command -v lsof >/dev/null 2>&1; then
    pids=$(lsof -tiTCP:"$control_port" -sTCP:LISTEN 2>/dev/null || true)
  elif command -v fuser >/dev/null 2>&1; then
    pids=$(fuser -n tcp "$control_port" 2>/dev/null || true)
  fi

  if [[ -n "$pids" ]]; then
    echo "[agentbot] stopping browser control (port=$control_port, pid=$pids)"
    kill $pids >/dev/null 2>&1 || true
  fi
}

start_agentbot() {
  if is_running; then
    echo "[agentbot] already running (pid=$(cat "$PID_FILE"))"
    exit 0
  fi

  nohup "$ROOT_DIR/scripts/run.sh" > "$LOG_FILE" 2>&1 &
  local pid=$!
  echo "$pid" > "$PID_FILE"
  sleep 1
  if is_running; then
    echo "[agentbot] started (pid=$pid)"
    echo "[agentbot] logs: $LOG_FILE"
  else
    echo "[agentbot] failed to start. check logs: $LOG_FILE"
    exit 1
  fi
}

stop_agentbot() {
  if ! is_running; then
    echo "[agentbot] not running"
    rm -f "$PID_FILE"
    stop_browser_control
    exit 0
  fi

  local pid
  pid=$(cat "$PID_FILE")
  kill "$pid" >/dev/null 2>&1 || true
  for _ in {1..20}; do
    if ! kill -0 "$pid" >/dev/null 2>&1; then
      rm -f "$PID_FILE"
      stop_browser_control
      echo "[agentbot] stopped"
      exit 0
    fi
    sleep 0.2
  done

  echo "[agentbot] force killing (pid=$pid)"
  kill -9 "$pid" >/dev/null 2>&1 || true
  rm -f "$PID_FILE"
  stop_browser_control
}

status_agentbot() {
  if is_running; then
    echo "[agentbot] running (pid=$(cat "$PID_FILE"))"
  else
    echo "[agentbot] not running"
  fi
}

case "${1:-start}" in
  start) start_agentbot ;;
  stop) stop_agentbot ;;
  restart) stop_agentbot; start_agentbot ;;
  status) status_agentbot ;;
  *)
    echo "Usage: $0 {start|stop|restart|status}"
    exit 1
    ;;
 esac
