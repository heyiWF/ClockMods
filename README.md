<div align="center">

<img src="docs/media/app-icon-playstore.png" alt="ClockMods" width="120" />

# ClockMods Ultimate

**一款高度可定制的 Android 全屏时钟**

优雅地让你的旧设备继续发光发热。（但你还得管它充电不是）

模拟时钟 · 数字时钟 · 实用工具　|　Android 12+　|　离线优先 · 无广告 · 无账号

</div>

---

ClockMods Ultimate 以沉浸式全屏界面显示时间，并保持屏幕常亮。它同时呈现公历日期、星期与中国农历，可自由切换完整时钟主题，并定制背景、字体、时间格式、时区、网络校时与实时天气。

## 主要功能

### 🕒 时钟与日期

- 实时显示时、分、秒，按秒边界刷新；可隐藏秒数或以较小字号显示。
- 支持 24 小时制与 12 小时制（12 小时制可显示「上午/下午」或 `AM/PM`）。
- 冒号每秒闪烁，Pro Classic 支持渐变、滑动、缩放、翻转等多种数字过渡风格。
- 显示公历日期、星期与中国农历。
- 可用表达式与自定义分隔符自由组合中/英文日期与星期。
- 横屏优先单行显示，竖屏自动分行；竖屏还可切换为时/分/秒竖排大字。

### 🎨 背景与外观

- 纯色背景或自定义背景图片。
- 图片背景可随时压暗，或设置定时压暗以减少对时间文字的干扰。
- 可分别调整时间与日期的字号和颜色。
- 内置多种字体。

### 🌐 时间与状态

- 默认使用设备系统时间，也可开启网络校时（SNTP/NTP）。
- 网络校时，可选每 30 分钟、1 小时、6 小时或每天同步。
- 可跟随系统时区，或从内置地区列表中选择其他时区。
- 可选显示网络与电池状态图标。
- 运行时保持屏幕常亮，采用沉浸式系统栏与边到边布局，横竖屏自动适配。
- 可开机自启动。

### 🌦️ 实时天气

- 由 [和风天气（QWeather）](https://www.qweather.com) 提供数据。
- 支持自动定位或手动指定省/市/区县；天气随界面语言显示为中文或英文。
- 更新频率可选 10 分钟、30 分钟、1 小时、3 小时、6 小时、12 小时（默认 30 分钟）。
- 可开启「详细天气」，轮播体感温度、相对湿度、风向风力与气象灾害预警等。
- 可设置自定义留言。

### ⭐ 工具与提醒

Ultimate 在全屏时钟之外提供一组工具：

- **时钟**：全屏时钟主界面，支持整点报时。
- **日历**：仪表盘式月历；二十四节气、传统与公历节日、法定节假日、「宜 / 忌」。
- **番茄钟**：基于计时器的番茄工作法。
- **闹钟**：全屏提醒 + 通知 + 振动响铃。
- **倒计时**：设置倒计时并在结束时提醒。
- **秒表**：计时与计次。

### 🧩 桌面小组件

- 数字时钟、模拟时钟、时钟天气、日期日历四个独立入口，长按桌面即可添加。
- 六套小组件主题：系统动态色、玻璃、深色仪表、纸张、霓虹、透明；独立于全屏时钟样式。
- 每个实例独立设置时区、系统或固定时间制、显示模块、背景透明度、文字比例和点击行为。
- 点击卡片右上角的设置按钮可重新配置；取消不会覆盖原配置。
- 实时时间由系统 TextClock / AnalogClock 驱动；日期按实例时区跨日刷新。
- 天气每 30 分钟尝试更新，点击天气区域可刷新；离线保留缓存与更新时间，无配置时引导进入天气设置。
- 日期日历第一版采用大日期卡，显示农历、节气和节假日，不含完整月历网格。

开发与扩展约定见 [Widget contract](docs/WIDGET_CONTRACT.md)。

### ✨ Ultimate 主题

- 内置 Pro Classic、Glass Atelier、Noir Instrument、Paper Station、Orbit Neon、Digital Grid、Typographic、双块、轨道、气泡、混合、丝带共十二套样式，涵盖原版 Pro、模拟、数字与混合时钟。
- 秒针支持平滑扫秒、跳秒和关闭。

## 应用信息

| 版本 | 最低系统 | 应用 ID | 界面与能力 |
| --- | --- | --- | --- |
| Ultimate `ultimate` / ClockMods Ultimate | Android 12（API 31） | `com.clockmods.ultimate` | 完整继承 Pro 工具页与相关能力，提供十二套完整时钟样式、模拟/数字样式切换、二级设置导航与可扩展 Clock Style SDK |

## 使用方法

1. 打开应用即进入全屏时钟界面。
2. **双击**时钟区域打开设置。
3. 从分类首页进入对应二级页面；Pro Classic 的字体、颜色和数字动效等选项已归入「时钟样式」，其他功能按其所属分类设置。

## 权限与隐私

ClockMods 不含账号、广告、云同步或用户行为统计，所有数据仅保存在应用私有存储中。

- `INTERNET`：仅在启用网络时间或天气后访问 NTP / QWeather 服务器。
- `ACCESS_NETWORK_STATE`、`ACCESS_WIFI_STATE`：用于显示网络状态图标。
- `ACCESS_COARSE_LOCATION`、`ACCESS_FINE_LOCATION`：仅在开启天气且使用自动定位时获取当前地区，不进行后台定位。
- 背景图片只处理用户主动选择的单张图片，不申请读取整个相册的权限。
- `POST_NOTIFICATIONS`、`SCHEDULE_EXACT_ALARM`、`USE_FULL_SCREEN_INTENT`、`RECEIVE_BOOT_COMPLETED`、`VIBRATE`、`FOREGROUND_SERVICE`、`FOREGROUND_SERVICE_MEDIA_PLAYBACK`：用于闹钟精确排程、到点全屏提醒、响铃前台服务、振动，以及设备重启或时间变化后重新排程。

## QWeather 配置

复制 `qweather.properties.example` 为 `qweather.properties`，填写 API Host、凭据 ID、项目 ID 及 PKCS#8 Ed25519 私钥的 Base64 内容。天气图标来自 [QWeather Icons](https://icons.qweather.com)（CC BY 4.0），已随项目打包 fill 与 line 两种风格。

## 日历数据

主时钟农历与专业版日历的农历、二十四节气、传统与公历节日、数九/三伏以及每日「宜/忌」，均由离线农历引擎 [tyme4j](https://github.com/6tail/tyme4j)（`cn.6tail:tyme4j`，MIT 许可）实时计算，天文算法可准确覆盖 1–9999 年，宜忌依据《钦定协纪辨方书》神煞规则推算。

法定节假日「休/班」安排每年由国务院公布、无法算法推导，故使用随应用打包的离线数据 [holiday-cn](https://github.com/NateScarlet/holiday-cn)（MIT 许可，见 `app/src/ultimate/assets/holidays`）。应用运行时不会联网抓取任何日历数据。

## 构建

环境要求：Android Studio 或 JDK 17+、Android SDK Platform 37、Build Tools 37.0.0、Gradle Wrapper 9.4.1。本项目使用 Kotlin DSL、Jetpack Compose 与 Material 3；当前 `compileSdk` / `targetSdk` 均为 37，最低支持 Android 12（API 31）。

```powershell
# 运行单元测试
.\gradlew.bat testUltimateDebugUnitTest

# 构建 Debug APK
.\gradlew.bat assembleUltimateDebug

# Lint 检查
.\gradlew.bat lintUltimateDebug
```

构建产物位于 `app/build/outputs/apk/ultimate/debug/`。

## 项目结构

```text
app/src/main/    Kotlin 核心层、Compose Material 3 主题、农历、时间、天气、背景与共享资源
app/src/ultimate/ Compose 宿主、十二套 Canvas 时钟样式、设置、工具页面、提醒组件、RemoteViews 小组件与离线资产
app/src/main/java/com/clockmods/sdk/clock/ 公开的时钟样式 SDK 契约
app/src/test/    核心逻辑单元测试
app/src/testUltimate/ Ultimate 主题、SDK 与工具功能测试
```

## 致谢

- 天气数据与图标：[QWeather 和风天气](https://www.qweather.com) · [QWeather Icons](https://icons.qweather.com)
- 农历与宜忌引擎：[tyme4j](https://github.com/6tail/tyme4j)
- 法定节假日数据：[holiday-cn](https://github.com/NateScarlet/holiday-cn)
