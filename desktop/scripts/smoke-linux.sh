#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
# Use only a disposable directory; the native smoke intentionally edits settings.
smoke_root=$(mktemp -d "${TMPDIR:-/tmp}/clockmods-smoke.XXXXXX")
export XDG_CONFIG_HOME="$smoke_root/config" XDG_DATA_HOME="$smoke_root/data" XDG_CACHE_HOME="$smoke_root/cache" XDG_RUNTIME_DIR="$smoke_root/runtime"
mkdir -p "$XDG_RUNTIME_DIR" "$XDG_CONFIG_HOME" "$XDG_DATA_HOME" "$XDG_CACHE_HOME"
chmod 700 "$XDG_RUNTIME_DIR"
export CLOCKMODS_SMOKE=1
binary="${1:-$PWD/src-tauri/target/debug/clockmods-desktop}"
if [ -x /workspace/.clockmods-desktop/run-gui.sh ]; then
 timeout 90s /workspace/.clockmods-desktop/run-gui.sh "$binary"
else
 timeout 90s xvfb-run -a dbus-run-session -- "$binary"
fi
