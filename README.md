<div align="center">

<img src="docs/media/app-icon-playstore.png" alt="ClockMods" width="120" />

# ClockMods

**让闲置 Android 设备成为一块清晰、实用的时钟屏幕**

三档系统兼容 · 全屏常亮 · 离线农历 · 无广告 · 无账号

</div>

ClockMods 是可定制的 Android 全屏时钟，适合桌面、床头与闲置手机或平板。时间、公历和农历一眼可见；背景、字体、字号、时区和天气可以按使用场景调整。`main` 提供三个独立的 flavor，让旧设备与较新的设备各有合适的版本。

[核心亮点](#核心亮点) · [版本选择](#版本选择) · [快速上手](#快速上手) · [开发与构建](#开发与构建) · [许可与致谢](#许可与致谢)

## 核心亮点

- **旧设备也有合适的版本**：兼容版从 Android 4.0 起使用平台原生控件；现代版提供 Material 3 设置；专业版加入日历与计时工具。三个版本使用不同包名，可在满足系统要求的设备上并存。
- **为远距离阅读设计**：全屏常亮、横竖屏自适应、可隐藏或缩小秒数，竖屏可显示时 / 分 / 秒三行大字；时间与日期分别设置字号、颜色和粗细。
- **无需网络的日期信息**：公历、星期、中国农历随时可用；专业版增加月历、节气、节日、宜忌与已收录年份的休 / 班安排。
- **从白天到夜晚的外观设置**：纯色与本地图片背景、字体选择、立即压暗和跨午夜定时压暗；现代版与专业版在 Android 12+ 支持系统动态取色。
- **按需开启联网能力**：多服务器网络校时、系统或指定时区、天气与详细信息轮播。自定义留言过长时横向滚动，保持设定字号。
- **专业版的实用工具**：时钟、日历、番茄钟、闹钟、倒计时、秒表集中在一个应用中，并提供整点视觉报时与勿扰时段。

## 版本选择

### 本分支的三个 Android 版本

| flavor | 应用名称与包名 | 最低系统 | 适合谁 |
| --- | --- | --- | --- |
| `compat` | ClockMods Lite · `com.clockmods.compat` | Android 4.0 / API 14 | 需要平台原生控件和旧系统支持；中 / 英文界面 |
| `modern` | ClockMods · `com.clockmods.modern` | Android 6.0 / API 23 | 希望使用 Material 3 设置与现代图片选择体验；中 / 英文界面 |
| `pro` | ClockMods Pro · `com.clockmods.pro` | Android 12 / API 31 | 需要日历、提醒和计时工具；简体 / 繁體 / English |

`main` 当前冻结功能更新，保留三个 flavor 的兼容性，继续进行漏洞修复、安全维护与必要的性能优化。Ultimate 的多主题、世界时钟和桌面小组件不属于本分支。

### 项目其他分支

| 分支 | 适用环境 | 主要定位 |
| --- | --- | --- |
| [main](https://github.com/heyiWF/ClockMods/tree/main) | Android 4.0 / 6.0 / 12 起，按 flavor 区分 | 兼容版、现代版、专业版；功能冻结，继续修复问题与安全维护 |
| [ultimate](https://github.com/heyiWF/ClockMods/tree/ultimate) | Android 12+ | 十二套时钟主题、五套日历主题、世界时钟与桌面小组件 |
| [ultimate-compose](https://github.com/heyiWF/ClockMods/tree/ultimate-compose) | Android 12+ | Ultimate 的 Kotlin / Jetpack Compose 实现 |
| [web](https://github.com/heyiWF/ClockMods/tree/web) | 支持现代 Web API 的浏览器 | 六套时钟主题、六个工具页面、可安装 PWA |
| [web-legacy](https://github.com/heyiWF/ClockMods/tree/web-legacy) | IE 11 / Trident 及现代浏览器 | 保留六套时钟主题与常用设置的兼容网页时钟 |

## 快速上手

1. 从本分支成功的 [APK 构建任务](https://github.com/heyiWF/ClockMods/actions/workflows/build-apk.yml?query=branch%3Amain) 下载适合设备的产物，或按下文自行构建。
2. 打开应用，**双击时钟区域**进入设置。在「样式」中调整显示，在「功能」中配置语言、时间、天气等，点击「应用」保存。
3. 想要显示天气，可选择手动城市，或启用自动定位并授予位置权限；需要构建中已有有效的 QWeather 配置。
4. 专业版可左右滑动切换工具页。使用闹钟时按系统提示允许通知、精确闹钟和全屏提醒；实际提醒方式受 Android 权限与后台策略影响。
5. 需要开机进入时钟时，开启「开机自启动」并将应用设为默认桌面。此功能依赖系统的桌面启动流程。

### 显示与天气

支持 12 / 24 小时制、中文或英文上午 / 下午标记、冒号闪烁、状态图标与时区选择。兼容版和现代版提供预设日期格式，专业版还支持日期表达式与自定义分隔符，并增加淡变、上滑、下滑、缩放、翻转等数字过渡。

网络校时可选 30 分钟、1 小时、6 小时或每天同步，并在服务器不可用时尝试其他服务器；失败时保留有效校时样本，没有有效样本则使用设备时间。

天气可选 10 分钟至 12 小时的更新间隔，轮播体感温度、湿度、风向风力、预警等信息；自定义留言可与天气共同显示。天气依赖网络与服务端数据，失败时不会阻止基础时钟运行。

### 专业版工具

| 页面 | 主要用途 |
| --- | --- |
| 时钟 | 沉浸式时间显示、整点视觉报时、可跨午夜的勿扰时段 |
| 日历 | 月份切换、日期点选、回到今天、农历与宜忌；横屏仪表盘显示时间、天气和未来三日预报 |
| 番茄钟 | 工作与休息阶段计时 |
| 闹钟 | 排程提醒、通知、全屏提示、响铃与振动；重启和时间变化后重新排程 |
| 倒计时 | 指定时长并在结束时提醒 |
| 秒表 | 连续计时与计次 |

农历、二十四节气、传统与公历节日、数九 / 三伏和每日宜忌由 [tyme4j](https://github.com/6tail/tyme4j) 在设备端计算。法定节假日的「休 / 班」安排使用随应用打包的 [holiday-cn](https://github.com/NateScarlet/holiday-cn) 数据；只有已收录年份能显示对应安排，不能从农历推算未来的调休。日历本身无需联网。

### 功能演示

以下为专业版基础功能与报时演示：

| 内容 | 视频 |
| --- | --- |
| 时钟、日历与计时工具概览 | [观看](docs/media/pro-features-overview.mp4) |
| 中文 12 小时制整点报时 | [观看](docs/media/pro-chime-12h-zh.mp4) |
| English 12-hour hourly chime | [观看](docs/media/pro-chime-12h-en.mp4) |

## 权限与数据

无需账号，不含广告、云同步或用户行为统计。设置与选取的背景图片保存在设备上；系统备份行为由系统与应用配置决定。启用网络校时会联系时间服务器；启用天气会向 QWeather 发送所选城市或定位坐标。

- **网络权限**：用于可选校时、天气请求，以及网络状态显示。
- **位置权限**：仅自动定位天气需要；手动选城市可以不授予位置权限，不进行后台定位。
- **背景图片**：通过系统选择器处理主动选择的单张图片。
- **专业版提醒权限**：通知、精确闹钟、全屏提醒、振动、响铃前台服务及开机广播，用于提醒与恢复排程。兼容版和现代版不包含专业版提醒工具。

## 开发与构建

推荐使用 Android Studio 或 JDK 21（CI 使用版本），安装 Android SDK Platform 36.1 与 Build Tools 36.1.0，使用仓库的 Gradle Wrapper。构建 SDK 与最低运行系统是不同概念；三个 flavor 的最低系统仍为 API 14 / 23 / 31。

```powershell
# 按需要选择 flavor；这里示例只构建兼容版
.\gradlew.bat --no-daemon testCompatDebugUnitTest lintCompatDebug assembleCompatDebug

# 检查与构建三个版本
.\gradlew.bat --no-daemon testCompatDebugUnitTest testModernDebugUnitTest testProDebugUnitTest lintCompatDebug lintModernDebug lintProDebug assembleCompatDebug assembleModernDebug assembleProDebug
```

Debug APK 位于 `app/build/outputs/apk/<flavor>/debug/`。在同一次 Gradle 调用中组合所需任务，共享依赖可复用；只修改某个 flavor 时，日常验证可先选择对应任务，跨版本变更需覆盖三个 flavor。

### 天气构建配置

天气为可选功能。自行构建时，将 [`qweather.properties.example`](qweather.properties.example) 复制为 `qweather.properties`，填写 `apiHost`、`credentialId`、`developerId`、`projectId` 和 PKCS#8 Ed25519 私钥的 Base64 内容。

GitHub Actions 的非 PR 构建会写入天气配置：`QWEATHER_API_HOST` 来自仓库 Variables，其余四项分别来自 `QWEATHER_CREDENTIAL_ID`、`QWEATHER_DEVELOPER_ID`、`QWEATHER_PROJECT_ID`、`QWEATHER_PRIVATE_KEY_BASE64` Secrets。PR 构建跳过这一步，任务结束后清理临时配置文件。该配置参与 APK 构建；未提供有效配置时，基础时钟和离线工具仍可使用。

### 源码与贡献

```text
app/src/main/    三个版本共享的时钟、农历、网络与背景逻辑
app/src/compat/  旧系统平台控件与适配
app/src/modern/  Material 3 设置与平台适配
app/src/pro/     专业版工具、提醒与离线资产
app/src/test/    共享逻辑单元测试
```

反馈问题时请附 flavor、Android 版本、复现步骤及相关设置；涉及显示效果可附截图，涉及动效请附短录像。提交修复时应维持最低 API 与三种版本边界，并运行受影响 flavor 的单元测试、lint 和构建；权限、闹钟与旧系统行为还需设备验证。

## 许可与致谢

项目采用 [MIT 许可证](LICENSE)。天气数据来自 [QWeather](https://www.qweather.com)，[QWeather Icons](https://icons.qweather.com) 图标采用 CC BY 4.0；农历引擎 [tyme4j](https://github.com/6tail/tyme4j) 与节假日数据 [holiday-cn](https://github.com/NateScarlet/holiday-cn) 采用 MIT。字体及其他资源以各自随附许可为准。
