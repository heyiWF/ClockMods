# Desktop port: sources, behavior and validation

## Source baseline

- `desktop` branches from `ultimate-compose` commit `0d60715acee4b334685125ba0a3e67364caf085e`.
- `desktop/` imports the existing TypeScript renderer and tests from `web` commit `616d3c015921c4db833a0a8bf1541b17be33a3f9`.
- Android source remains available as a reference. The web branch already translates the clock/calendar designs and core time logic; it is not an Android binary running inside a WebView. Pixel-for-pixel parity is not claimed.
- Tauri uses the installed system WebView instead of bundling Chromium. Package size must be measured on release builds; installed system WebKit/WebView2 dependencies still consume disk space.

## Feature mapping

| Ultimate Compose feature | Desktop implementation |
| --- | --- |
| 12 clock styles, per-style typography, palettes, transitions | `src/pages/clock.ts`, `src/ui/ultimate-face.ts`, clock/theme settings |
| 5 calendar styles, lunar/almanac, holidays, month/week navigation | `src/pages/calendar.ts`, `src/lunar/`, `src/core/calendar-themes.ts` |
| Six world clocks, ordering and timezone selection | Existing world-clock settings and timezone catalog |
| Weather, forecast, cached data and custom message carousel | Existing weather UI; bounded HTTPS requests use Rust to avoid browser CORS constraints |
| Pomodoro, countdown, daily alarm | UI state plus persisted Rust scheduler; native notifications and WebView audio/overlays |
| Stopwatch and laps | Existing monotonic stopwatch implementation |
| Hourly/half-hour chime and quiet periods | Existing visual chime controller while UI runs |
| Android RemoteViews widgets | Four kinds of independent, draggable native windows; six themes; per-widget timezone, clock format, seconds, opacity and scale |
| Background image, blur, dim schedule, burn-in pixel shift | Existing renderer/settings, responsive to native window size |
| Keep screen on, full screen, orientation | Native display inhibitor, F11/Escape, responsive portrait/landscape window sizes |
| Android launcher / auto-start settings | Opt-in OS login launch, tray show/quit menu, single-instance activation |
| Simplified/Traditional Chinese and English | Existing translated UI; desktop-specific controls |
| Android dynamic color, vibration, cellular signal | Desktop palette and available system capabilities; no fabricated vibration or cellular hardware |

Android lock-screen permissions, exact alarms after process termination, and home-screen widget APIs do not exist as one portable desktop API. A desktop process cannot promise reminders while the computer is off or the app has exited. Native reminders catch up once after sleep/restart and do not replay a daily backlog. Notifications can be muted by the OS. Auto-location depends on the WebView/OS location provider; manual city selection is always available. Network clock calibration currently uses a validated HTTPS Date header, not Android's UDP SNTP, so sub-second NTP accuracy is not claimed.

## Platforms

The intended OS baseline is Windows 10/11 with WebView2, macOS 13+, and Linux with WebKitGTK 4.1 (2.40+). The provided Linux package requires glibc 2.39 or newer; build from source on an older supported distribution rather than installing an incompatible binary. Older systems require separate compatibility work.

Hosted CI (`build-desktop.yml`) defines Windows x86/x86_64/ARM64, macOS x86_64/ARM64 and Linux x86_64/ARM64 builds. Every push to `desktop` triggers the workflow, including documentation-only changes. Linux jobs produce AppImage, DEB and RPM; other jobs produce NSIS/DMG. Jobs upload unsigned development artifacts; no release is published. Windows code signing and Apple signing/notarization require separately configured certificates before public distribution. Transparent macOS windows use Tauri's private-API feature; these builds target direct distribution, not the Mac App Store.

The opt-in `build-desktop-native.yml` requires trusted self-hosted Linux runners labelled `i686`, `armv7`, `riscv64`, or `loongarch64`. Set repository Actions variable `CLOCKMODS_NATIVE_ARCHES` to a JSON array of the installed runner labels, for example `["riscv64","loongarch64","armv7","i686"]`, to build them on every push to `desktop`. Without this variable those jobs are skipped. Manual dispatch can select one architecture on `desktop` (GitHub requires the workflow file on the default branch for the dispatch UI). It builds frontend assets on x86_64 then tests and compiles Rust on the destination CPU. This avoids a dependency on Node binaries for LoongArch/32-bit Linux. Install the same pinned Rust toolchain (or a compatible verified distribution toolchain), a C compiler, pkg-config, WebKitGTK 4.1 and GTK 3 development packages. Runner labels alone do not prove the correct CPU; check `rustc -vV`, `uname -m` and artifact architecture.

Expected Rust targets:

```
i686-pc-windows-msvc
x86_64-pc-windows-msvc
aarch64-pc-windows-msvc
x86_64-apple-darwin
aarch64-apple-darwin
i686-unknown-linux-gnu
armv7-unknown-linux-gnueabihf
x86_64-unknown-linux-gnu
aarch64-unknown-linux-gnu
riscv64gc-unknown-linux-gnu
loongarch64-unknown-linux-gnu
```

Support for RISC-V, LoongArch and 32-bit Linux is conditional on the target distribution's WebKitGTK packages and the entire native dependency chain. Do not label those binaries supported until the native build and smoke tests pass. ARMv7 means hard-float ARMv7, not every historic ARM CPU; old MIPS Loongson is a different architecture. No universal binary combines different operating systems.

## Native data and security boundaries

- The native scheduler persists deadlines and pending alerts before emitting notifications. Acknowledged events are removed, preventing repeated notifications across navigation or restart.
- Corrupt reminder/widget state fails explicitly and preserves the file rather than silently deleting it.
- Tauri capabilities apply only to bundled local `main` and `widget-*` windows. No shell execution, unrestricted filesystem plugin or remote-page capabilities are granted.
- Weather HTTP supports GET/HEAD, requires HTTPS, rejects credentials in URLs and redirects, bounds responses to 1 MiB, and limits bearer tokens to QWeather hostnames. User-configured HTTPS proxy URLs receive no QWeather token.
- QWeather credentials entered in the frontend remain in local application storage (not an OS keychain). Use a personal HTTPS weather proxy if local credential storage is unsuitable. No secret is included in CI artifacts or source.
- Production bundles contain the UI; there is no service worker or listening localhost server.

## Validation

Run `npm test`, `npm run build`, and `cargo test --locked --manifest-path src-tauri/Cargo.toml`. Frontend tests cover imported behavior plus native bridge/widget integration; Rust tests cover durable reminder state, cancellation, catch-up and URL restrictions.

For a real native WebView smoke test, build the executable with the production assets and run it in a disposable user-data directory with `CLOCKMODS_SMOKE=1`. The UI traverses all clock/calendar themes and tools, opens/removes a widget, hides the main window and waits for a real native timer to restore it. It prints `CLOCKMODS_SMOKE_RESULT=...` and exits nonzero on failure. Set an external timeout so a renderer boot failure cannot hang CI. Always use disposable user data: this test intentionally changes themes and creates a timer.

On Linux, use a working graphical session or Xvfb and a session D-Bus. On each target platform also manually test notifications with OS permissions, sound, login start/stop, tray restore/quit, sleep/wake, high DPI, transparency, multiple monitors, timezone/DST changes, file selection, fonts, weather and geolocation. Passing unit tests does not prove these OS integrations.

Current evidence and remaining limitations must be recorded after running the commands; the existence of a workflow is not a successful CI run.

### Cloud validation on 2026-10-15

- Debian Linux x86_64: 335 frontend tests in 36 files, 7 Rust tests, TypeScript/Vite production build and native release build passed.
- Both the release executable and final AppImage passed all 23 native WebView smoke checks, including a real widget window and a timer restoring the hidden main window. AppImage was launched from `/tmp`, without workspace library variables or WebKit bind mounts, using disposable user data, Xvfb and session D-Bus.
- The cloud AppImage is 160,319,992 bytes (152.89 MiB), including GTK/WebKit and GStreamer modules; DEB is 7,922,736 bytes (7.56 MiB), relying on distribution runtime packages. Those sizes are not directly comparable to an app with all runtime dependencies already installed.
- Both desktop workflows pass Actionlint 1.7.12. This is local workflow validation; hosted build results are not yet verified. GitHub API access is blocked by this cloud environment's current network policy.
- Real audio output, OS notifications, login launch, suspend/resume, Wayland, Windows, macOS and the other CPU architectures still require platform testing. Headless tests do not establish those capabilities.
