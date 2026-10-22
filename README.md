<div align="center">

<img src="docs/media/app-icon-playstore.png" alt="ClockMods" width="120" />

# ClockMods

**让闲置手机，再次成为每天用得上的时钟。**

全屏常亮 · 横竖屏适配 · 离线农历 · 个性化外观

</div>

ClockMods 是一款可定制的 Android 全屏时钟。把手机放在书桌、床头或客厅，时间、公历和农历便能清楚地呈现在眼前。旧设备也能找到合适的版本，继续陪伴日常生活。

> **关于本分支（`mylite`）**
> 这是作者给自己用的「时钟姬」分支，只求一块干净、顺眼的床头时钟，不是面向发布的版本。
> 相比 `main`，这里砍掉了英文界面、日期格式自定义和天气行自定义留言，字体固定为内置 Roboto，状态栏改成「网络 + 电量」两行文字，并顺手压缩了安装包体积。
> `main` 才是正式的公开分支，功能与发布流程都以它为准。

## 为你的设备，选一个合适的版本

| 版本 | 支持系统 | 适合这样的你 |
| --- | --- | --- |
| ClockMods Lite · 兼容版 | Android 4.0 及以上 | 想让旧手机稳定显示时间，保留熟悉、简洁的设置方式 |
| ClockMods · 现代版 | Android 6.0 及以上 | 喜欢现代设置界面，希望轻松调整字体、背景和显示信息 |
| ClockMods Pro · 专业版 | Android 12 及以上 | 希望在时钟之外，一并查看月历、设置提醒和管理计时 |

三个版本可在满足系统要求的设备上同时安装。经典系列以兼容性与日常使用为主，持续进行问题修复和安全维护。

## 远一点，也能看清时间

全屏大字搭配屏幕常亮，横放、竖放都能使用。你可以隐藏秒数，让画面更安静；也可以选择竖排时、分、秒，把屏幕变成一块醒目的数字时钟。

12 / 24 小时制、字号都能按习惯调整，日期与时间可分别设置，既适合近处阅读，也适合放在房间另一侧。字体固定为内置 Roboto，文字颜色统一为白色，省去反复调整的麻烦。

## 从白天到夜晚，调成喜欢的样子

选一张本机照片作为背景，或者用简单的纯色衬托时间。立即压暗和定时压暗让显示亮度随作息变化，跨过午夜也能按设定运行。

现代版与专业版在 Android 12 及以上支持系统动态取色。专业版还提供多种数字切换动效，让时间变化更自然。

## 日期与天气

公历、星期和中国农历可离线查看。启用天气后，可以轮播温度、体感、湿度、风力等信息；文字过长时会滚动展示。

天气支持手动选择城市或自动定位，需要联网及有效的天气服务配置。网络校时可以按需开启，基础时钟和离线日期不依赖天气服务。

## 专业版，让常用工具就在手边

| 工具 | 用来做什么 |
| --- | --- |
| 月历 | 浏览月份、选择日期，查看农历、节气、节日与传统宜忌 |
| 番茄钟 | 为工作与休息安排一段专注时间 |
| 闹钟 | 设置定时提醒，按设备权限提供响铃、振动与全屏提示 |
| 倒计时 | 为一件事设定时长，到点提醒 |
| 秒表 | 连续计时与记录计次 |

月历支持已收录年份的休 / 班标记，横屏还可结合天气与预报展示。整点视觉报时与勿扰时段，让提醒更贴合作息。传统宜忌作为民俗历法内容提供，调休安排以已收录的数据为准。

[观看专业版工具演示](docs/media/pro-features-overview.mp4) · [观看中文报时演示](docs/media/pro-chime-12h-zh.mp4)

## 开始使用

1. 下载适合设备系统的版本并安装。
2. **双击时钟区域**打开设置，调整样式和功能，点击“应用”保存。
3. 将设备横放或竖放，按需要开启常亮、压暗和天气。
4. 专业版可左右滑动切换工具页；使用提醒时按系统提示授予相应权限。

## 下载体验
[![Build APK](https://github.com/heyiWF/ClockMods/actions/workflows/build-apk.yml/badge.svg?branch=main)](https://github.com/heyiWF/ClockMods/actions/workflows/build-apk.yml?query=branch%3Amain)

打开 [安装包下载页面](https://github.com/heyiWF/ClockMods/actions/workflows/build-apk.yml?query=branch%3Amain)，选择一次成功的构建，在 **Artifacts** 中下载 `clockmods-apks-main-运行序号`。解压后选择兼容版、现代版或专业版的 APK 安装。

目前提供开发预览版 APK。

## 本地使用，按需联网

设置与选取的背景图片保存在设备上，打开应用即可使用和调整。

仅自动定位天气需要位置权限，也可以手动选城市。天气和校时会连接对应服务；闹钟提醒的呈现方式受 Android 权限与后台策略影响。
## 更多

- [反馈问题与建议](https://github.com/heyiWF/ClockMods/issues)
- [开发与配置参考](docs/product-development-guide.md)
- 其他版本：[Android 经典系列](https://github.com/heyiWF/ClockMods/tree/main) · [Ultimate](https://github.com/heyiWF/ClockMods/tree/ultimate) · [Ultimate Compose](https://github.com/heyiWF/ClockMods/tree/ultimate-compose) · [Web](https://github.com/heyiWF/ClockMods/tree/web) · [Web Legacy](https://github.com/heyiWF/ClockMods/tree/web-legacy)

## 许可与致谢

项目采用 [MIT 许可证](LICENSE)。天气数据来自 [QWeather](https://www.qweather.com)，[QWeather Icons](https://icons.qweather.com) 图标采用 CC BY 4.0；农历引擎 [tyme4j](https://github.com/6tail/tyme4j) 与节假日数据 [holiday-cn](https://github.com/NateScarlet/holiday-cn) 采用 MIT。字体及其他资源以各自随附许可为准。
