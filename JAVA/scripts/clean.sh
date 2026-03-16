#!/usr/bin/env bash
set -euo pipefail

printf '[INFO] Starting cleanup process for Agentbot project...\n'

# Current directory is Agentbot/JAVA
BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/"

# 1. Clean Backend (Maven)
if [ -d "${BASE_DIR}target" ]; then
  printf '[CLEAN] Removing backend target directory: %s\n' "${BASE_DIR}target"
  rm -rf "${BASE_DIR}target"
fi

# 2. Clean Frontend (Vite/Vue)
if [ -d "${BASE_DIR}frontend/dist" ]; then
  printf '[CLEAN] Removing frontend build directory: %s\n' "${BASE_DIR}frontend/dist"
  rm -rf "${BASE_DIR}frontend/dist"
fi

# 2.1 Clean top-level dist (if exists)
if [ -d "${BASE_DIR}dist" ]; then
  printf '[CLEAN] Removing dist directory: %s\n' "${BASE_DIR}dist"
  rm -rf "${BASE_DIR}dist"
fi

# 2.2 Clean top-level build (if exists)
if [ -d "${BASE_DIR}build" ]; then
  printf '[CLEAN] Removing build directory: %s\n' "${BASE_DIR}build"
  rm -rf "${BASE_DIR}build"
fi

# 2.3 Clean static resources files (keep directory)
if [ -d "${BASE_DIR}src/main/resources/static" ]; then
  printf '[CLEAN] Removing static files under: %s\n' "${BASE_DIR}src/main/resources/static"
  find "${BASE_DIR}src/main/resources/static" -maxdepth 1 -type f -print0 | xargs -0 rm -f
fi

# 2.4 Clean static assets files (keep directory)
if [ -d "${BASE_DIR}src/main/resources/static/assets" ]; then
  printf '[CLEAN] Removing static assets files under: %s\n' "${BASE_DIR}src/main/resources/static/assets"
  find "${BASE_DIR}src/main/resources/static/assets" -maxdepth 1 -type f -print0 | xargs -0 rm -f
fi

# 3. Clean Log Files (HS Error logs and build logs)
printf '[CLEAN] Removing log files...\n'
if [ -f "${BASE_DIR}build_log.txt" ]; then
  rm -f "${BASE_DIR}build_log.txt"
fi

# hs_err logs are in the root directory
rm -f "${BASE_DIR}../../hs_err_pid"*.log 2>/dev/null || true

# agentbot log directory (config/log)
if [ -d "${BASE_DIR}log" ]; then
  printf '[CLEAN] Removing agentbot log directory: %s\n' "${BASE_DIR}log"
  rm -rf "${BASE_DIR}log"
fi

# remove config/node.yml
if [ -f "${BASE_DIR}config/node.yml" ]; then
  printf '[CLEAN] Removing config node.yml: %s\n' "${BASE_DIR}config/node.yml"
  rm -f "${BASE_DIR}config/node.yml"
fi

# 4. Clean Workspace Sessions data (all agents)
printf '[CLEAN] Cleaning agent sessions...\n'
if [ -d "${BASE_DIR}workspace/agents" ]; then
  for agent_dir in "${BASE_DIR}workspace/agents"/*; do
    if [ -d "${agent_dir}/sessions" ]; then
      agent_name="$(basename "${agent_dir}")"
      printf '[CLEAN] Removing sessions for agent: %s\n' "${agent_name}"
      rm -rf "${agent_dir}/sessions"
      mkdir -p "${agent_dir}/sessions"
    fi
  done
fi

# Clean old sessions directory (if exists from pre-migration)
if [ -d "${BASE_DIR}workspace/sessions" ]; then
  printf '[CLEAN] Removing old workspace sessions directory\n'
  rm -rf "${BASE_DIR}workspace/sessions"
fi

printf '[INFO] Cleanup completed successfully.\n'
