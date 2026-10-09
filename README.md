<div align="center">

<img src="public/icons/icon-192.png" alt="ClockMods" width="120" />

# ClockMods Web Legacy

**为 IE 内核保留一块清晰、可定制的时钟屏幕**

IE 11 / Trident · 六套时钟主题 · 离线农历 · ES5 入口 · 无账号

</div>

`web-legacy` 是 ClockMods 的兼容网页时钟，面向 IE 11 / Trident、相应旧版 WebView 与现代浏览器。它保留常用显示设置，并用传统脚本和普通 CSS 呈现五套 Material 风格主题，适合仍使用旧内核的设备或嵌入式屏幕。

[核心亮点](#核心亮点) · [版本选择](#版本选择) · [快速上手](#快速上手) · [开发与构建](#开发与构建) · [许可与致谢](#许可与致谢)

## 核心亮点

- **兼容内核也能选择主题**：经典、双区块、轨道、气泡、融合、丝带六套布局，支持卡片 / 强调色、自动文字对比色与阴影。
- **时间显示按需定制**：12 / 24 小时制、隐藏或缩小秒数、闪烁冒号、横竖屏自适应和竖排大字；字体、粗细、字号、日期与时间颜色可调整。
- **日期无需在线接口**：公历格式表达式与中国农历在本地运行，界面支持简体 / 繁體 / English。
- **保留常用环境设置**：纯色或本地图片背景、立即 / 跨午夜定时压暗、自定义留言滚动、整点视觉报时与勿扰时段。
- **天气和校时按需接入**：通过 XHR 连接天气代理，支持缓存、详细轮播与摄氏 / 华氏；可从 HTTP `Date` 头校时。
- **兼容性从入口保证**：ES5 经典脚本，不依赖 ES module、Promise、fetch、Map / Set、ResizeObserver 或 CSS 自定义属性；未支持的全屏功能可降级。

本分支只提供全屏时钟，不加载月历、番茄钟、闹钟、倒计时和秒表页面。没有 PWA / service worker 安装流程；基础时钟与农历在页面资源加载后不依赖天气或校时网络。

## 主题与兼容性

| 主题 | 视觉特点 |
| --- | --- |
| 经典 | 清晰的大字时间与传统全屏布局 |
| 双区块 | 成组时间卡片 |
| 轨道 | 环形时间布局 |
| 气泡 | 圆润数字气泡 |
| 融合 | 层叠卡片布局 |
| 丝带 | 横向时间条带 |

主题用 ES5 DOM、普通 CSS 和 IE flex 前缀实现。支持日期与辅助文字独立字号及天气轮播动效；主题默认自动适配可用区域，手动字号均为 8–80 px，0 表示自动。经典主题保留独立比例控制。设置先编辑再应用，取消保留原配置。配色等选项按当前兼容入口保存，切换主题会初始化对应配色，不提供现代 Web 的完整逐主题偏好存储。

基础数字动效为淡变、上滑、缩放、翻页；天气可选择淡变、上滑、下滑、右滑、缩放、翻页。扫描裁剪、图片模糊、世界时钟与桌面小组件没有引入此入口。

| 项目 | 支持边界 |
| --- | --- |
| 浏览器目标 | IE 11 / Trident 与现代浏览器；更早 IE 版本不在目标内 |
| 样式与脚本 | 经典 script、ES5、普通 CSS；不依赖 CSS Grid、CSS 变量、dvh 或 dialog |
| 颜色与字体 | 不支持原生颜色选择器时使用文本输入；字体使用设备可用的系统字体栈 |
| 时间与时区 | 本地、UTC、中国、日本、纽约、伦敦 |
| 屏幕常亮 | 由操作系统或宿主设置控制，不依赖现代 Screen Wake Lock |
| 本地保存 | 尝试 localStorage；存储受限时当前会话仍可使用，但不能保证刷新后保留配置或图片 |
| 离线与提醒 | 无 service worker 缓存；报时依赖页面运行，不能作为关闭页面后的系统提醒 |

## 版本选择

| 分支 | 适用环境 | 主要定位 |
| --- | --- | --- |
| [main](https://github.com/heyiWF/ClockMods/tree/main) | Android 4.0 / 6.0 / 12 起，按 flavor 区分 | 兼容版、现代版、专业版；功能冻结，继续修复问题与安全维护 |
| [ultimate](https://github.com/heyiWF/ClockMods/tree/ultimate) | Android 12+ | 十二套时钟主题、五套日历主题、世界时钟与桌面小组件 |
| [ultimate-compose](https://github.com/heyiWF/ClockMods/tree/ultimate-compose) | Android 12+ | Ultimate 的 Kotlin / Jetpack Compose 实现 |
| [web](https://github.com/heyiWF/ClockMods/tree/web) | 支持现代 Web API 的浏览器 | 六套时钟主题、六个工具页面、可安装 PWA |
| [web-legacy](https://github.com/heyiWF/ClockMods/tree/web-legacy) | IE 11 / Trident 及现代浏览器 | 保留六套时钟主题与常用设置的兼容网页时钟 |

使用现代浏览器并需要完整工具页、PWA 或扫描等动效时，可以选择 `web`；使用 Android 原生提醒与小组件时，选择对应 Android 分支。

## 快速上手

1. 打开部署的生产页面。自行运行时先构建，再通过 HTTP / HTTPS 服务打开 `dist/`；IE 内核验证不要使用 Vite 热更新入口。
2. 双击时钟或点击设置按钮，选择主题、字体、时间制、日期与背景，点击「应用」。
3. 需要天气时，填写城市 ID 和天气代理 URL；无需在 IE 页面中配置 QWeather 私钥。
4. 需要持续显示时，在设备或宿主设置中调整自动锁屏；整点报时和动效需要页面保持运行。

从现代 `web` 的本地配置切换时，兼容入口会迁移常用的 `clock_prefs.*` 选项，未支持的功能不会因此出现在页面中。

## 天气与校时配置

### XHR 天气代理

建议使用同源代理，避免旧内核的跨域与服务端 TLS 支持差异。代理地址中的 `{location}` 会替换为配置的城市 ID；没有占位符时会追加 `location` 查询参数。

代理可以直接返回以下结构，也支持数据位于 `data`、`now` 或 `results[0].now`：

```json
{
  "city": "深圳",
  "text": "晴",
  "temperature": "29",
  "feelsLike": "31",
  "humidity": "72",
  "windDir": "东南风",
  "windScale": "2"
}
```

天气缓存用于请求失败时继续显示最近一次结果。旧内核是否能连接服务，还取决于代理的 HTTPS、跨域响应与宿主网络能力。

### HTTP 校时

校时地址需要允许 HEAD 并返回 `Date` 头。跨域服务还应允许来源，并设置 `Access-Control-Expose-Headers: Date`；HTTPS 页面需选择 HTTPS 地址。这是 HTTP 时间估算，不是原生 UDP NTP。网络失败不阻止基础时钟继续运行。

## 本地数据与隐私

无需账号，不含广告或行为统计，没有内置云同步。设置、图片和天气缓存尝试保存在当前浏览器本地；存储容量或权限不足时不能保证跨刷新保存。天气代理会收到配置的城市 ID，校时会请求所选服务器；QWeather 签名与私钥由代理服务端处理。

## 开发与构建

开发环境推荐 Node.js 22.12+ 与 npm，使用仓库锁文件安装依赖：

```bash
npm ci
npm run dev
npm run typecheck
npm test
npm run build
npm run preview
```

生产文件输出到 `dist/`，可部署到静态站点服务。天气代理是可选的独立服务，不属于静态页面部署的必需组件。

### IE 内核验证

`npm run dev` 用于现代开发环境，Vite 会注入模块化热更新脚本。IE 验证请执行 `npm run build`，然后用 `npm run preview` 或其他静态服务访问生产页面。

```text
index.html                  单时钟页面与设置面板
public/legacy-clock.css     IE 11 兼容主题与布局
public/legacy-clock.js      ES5 时钟、设置、XHR 与存储逻辑
public/legacy-lunar.js      lunar-javascript UMD 农历引擎
tests/                      共享逻辑与兼容入口契约测试
```

测试包含生产入口与 ES5 兼容契约，并在禁用部分现代 API 的环境中验证启动。此类检查及现代浏览器预览不能替代真实 IE 11 / Trident 验收；发布兼容性变更前应检查目标内核中的设置、布局、天气与校时。

反馈问题请注明宿主、IE 文档模式、系统版本、主题、窗口尺寸和复现步骤。贡献时优先保留兼容入口的语法与 API 边界，不应直接引入现代 Web 的构建入口。

## 许可与致谢

ClockMods 项目采用 [MIT 许可证](https://github.com/heyiWF/ClockMods/blob/main/LICENSE)。兼容入口的农历引擎为 [lunar-javascript](https://github.com/6tail/lunar-javascript)，许可见 [lunar-LICENSE.txt](public/licenses/lunar-LICENSE.txt)。天气数据来自 [QWeather](https://www.qweather.com)，图标采用 [QWeather Icons](https://icons.qweather.com) 的 CC BY 4.0 许可；其他资源以随附许可为准。

## 本轮兼容改进与验证

- 设置提供隔离的实时预览，颜色、字号、字体名称和字重等草稿即时显示；预览不保存配置、不发起天气或校时请求，取消销毁预览。
- 日期与辅助文字分别分配空间，互不牵动；时间字号在区域上限以内具有可见缩放。支持输入已安装字体名称及选择 100–900 字重，缺少中文字形时使用系统回退字体。旧内核不枚举本机字体库。
- 设置和时区城市名称完整跟随简体、繁体、英语；自定义留言不翻译。鼠标静止三秒隐藏，设置打开时保持光标；天气来源提示只随鼠标移动短暂显示。触屏双击可打开设置。
- 197 项测试、类型检查和生产构建通过；Chrome 中完成六套主题 × 五种尺寸（240×720 至 3840×2160）检查，验证预览、取消、持久化、字号独立、光标与语言。运行测试禁用现代 API，产物保留 ES5 经典脚本。本轮没有实体 IE 设备实测。
- 此兼容入口仍只有时钟，不包含现代版的月历、世界时钟或多页滑动，也不新增旧内核缺失的高斯模糊与字体枚举。繁体转换使用 OpenCC 字符表，许可见 `public/licenses/opencc.txt`。
