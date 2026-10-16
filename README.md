<div align="center">

<img src="public/icons/icon-192.png" alt="ClockMods" width="120" />

# ClockMods Web Legacy

**让旧浏览器，也能拥有一块喜欢的时钟屏幕。**

IE 11 / Trident · 六套主题 · 离线农历 · 实时样式预览

</div>

ClockMods Web Legacy 是面向 IE 11 / Trident 及相应旧版浏览器宿主的网页时钟。保留清晰的时间、农历与外观设置，让仍在使用旧内核的电脑或嵌入式屏幕，也能成为一块日常看得见、用得上的时钟。

## 六套主题，给旧屏幕一点新鲜感

| 主题 | 视觉特点 |
| --- | --- |
| 经典 | 清晰的大字时间与传统全屏布局 |
| 双区块 | 成组时间卡片 |
| 轨道 | 环形时间布局 |
| 气泡 | 圆润数字气泡 |
| 融合 | 层叠卡片布局 |
| 丝带 | 横向时间条带 |

从传统大字到成组卡片、环形与条带布局，选择适合屏幕的构图，再调整颜色、字体和背景。横放、竖放都有对应的显示方式。

## 调到合适，再留下来

12 / 24 小时制、秒数、日期和辅助文字都能按习惯设置。字号可自动适配，也可以手动调整；支持输入已安装字体名称和选择字重。

设置面板会实时预览颜色、字体与字号，点击“应用”才保存，取消不会覆盖原配置。配色随主题切换初始化，适合选定一套喜欢的主题后继续细调。

## 时间之外，也能放一点日常信息

中国农历与公历在本地计算，页面资源加载后，基础时钟和日期不依赖天气服务。界面支持简体中文、繁體中文与 English。

可以选取本机图片作为背景，放上一句滚动留言，或设置整点视觉报时与安静时段。立即压暗、跨午夜定时压暗，让画面适应不同的使用时间。

需要天气时，填入城市与天气代理地址，查看温度及详细信息；请求失败时可继续显示最近的缓存。天气与网络校时均按需配置。

## 专注于一块持续显示的时钟

这个版本提供持续显示的时钟页面，适合使用旧内核的电脑与嵌入式设备。

持续显示时，请在设备或宿主中调整自动锁屏，并保持页面运行以使用报时。本地保存取决于浏览器的存储权限和容量。

## 开始体验

1. 打开已部署的兼容页面。自行部署请参考 [部署与配置说明](docs/product-development-guide.md)，使用构建后的生产页面。
2. **双击时钟或点击设置按钮**，选择主题、字体、日期与背景。
3. 查看实时预览，点击“应用”保存喜欢的样式。
4. 如需天气，填写城市 ID 与代理地址；长时间展示时保持页面打开。

兼容目标为 IE 11 / Trident，也可在现代浏览器中使用。获取源码见 [Web Legacy 版本](https://github.com/heyiWF/ClockMods/tree/web-legacy)。

## 本地设置，简单使用

设置、背景图片与天气缓存保存在当前浏览器，打开页面即可开始使用和调整。

天气通过你配置的代理获取，签名凭据由代理处理。具体接入方式见 [天气与校时配置](docs/product-development-guide.md#天气与校时配置)。

## 更多

- [反馈问题与建议](https://github.com/heyiWF/ClockMods/issues)
- [开发与配置参考](docs/product-development-guide.md)
- 其他版本：[Android 经典系列](https://github.com/heyiWF/ClockMods/tree/main) · [Ultimate](https://github.com/heyiWF/ClockMods/tree/ultimate) · [Ultimate Compose](https://github.com/heyiWF/ClockMods/tree/ultimate-compose) · [Web](https://github.com/heyiWF/ClockMods/tree/web) · [Web Legacy](https://github.com/heyiWF/ClockMods/tree/web-legacy)

## 许可与致谢

ClockMods 项目采用 [MIT 许可证](https://github.com/heyiWF/ClockMods/blob/main/LICENSE)。兼容入口的农历引擎为 [lunar-javascript](https://github.com/6tail/lunar-javascript)，许可见 [lunar-LICENSE.txt](public/licenses/lunar-LICENSE.txt)。天气数据来自 [QWeather](https://www.qweather.com)，图标采用 [QWeather Icons](https://icons.qweather.com) 的 CC BY 4.0 许可；其他资源以随附许可为准。
