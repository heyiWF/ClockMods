# ClockMods Desktop application

Tauri 2 / TypeScript desktop application. See the [repository README](../README.md) and [porting and validation guide](../docs/desktop-port.md).

```sh
npm ci
npm test
npm run build
cargo test --locked --manifest-path src-tauri/Cargo.toml
npm run desktop:dev
npm run desktop:build
```

The production UI is embedded in the executable. Use `npm run dev` for browser frontend checks and `npm run desktop:dev` for native reminders, windows and widgets.

On Linux architectures without a Node/Tauri CLI binary, build `dist/` on a supported build host, copy it to the same source revision on the target, then run `bash scripts/build-native.sh`. Native runtime dependencies are required on the destination machine.

For the prepared rootless cloud image, run `bash scripts/cloud-appimage.sh` after building the release executable. It supplies the GTK modules and WebKit helpers from the workspace sysroot. Run `bash scripts/smoke-linux.sh /absolute/path/to/ClockMods.AppImage` with `APPIMAGE_EXTRACT_AND_RUN=1` to check the packaged UI in disposable user data.
