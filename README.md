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

## 只有一个版本：兼容版

本分支只保留兼容版（ClockMods Lite），支持 Android 4.0 及以上，安装后包名为 `com.clockmods.compat`。现代版与专业版的源码、依赖和构建配置都已移除。

## 远一点，也能看清时间

全屏大字搭配屏幕常亮，横放、竖放都能使用。你可以隐藏秒数，让画面更安静；也可以选择竖排时、分、秒，把屏幕变成一块醒目的数字时钟。

12 / 24 小时制、字号都能按习惯调整，日期与时间可分别设置，既适合近处阅读，也适合放在房间另一侧。字体固定为内置 Roboto，文字颜色统一为白色，省去反复调整的麻烦。

## 从白天到夜晚，调成喜欢的样子

选一张本机照片作为背景，或者用简单的纯色衬托时间。立即压暗和定时压暗让显示亮度随作息变化，跨过午夜也能按设定运行。

## 日期与天气

公历、星期和中国农历可离线查看。启用天气后，可以轮播温度、体感、湿度、风力等信息；文字过长时会滚动展示。

天气支持手动选择城市或自动定位，需要联网及有效的天气服务配置。网络校时可以按需开启，基础时钟和离线日期不依赖天气服务。

## 开始使用

1. 构建并安装兼容版 APK（见下方「自己构建」）。
2. **双击时钟区域**打开设置，调整样式和功能，点击“应用”保存。
3. 将设备横放或竖放，按需要开启常亮、压暗和天气。
4. 使用网络校时或天气时，按系统提示授予相应权限。

## 自己构建

本分支不配置 CI，不会自动产出安装包，需要时在本地构建：

```bash
./gradlew assembleCompatDebug
```

产物位于 `app/build/outputs/apk/compat/debug/app-compat-debug.apk`。

## 本地使用，按需联网

设置与选取的背景图片保存在设备上，打开应用即可使用和调整。

仅自动定位天气需要位置权限，也可以手动选城市。天气和校时会连接对应服务；闹钟提醒的呈现方式受 Android 权限与后台策略影响。
## 更多

- [反馈问题与建议](https://github.com/heyiWF/ClockMods/issues)
- [开发与配置参考](docs/product-development-guide.md)
- 其他版本：[Android 经典系列](https://github.com/heyiWF/ClockMods/tree/main) · [Ultimate](https://github.com/heyiWF/ClockMods/tree/ultimate) · [Ultimate Compose](https://github.com/heyiWF/ClockMods/tree/ultimate-compose) · [Web](https://github.com/heyiWF/ClockMods/tree/web) · [Web Legacy](https://github.com/heyiWF/ClockMods/tree/web-legacy)

## 许可与致谢

项目采用 [MIT 许可证](LICENSE)。天气数据来自 [QWeather](https://www.qweather.com)，[QWeather Icons](https://icons.qweather.com) 图标采用 CC BY 4.0；农历引擎 [tyme4j](https://github.com/6tail/tyme4j) 与节假日数据 [holiday-cn](https://github.com/NateScarlet/holiday-cn) 采用 MIT。字体及其他资源以各自随附许可为准。
