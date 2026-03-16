#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
"$ROOT/scripts/clean.sh"
VERSION="$(ROOT="$ROOT" python3 - <<'PY'
import os
import xml.etree.ElementTree as ET
root = ET.parse(f"{os.environ['ROOT']}/pom.xml").getroot()
ns = {'m': 'http://maven.apache.org/POM/4.0.0'}
ver = root.find('m:version', ns)
print(ver.text.strip() if ver is not None else '0.0.0')
PY
)"
JAR="agentbot-${VERSION}.jar"

echo "[agentbot] build artifacts"
"$ROOT/scripts/build.sh"

if [[ ! -f "$ROOT/target/$JAR" ]]; then
  echo "[ERROR] Missing jar after build: $ROOT/target/$JAR"
  exit 1
fi

STAGE="$ROOT/build/package/input"
rm -rf "$STAGE"
mkdir -p "$STAGE"
cp "$ROOT/target/$JAR" "$STAGE/"
cp -R "$ROOT/config" "$STAGE/config"
if [[ -d "$ROOT/workspace" ]]; then
  cp -R "$ROOT/workspace" "$STAGE/workspace"
else
  mkdir -p "$STAGE/workspace"
fi
if [[ -d "$ROOT/frontend" ]]; then
  cp -R "$ROOT/frontend" "$STAGE/frontend"
fi

UNINSTALL="$STAGE/uninstall-keep-data.sh"
cat > "$UNINSTALL" <<'SH'
#!/usr/bin/env bash
set -euo pipefail
BASE="$(cd "$(dirname "$0")" && pwd)"
find "$BASE" -maxdepth 1 -mindepth 1 \
  ! -name "workspace" ! -name "config" ! -name "uninstall-keep-data.sh" \
  -exec rm -rf {} +
echo "[INFO] Done."
SH
chmod +x "$UNINSTALL"

command -v jpackage >/dev/null 2>&1 || { echo "jpackage not found (JDK 17+ required)."; exit 1; }

DEST="$ROOT/dist"
mkdir -p "$DEST"

JPACKAGE_ARGS=(
  --type deb
  --name agentbot
  --app-version "$VERSION"
  --input "$STAGE"
  --main-jar "$JAR"
  --dest "$DEST"
  --vendor "agentbot"
)

HAS_MENU_DIR=false
for dir in /usr/share/applications /usr/local/share/applications; do
  if [[ -d "$dir" && -w "$dir" ]]; then
    HAS_MENU_DIR=true
    break
  fi
done

if $HAS_MENU_DIR; then
  JPACKAGE_ARGS+=(--linux-shortcut --linux-menu-group "Utility")
else
  echo "[WARN] No writable system menu directory found; skip --linux-shortcut."
fi

jpackage "${JPACKAGE_ARGS[@]}"

echo "DEB generated in $DEST"
