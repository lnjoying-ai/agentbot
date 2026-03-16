#!/usr/bin/env bash
set -euo pipefail

echo "[agentbot] install build dependencies (Ubuntu)"
sudo apt-get update
sudo apt-get install -y \
  openjdk-17-jdk \
  maven \
  nodejs \
  npm \
  python3 \
  fakeroot \
  dpkg-dev

echo "[agentbot] done"
