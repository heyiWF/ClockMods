#!/usr/bin/env bash
# Rootless setup for the Debian x86_64 Codex cloud image. Other architectures
# should install their distribution's development packages and use build-native.sh.
set -euo pipefail
cd /workspace/ClockMods/desktop
setup_root=/workspace/.clockmods-desktop
mkdir -p "$setup_root" "$setup_root/apt/"{lists/partial,archives/partial,empty} "$setup_root/sysroot"
cat > "$setup_root/apt/sources.list" <<'SOURCES'
deb [signed-by=/usr/share/keyrings/debian-archive-keyring.gpg] https://deb.debian.org/debian trixie main
SOURCES
cat > "$setup_root/apt/apt.conf" <<'APT'
Dir::Etc::sourcelist "/workspace/.clockmods-desktop/apt/sources.list";
Dir::Etc::sourceparts "/workspace/.clockmods-desktop/apt/empty";
Dir::Etc::parts "/workspace/.clockmods-desktop/apt/empty";
Dir::State::lists "/workspace/.clockmods-desktop/apt/lists";
Dir::Cache::archives "/workspace/.clockmods-desktop/apt/archives";
Dir::Cache::pkgcache "/workspace/.clockmods-desktop/apt/pkgcache.bin";
Dir::Cache::srcpkgcache "/workspace/.clockmods-desktop/apt/srcpkgcache.bin";
Debug::NoLocking "true";
APT::Sandbox::User "agent";
APT
export APT_CONFIG="$setup_root/apt/apt.conf"
apt-get update
apt-get --download-only --no-install-recommends -y install libwebkit2gtk-4.1-dev libappindicator3-dev librsvg2-dev patchelf xvfb xauth dbus-x11
for package in "$setup_root/apt/archives/"*.deb; do dpkg-deb -x "$package" "$setup_root/sysroot"; done
python3 - <<'PY'
from pathlib import Path
root=Path('/workspace/.clockmods-desktop/sysroot')
for p in root.rglob('*.pc'):
 s=p.read_text().replace('prefix=/usr',f'prefix={root}/usr').replace('libdir=/usr',f'libdir={root}/usr').replace('includedir=/usr',f'includedir={root}/usr');p.write_text(s)
# Development symlinks can refer to runtime files already supplied by the image.
for p in list(root.rglob('*')):
 if not p.is_symlink() or p.exists():continue
 missing=p.resolve()
 try:system=Path('/')/missing.relative_to(root)
 except ValueError:continue
 if system.exists():
  missing.parent.mkdir(parents=True,exist_ok=True)
  if not missing.is_symlink():missing.symlink_to(system)
# WebKit helpers use a fixed /usr/lib path. A private mount namespace maps this
# directory without touching system files or disabling WebKit's sandbox.
out=Path('/workspace/.clockmods-desktop/runtime-libs');out.mkdir(exist_ok=True)
for p in Path('/usr/lib/x86_64-linux-gnu').iterdir():
 target=out/p.name
 if not target.exists() and not target.is_symlink():target.symlink_to(Path('/run/clockmods-host-libs')/p.name)
p=out/'webkit2gtk-4.1'
if not p.is_symlink():p.symlink_to(root/'usr/lib/x86_64-linux-gnu/webkit2gtk-4.1')
PY
export CARGO_HOME="$setup_root/cargo" RUSTUP_HOME="$setup_root/rustup"
if [ ! -x "$CARGO_HOME/bin/rustup" ]; then
 curl -fsSL --retry 3 https://static.rust-lang.org/rustup/dist/x86_64-unknown-linux-gnu/rustup-init -o "$setup_root/rustup-init"
 curl -fsSL --retry 3 https://static.rust-lang.org/rustup/dist/x86_64-unknown-linux-gnu/rustup-init.sha256 -o "$setup_root/rustup-init.sha256"
 (cd "$setup_root" && sha256sum -c rustup-init.sha256)
 chmod +x "$setup_root/rustup-init"
 "$setup_root/rustup-init" -y --profile minimal --default-toolchain 1.99.0 --no-modify-path
fi
cat > "$setup_root/activate.sh" <<'ACTIVATE'
export CARGO_HOME=/workspace/.clockmods-desktop/cargo
export RUSTUP_HOME=/workspace/.clockmods-desktop/rustup
export PATH="$CARGO_HOME/bin:/workspace/.clockmods-desktop/sysroot/usr/bin:$PATH"
export npm_config_cache=/workspace/.clockmods-setup/npm-cache
export PKG_CONFIG_PATH=/workspace/.clockmods-desktop/sysroot/usr/lib/x86_64-linux-gnu/pkgconfig:/workspace/.clockmods-desktop/sysroot/usr/share/pkgconfig
export LD_LIBRARY_PATH=/workspace/.clockmods-desktop/sysroot/usr/lib/x86_64-linux-gnu${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}
export LIBRARY_PATH=/workspace/.clockmods-desktop/sysroot/usr/lib/x86_64-linux-gnu${LIBRARY_PATH:+:$LIBRARY_PATH}
export XDG_DATA_DIRS=/workspace/.clockmods-desktop/sysroot/usr/share:/usr/local/share:/usr/share
export CARGO_BUILD_JOBS=4
ACTIVATE
source "$setup_root/activate.sh"
rustup toolchain install 1.99.0 --profile minimal --component rustfmt --component clippy
cat > "$setup_root/run-gui.sh" <<'GUI'
#!/usr/bin/env bash
set -euo pipefail
source /workspace/.clockmods-desktop/activate.sh
export DISPLAY="${DISPLAY:-:99}"
export XDG_RUNTIME_DIR="${XDG_RUNTIME_DIR:-/workspace/.clockmods-desktop/runtime}"
export XDG_CONFIG_HOME="${XDG_CONFIG_HOME:-/workspace/.clockmods-desktop/user-config}"
export XDG_DATA_HOME="${XDG_DATA_HOME:-/workspace/.clockmods-desktop/user-data}"
export XDG_CACHE_HOME="${XDG_CACHE_HOME:-/workspace/.clockmods-desktop/user-cache}"
mkdir -p "$XDG_RUNTIME_DIR" "$XDG_CONFIG_HOME" "$XDG_DATA_HOME" "$XDG_CACHE_HOME"
chmod 700 "$XDG_RUNTIME_DIR"
export WEBKIT_DISABLE_DMABUF_RENDERER=1 LIBGL_ALWAYS_SOFTWARE=1
exec bwrap --ro-bind / / --tmpfs /run --ro-bind /usr/lib/x86_64-linux-gnu /run/clockmods-host-libs --ro-bind /workspace/.clockmods-desktop/runtime-libs /usr/lib/x86_64-linux-gnu --bind /workspace /workspace --bind /tmp /tmp --dev-bind /dev /dev -- dbus-run-session -- "$@"
GUI
chmod +x "$setup_root/run-gui.sh"
node --version
pkg-config --modversion webkit2gtk-4.1
npm ci --no-audit --no-fund
npm run build
npm test
cargo test --locked --manifest-path src-tauri/Cargo.toml
cargo build --locked --features custom-protocol --manifest-path src-tauri/Cargo.toml
