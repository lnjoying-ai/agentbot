#!/usr/bin/env bash
set -euo pipefail

SSID="${1:-}"
GATEWAY_IP="${2:-}"
STATIC_IP="${3:-}"

if [[ -z "${SSID}" || -z "${GATEWAY_IP}" || -z "${STATIC_IP}" ]]; then
  echo "usage: $(basename "$0") \"WiFi name\" \"gateway IP\" \"fixed IP\"" >&2
  exit 1
fi

echo "[1/2] fixed IP address"
nmcli connection modify "${SSID}" \
  ipv4.method manual \
  ipv4.addresses "${STATIC_IP}" \
  ipv4.gateway "${GATEWAY_IP}" \
  ipv4.dns "8.8.8.8,223.5.5.5"

echo "[2/2] Refresh configuration"
nmcli connection up "${SSID}"
