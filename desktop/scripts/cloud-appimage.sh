#!/usr/bin/env bash
# Bundle an already-built release in the rootless Codex Debian x86_64 image.
# Normal Linux installations and hosted CI use `npm run desktop:build` directly.
set -euo pipefail
cd "$(dirname "$0")/.."
source /workspace/.clockmods-desktop/activate.sh
export XDG_CACHE_HOME=/workspace/.clockmods-desktop/cache
export APPIMAGE_EXTRACT_AND_RUN=1
export LD_GTK_LIBRARY_PATH=/workspace/.clockmods-desktop/sysroot/usr/lib/x86_64-linux-gnu
python3 - <<'PY'
from pathlib import Path
import json, shutil
root = Path('/workspace/.clockmods-desktop')
base = root / 'sysroot/usr/lib/x86_64-linux-gnu'
# Rootless APT downloads only missing packages. Merge the runtime modules
# already supplied by the base image into the packaging sysroot.
for name in ['gio', 'gtk-3.0', 'gdk-pixbuf-2.0', 'girepository-1.0']:
    source = Path('/usr/lib/x86_64-linux-gnu') / name
    if source.exists():
        shutil.copytree(source, base / name, dirs_exist_ok=True, symlinks=False)
shutil.copytree('/usr/share/glib-2.0/schemas', root / 'sysroot/usr/share/glib-2.0/schemas', dirs_exist_ok=True)
# Tauri's normal helper discovery searches /usr; explicitly include the
# rootless WebKit helpers, along with the downloaded packages' license files.
files = {
    'usr/lib/x86_64-linux-gnu/webkit2gtk-4.1': str(base / 'webkit2gtk-4.1'),
    'usr/share/doc/clockmods-bundled-packages': str(root / 'sysroot/usr/share/doc'),
}
(root / 'appimage-config.json').write_text(json.dumps({'bundle': {'linux': {'appimage': {'files': files}}}}))
PY
npm run tauri -- bundle --bundles appimage --ci --config /workspace/.clockmods-desktop/appimage-config.json "$@"
