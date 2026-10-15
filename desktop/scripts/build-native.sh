#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
test -f dist/index.html || { echo 'Build desktop/dist with npm ci && npm run build on a supported Node host, then copy dist here.' >&2; exit 1; }
# Compile on the destination architecture. This avoids assuming a prebuilt
# Tauri Node CLI or Node.js binary exists for LoongArch or 32-bit Linux.
pkg-config --atleast-version=2.40 webkit2gtk-4.1
pkg-config --exists gtk+-3.0
native_target=$(rustc -vV | sed -n 's/^host: //p')
case "${CLOCKMODS_EXPECT_ARCH:-}" in
  riscv64) expected=riscv64gc-unknown-linux-gnu ;;
  loongarch64) expected=loongarch64-unknown-linux-gnu ;;
  armv7) expected=armv7-unknown-linux-gnueabihf ;;
  i686) expected=i686-unknown-linux-gnu ;;
  "") expected="$native_target" ;;
  *) echo 'Unknown requested architecture' >&2; exit 1 ;;
esac
[ "$native_target" = "$expected" ] || { echo "Runner Rust host $native_target does not match $expected" >&2; exit 1; }
cargo test --locked --manifest-path src-tauri/Cargo.toml
cargo build --release --locked --features custom-protocol --manifest-path src-tauri/Cargo.toml
mkdir -p artifacts
machine="$native_target"
tar -czf "artifacts/clockmods-linux-${machine}.tar.gz" -C src-tauri/target/release clockmods-desktop
sha256sum "artifacts/clockmods-linux-${machine}.tar.gz" > "artifacts/clockmods-linux-${machine}.tar.gz.sha256"
