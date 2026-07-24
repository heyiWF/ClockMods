# ClockMods

ClockMods 是一款可高度自定义的 Android 全屏时钟，适合放在桌面、床头、旧手机或平板上持续显示时间。应用采用沉浸式界面并保持屏幕常亮，可显示公历、星期和农历，也可以自定义背景、字体、时间格式、时区及网络校时方式。

项目提供兼容版、现代版和专业版三个版本，三者拥有各自独立的应用 ID，可以安装在同一台设备上。

## 功能演示

以下视频均由 Pro 版在 Android 模拟器上以横屏模式实机录制，可直接在 GitHub 中打开：

| 演示内容 | 视频 |
| --- | --- |
| Pro 主要功能概览：时钟、日历、番茄钟、闹钟、倒计时和秒表 | [观看 Pro 主要功能概览](docs/media/pro-features-overview.mp4) |
| 中文 12 小时制整点报时：`下午1:00` | [观看中文整点报时](docs/media/pro-chime-12h-zh.mp4) |
| English 12-hour hourly chime: `1:00 PM` | [Watch the English hourly chime](docs/media/pro-chime-12h-en.mp4) |

整点报时会跟随时钟设置使用相同的字体和粗体样式，并根据界面语言及 12/24 小时制显示时间；动画结束时会平滑淡出。

## 主要功能

### 时钟与日期

- 实时显示时、分、秒，并按秒边界刷新。
- 支持 24 小时制和 12 小时制；12 小时制可显示“上午/下午”或 `AM/PM`。
- 可隐藏秒数，或将秒数改为较小字号显示。
- 支持冒号每秒闪烁和数字变化渐变动效。
- 显示公历日期、星期和中国农历，农历数据范围为 1900–2050 年。
- 支持中文或英文时钟界面。
- 横屏时日期和农历优先单行显示，竖屏时自动分行，字号会根据屏幕尺寸调整。

### 背景与外观

- 使用纯色背景，支持 HSV 连续取色和常用色快捷选择。
- 从系统相册或文档选择器设置自定义背景图片。
- 自动读取图片 EXIF 方向，并按屏幕尺寸采样、居中裁剪和铺满显示。
- 可随时压暗图片背景，减少背景对时间文字的干扰。
- 支持定时压暗，可设置开始和结束时间，并支持跨越午夜。
- 可分别调整时间和日期的字号及颜色。
- 可启用粗体文字，并一键恢复默认样式。

### 时间与状态

- 默认使用设备系统时间，也可通过 `ntp.aliyun.com` 获取网络时间。
- 网络校时支持每 30 分钟、1 小时、6 小时或每天同步；同步失败时自动使用设备时间。
- 可跟随系统时区，也可从内置地区列表中选择其他时区。
- 可选择显示网络状态和电池电量图标。
- 应用运行时保持屏幕常亮，并使用沉浸式系统栏和边到边布局。
- 支持横竖屏切换，布局会自动适配屏幕方向。

### 实时天气

- 可在“功能”页开启实时天气，天气固定使用中文显示。
- 开启后通过设备定位查询当前城市、区县、天气和温度，并在时间下方显示对应的 QWeather fill 图标。
- 更新频率支持 10 分钟、30 分钟、1 小时、3 小时、6 小时和 12 小时，默认 30 分钟。
- 天气仅在应用前台定位和刷新；定位、网络或配置失败时会在天气行显示状态提示。

### 专业版扩展（仅 Pro 版）

专业版在全屏时钟之外提供一组滑动切换的工具页面：

- **时钟**：保留全屏时钟主界面，并支持整点报时（可开启静音时段，静音时段支持跨越午夜）。
- **日历**：按月查看公历日历，可前后翻月。
- **番茄钟**：基于计时器的番茄工作法计时。
- **闹钟**：设置闹钟并在到点时通过全屏提醒、前台服务、通知和振动响铃；开机、应用更新、系统时间或时区变化后会自动重新排程。
- **倒计时**：设置倒计时并在结束时提醒。
- **秒表**：计时与计次。

## 使用方法

1. 打开应用即可进入全屏时钟界面。
2. 双击时钟区域打开设置。
3. 在“样式”页调整背景、字体、秒数、农历和状态图标。
4. 在“功能”页设置时间制式、网络校时、地区时区、天气和界面语言。
5. 点击“应用”保存设置。

## 双版本说明

| 版本 | 最低系统 | 应用 ID | 界面与系统能力 |
| --- | --- | --- | --- |
| 兼容版 `compat` / ClockMods Lite | Android 4.0（API 14） | `com.clockmods.compat` | 使用 Android 平台控件，面向旧设备；API 19 以上使用系统文档选择器 |
| 现代版 `modern` / ClockMods | Android 6.0（API 23） | `com.clockmods.modern` | 使用 Material 3 设置面板；Android 12 以上支持动态取色；Android 13 以上使用 Photo Picker |
| 专业版 `pro` / ClockMods Pro | Android 10（API 29） | `com.clockmods.pro` | 在现代版界面基础上，增加闹钟、倒计时、番茄钟、秒表、日历和整点报时等工具页面 |

三个版本共享时钟、农历、网络校时、时区、设置存储和背景图片处理逻辑。专业版复用现代版的界面与资源，并叠加自身的工具页面和后台调度能力。Material Components 仅用于现代版和专业版，因此不会提高兼容版的最低 Android 版本。

## 权限与隐私

- `INTERNET`：仅在启用网络时间后用于访问 NTP 服务器。
- `ACCESS_NETWORK_STATE` 和 `ACCESS_WIFI_STATE`：用于显示网络状态图标。
- `ACCESS_COARSE_LOCATION` 和 `ACCESS_FINE_LOCATION`：仅在用户开启天气后，用于获取当前地区天气；不进行后台定位。
- 专业版额外申请 `POST_NOTIFICATIONS`、`SCHEDULE_EXACT_ALARM`、`USE_FULL_SCREEN_INTENT`、`RECEIVE_BOOT_COMPLETED`、`VIBRATE`、`FOREGROUND_SERVICE` 和 `FOREGROUND_SERVICE_MEDIA_PLAYBACK`：用于闹钟精确排程、到点全屏提醒、响铃前台服务、振动，以及在设备重启或时间变化后重新排程闹钟。
- 应用不申请读取整个相册的权限，只处理用户主动选择的单张图片。
- 背景图片副本和各项设置保存在应用自身的私有存储中。
- 项目不包含账号、广告、云同步或用户行为统计功能。

## QWeather 配置

复制 `qweather.properties.example` 为 `qweather.properties`，填写 API Host、凭据 ID、项目 ID 和 PKCS#8 Ed25519 私钥的 Base64 内容。

天气数据由 [和风天气](https://www.qweather.com) 提供。QWeather Icons 来自随项目导入的 QWeather Icons 1.8.0，图标采用 CC BY 4.0 许可。

## 当前不包含的功能

ClockMods 当前不提供后台天气更新、桌面小部件、云同步和图片编辑功能。闹钟、倒计时、番茄钟、秒表和整点报时仅在专业版中提供。

## 构建环境

- Android Studio 或 JDK 17 及以上版本
- Android SDK Platform 36.1
- Android SDK Build Tools 36.1.0
- Gradle Wrapper 9.4.1

在 Windows PowerShell 中运行单元测试：

```powershell
.\gradlew.bat testCompatDebugUnitTest testModernDebugUnitTest testProDebugUnitTest
```

构建三个版本的 Debug APK：

```powershell
.\gradlew.bat assembleCompatDebug assembleModernDebug assembleProDebug
```

运行 Lint 检查：

```powershell
.\gradlew.bat lintCompatDebug lintModernDebug lintProDebug
```

构建产物位于：

- `app/build/outputs/apk/compat/debug/app-compat-debug.apk`
- `app/build/outputs/apk/modern/debug/app-modern-debug.apk`
- `app/build/outputs/apk/pro/debug/app-pro-debug.apk`

## 项目结构

```text
app/src/main/    三个版本共享的时钟、农历、时间、背景和资源代码
app/src/compat/  兼容版设置界面与平台适配
app/src/modern/  现代版 Material 3 设置界面与平台适配
app/src/pro/     专业版工具页面（闹钟、倒计时、番茄钟、秒表、日历、整点报时），复用现代版界面与资源
app/src/test/    核心逻辑单元测试
```

## 手工验收建议

1. 切换横屏和竖屏，检查时间、日期和农历是否完整显示且没有重叠。
2. 测试纯色及图片背景，并检查带 EXIF 旋转信息的横图、竖图和大尺寸图片。
3. 调整字号、文字颜色、粗体、秒数和动画设置，重启应用后确认设置仍然保留。
4. 切换 12/24 小时制、中文/英文、系统时区和指定地区时区。
5. 启用网络校时并断开网络，确认应用仍能回退到设备时间正常显示。
6. 测试常驻压暗和跨午夜的定时压暗设置。