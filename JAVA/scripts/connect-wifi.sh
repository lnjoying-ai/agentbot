#!/usr/bin/env bash
set -euo pipefail

SSID="${1:-}"
PASSWORD="${2:-}"

if [[ -z "${SSID}" || -z "${PASSWORD}" ]]; then
  echo "usage: $(basename "$0") <WiFi name> <WiFi password>" >&2
  exit 1
fi

echo "[1/4] start WiFi"
nmcli r wifi on

echo "[2/4] Scan available networks"
nmcli dev wifi list

echo "[3/4] connect to WiFi"
nmcli dev wifi connect "${SSID}" password "${PASSWORD}"

echo "[4/4] Verify connection status"
nmcli con show --active
