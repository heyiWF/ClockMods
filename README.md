# ClockMods Desktop

基于 `ultimate-compose` 的 **Tauri 2 桌面移植**。复用仓库 `web` 分支的主题和时间工具，在 Rust 原生进程中实现托盘、提醒调度、独立小组件和窗口控制。

- 十二套时钟主题，五套日历主题，最多六个世界时钟。
- 离线农历、节气、节日与宜忌；天气、留言轮播、字体与图片背景。
- 番茄钟、闹钟、倒计时、秒表、整点与半点视觉报时。
- 四类独立桌面小组件、六套外观、独立时区与显示设置。
- 窗口置顶、全屏、保持常亮、登录启动；关闭主窗口后保留托盘提醒。
- 简体中文、繁體中文、English。

## 开发

安装 Node.js 24、Rust 1.99，以及 [Tauri 2 系统依赖](https://v2.tauri.app/start/prerequisites/)。Linux 需要 WebKitGTK **4.1** 和 GTK 3。

```bash
cd desktop
npm ci
npm test
npm run build
cargo test --locked --manifest-path src-tauri/Cargo.toml
npm run desktop:dev
```

打包：`npm run desktop:build`；仅打包 Linux AppImage 可使用 `npm run desktop:build -- --bundles appimage`。输出位于 `desktop/src-tauri/target/release/bundle/`。不需要 Android SDK。

## 平台与架构

| 系统 | 目标架构 | 构建方式 |
| --- | --- | --- |
| Windows | x86、x86_64、ARM64 | GitHub Actions，NSIS 安装包 |
| macOS | Intel x86_64、Apple Silicon ARM64 | 对应 macOS runner，DMG |
| Linux | x86_64、ARM64 | 对应 Linux runner，AppImage/DEB/RPM |
| Linux | x86、ARMv7、RISC-V 64、LoongArch64 | 目标架构原生 runner，压缩包 |

配置了构建流程不等于完成了平台认证。**目前只有 Linux x86_64 能在本云环境运行验证**；其余目标必须在对应的操作系统、CPU 和发行版执行 CI 与桌面验收。额外架构需要发行版提供 WebKitGTK 4.1、GTK 3 和 Rust 工具链，详见 [桌面移植与验证说明](docs/desktop-port.md)。

每次 push 到 `desktop` 都触发 [桌面 CI](.github/workflows/build-desktop.yml)，产物在 Actions 的 Artifacts 中下载。额外 Linux 架构配置好自托管 runner 后，设置仓库变量 `CLOCKMODS_NATIVE_ARCHES`（JSON 数组）即可随每次 push 构建，详见移植说明。

AppImage 需要赋予执行权限后运行；没有 FUSE 的环境可使用 `APPIMAGE_EXTRACT_AND_RUN=1 ./ClockMods_*.AppImage`。当前 Linux 构建基线为 glibc 2.39，AppImage 不意味着兼容所有旧发行版。

macOS 不支持 32 位；Windows ARM32 不在本项目目标中。“国产 CPU”按指令集划分：国产 x86/ARM CPU 使用对应版本，龙芯使用 LoongArch64，旧 MIPS 龙芯不等同于 LoongArch。

## 提醒和数据

提醒由原生进程调度；切换页面、最小化和隐藏到托盘不会取消提醒。**退出应用、注销、关机时不能提醒，也不会唤醒关机的电脑**。恢复运行后补发到期提醒。登录启动默认关闭，可在“桌面”设置中开启。

用户设置、背景图和天气缓存保存在本地 WebView 数据目录；提醒和小组件配置保存在系统应用数据目录。天气需在设置中提供自己的 QWeather 配置或 HTTPS 代理；不开启天气也可使用离线功能。签名凭据不会写入可执行文件。

桌面小组件是可拖动的独立窗口，不是 Windows Widgets、macOS WidgetKit 或 Android RemoteViews 插件。透明度、托盘、定位、通知、常亮等能力取决于桌面环境；Wayland 特别需要目标环境验收。

Android 原始工程仍保留在 `app/`，开发资料见 [Android 开发说明](docs/product-development-guide.md)。本分支的桌面入口位于 `desktop/`。

MIT；继承资源的字体、QWeather 图标、tyme4ts、holiday-cn、CLDR 和 OpenCC 许可见 `desktop/public/licenses/` 与 `desktop/src/assets/fonts/`。
