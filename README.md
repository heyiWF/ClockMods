<div align="center">

<img src="docs/media/app-icon-playstore.png" alt="ClockMods" width="120" />

# ClockMods

**一款高度可定制的 Android 全屏时钟**

优雅地让你的旧设备继续发光发热。（但你还得管它充电不是）

兼容版 · 现代版 · 专业版　|　Android 4.0+　|　离线优先 · 无广告 · 无账号

</div>

---

ClockMods 以沉浸式全屏界面显示时间，并保持屏幕常亮。它同时呈现公历日期、星期与中国农历，可自由定制背景、字体、时间格式、时区与网络校时方式，并能显示实时天气。项目提供 **兼容版 / 现代版 / 专业版** 三个版本，各自拥有独立的应用 ID，可在同一台设备上并存。

## 功能演示

以下视频均由专业版在 Android 模拟器上横屏实机录制，可直接在 GitHub 中播放：

| 演示内容 | 视频 |
| --- | --- |
| 专业版功能概览：时钟、日历、番茄钟、闹钟、倒计时、秒表 | [观看](docs/media/pro-features-overview.mp4) |
| 中文 12 小时制整点报时（`下午1:00`） | [观看](docs/media/pro-chime-12h-zh.mp4) |
| English 12-hour hourly chime (`1:00 PM`) | [观看](docs/media/pro-chime-12h-en.mp4) |

整点报时沿用时钟的字体与粗体样式，按界面语言和 12/24 小时制显示时间，并在动画结束时平滑淡出。

## 主要功能

### 🕒 时钟与日期

- 实时显示时、分、秒，按秒边界刷新；可隐藏秒数或以较小字号显示。
- 支持 24 小时制与 12 小时制（12 小时制可显示「上午/下午」或 `AM/PM`）。
- 冒号每秒闪烁、数字变化渐变动效；专业版另提供滑动、缩放、翻转等多种过渡风格。
- 显示公历日期、星期与中国农历（离线天文算法精确覆盖 1–9999 年）。
- 可切换日期格式：兼容版/现代版提供一组固定格式，专业版可用表达式与自定义分隔符（含 emoji）自由组合中/英文日期与星期。
- 横屏优先单行显示，竖屏自动分行；竖屏还可切换为时/分/秒竖排大字。
- 中英文之间、中文与数字之间按书写规范自动加入半角空隙；温度与百分比等符号紧跟数字显示。

### 🎨 背景与外观

- 纯色背景，支持 HSV 连续取色与常用色快捷选择。
- 自定义背景图片：自动读取 EXIF 方向，按屏幕尺寸采样、居中裁剪并铺满。
- 图片背景可随时压暗，或设置定时压暗（支持跨越午夜）以减少对时间文字的干扰。
- 可分别调整时间与日期的字号和颜色，一键加粗，一键恢复默认。
- 多种字体可选（系统、Roboto、Google Sans；专业版内置更多字体）。
- **设置面板采用 Material 3 分组卡片**，各功能区清晰分区、间距统一。

### 🌐 时间与状态

- 默认使用设备系统时间，也可开启网络校时（SNTP/NTP）。
- 网络校时内置 **多台服务器自动故障转移**（`ntp.aliyun.com`、`ntp.tencent.com`、`ntp.ntsc.ac.cn`、`cn.pool.ntp.org`），可选每 30 分钟、1 小时、6 小时或每天同步；失败时自动回退设备时间。
- 可跟随系统时区，或从内置地区列表中选择其他时区。
- 可选显示网络与电池状态图标。
- 运行时保持屏幕常亮，采用沉浸式系统栏与边到边布局，横竖屏自动适配。
- 可开启「开机自启动」：将应用设为设备默认桌面，开机后由系统自动拉起时钟界面（三个版本均支持，设置中一键开关，无需额外系统权限）。

### 🌦️ 实时天气

- 由 [和风天气（QWeather）](https://www.qweather.com) 提供数据。
- 支持自动定位或手动指定省/市/区县；天气随界面语言显示为中文或英文。
- 更新频率可选 10 分钟、30 分钟、1 小时、3 小时、6 小时、12 小时（默认 30 分钟）。
- 可开启「详细天气」，轮播体感温度、相对湿度、风向风力与气象灾害预警等；单条过宽时横向滚动，不换行、不拆分、不缩小字号。
- 可设置自定义留言，非空时与天气在同一行轮播显示；无论留言长短，字号始终保持设定值，过宽时横向滚动。

### ⭐ 专业版扩展（仅 Pro）

专业版在全屏时钟之外提供一组可左右滑动切换的工具页面：

- **时钟**：全屏时钟主界面，支持整点报时（可设静音时段，支持跨越午夜）。
- **日历**：仪表盘式月历，可滑动或翻月、点选日期并一键回到今天；离线整合二十四节气、传统与公历节日、法定节假日「休/班」标记，并展示所选日期的「宜 / 忌」。横屏时左侧同时显示时间、实时天气与未来三日预报。
- **番茄钟**：基于计时器的番茄工作法。
- **闹钟**：全屏提醒 + 前台服务 + 通知 + 振动响铃；开机、应用更新、系统时间或时区变化后自动重新排程。
- **倒计时**：设置倒计时并在结束时提醒。
- **秒表**：计时与计次。

## 版本一览

| 版本 | 最低系统 | 应用 ID | 界面与能力 |
| --- | --- | --- | --- |
| 兼容版 `compat` / ClockMods Lite | Android 4.0（API 14） | `com.clockmods.compat` | 使用平台原生控件，面向旧设备；API 19+ 使用系统文档选择器；界面支持中/英文 |
| 现代版 `modern` / ClockMods | Android 6.0（API 23） | `com.clockmods.modern` | Material 3 设置面板；Android 12+ 支持动态取色；Android 13+ 使用 Photo Picker；界面支持中/英文 |
| 专业版 `pro` / ClockMods Pro | Android 12（API 31） | `com.clockmods.pro` | 在现代版基础上增加日历、闹钟、倒计时、番茄钟、秒表、整点报时等工具页面；界面支持简体/繁体/英文三语 |

## 使用方法

1. 打开应用即进入全屏时钟界面。
2. **双击**时钟区域打开设置。
3. 在「样式」页调整背景、字体、秒数、农历与状态图标。
4. 在「功能」页设置屏幕方向、开机自启动、时间制式、网络校时、时区、界面语言、日期格式、自定义留言与天气。
5. 点击「应用」保存设置。

## 权限与隐私

ClockMods 不含账号、广告、云同步或用户行为统计，所有数据仅保存在应用私有存储中。

- `INTERNET`：仅在启用网络时间或天气后访问 NTP / QWeather 服务器。
- `ACCESS_NETWORK_STATE`、`ACCESS_WIFI_STATE`：用于显示网络状态图标。
- `ACCESS_COARSE_LOCATION`、`ACCESS_FINE_LOCATION`：仅在开启天气且使用自动定位时获取当前地区，不进行后台定位。
- 背景图片只处理用户主动选择的单张图片，不申请读取整个相册的权限。
- 专业版额外申请 `POST_NOTIFICATIONS`、`SCHEDULE_EXACT_ALARM`、`USE_FULL_SCREEN_INTENT`、`RECEIVE_BOOT_COMPLETED`、`VIBRATE`、`FOREGROUND_SERVICE`、`FOREGROUND_SERVICE_MEDIA_PLAYBACK`：用于闹钟精确排程、到点全屏提醒、响铃前台服务、振动，以及设备重启或时间变化后重新排程。

## QWeather 配置

复制 `qweather.properties.example` 为 `qweather.properties`，填写 API Host、凭据 ID、项目 ID 及 PKCS#8 Ed25519 私钥的 Base64 内容。天气图标来自 [QWeather Icons](https://icons.qweather.com)（CC BY 4.0），已随项目打包 fill 与 line 两种风格。

## 日历数据

主时钟农历与专业版日历的农历、二十四节气、传统与公历节日、数九/三伏以及每日「宜/忌」，均由离线农历引擎 [tyme4j](https://github.com/6tail/tyme4j)（`cn.6tail:tyme4j`，MIT 许可）实时计算，天文算法可准确覆盖 1–9999 年，宜忌依据《钦定协纪辨方书》神煞规则推算。

法定节假日「休/班」安排每年由国务院公布、无法算法推导，故使用随应用打包的离线数据 [holiday-cn](https://github.com/NateScarlet/holiday-cn)（MIT 许可，见 `app/src/pro/assets/holidays`）。应用运行时不会联网抓取任何日历数据。

## 构建

环境要求：Android Studio 或 JDK 17+、Android SDK Platform 36.1、Build Tools 36.1.0、Gradle Wrapper 9.4.1。

```powershell
# 运行单元测试
.\gradlew.bat testCompatDebugUnitTest testModernDebugUnitTest testProDebugUnitTest

# 构建三个版本的 Debug APK
.\gradlew.bat assembleCompatDebug assembleModernDebug assembleProDebug

# Lint 检查
.\gradlew.bat lintCompatDebug lintModernDebug lintProDebug
```

构建产物位于 `app/build/outputs/apk/<flavor>/debug/`。

## 项目结构

```text
app/src/main/    三个版本共享的时钟、农历、时间、天气、背景与资源代码
app/src/compat/  兼容版设置界面与平台适配（无 Material 依赖）
app/src/modern/  现代版 Material 3 设置界面与平台适配
app/src/pro/     专业版工具页面（日历、闹钟、倒计时、番茄钟、秒表、整点报时），复用现代版界面与资源
app/src/test/    共享逻辑单元测试
```

## 致谢

- 天气数据与图标：[QWeather 和风天气](https://www.qweather.com) · [QWeather Icons](https://icons.qweather.com)
- 农历与宜忌引擎：[tyme4j](https://github.com/6tail/tyme4j)
- 法定节假日数据：[holiday-cn](https://github.com/NateScarlet/holiday-cn)
