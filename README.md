# ClockMods Web Legacy

`web-legacy` 是从 `web` 分支创建的 IE 内核兼容版本。该分支只提供全屏网页时钟，不加载月历、番茄钟、闹钟、倒计时和秒表页面。

## 兼容目标

- IE 11 / Trident，以及使用 IE 内核的旧版 WebView。
- 页面使用经典 `<script>`，业务代码采用 ES5 语法。
- 不依赖 ES module、Promise、`fetch`、`ResizeObserver`、CSS 自定义属性、CSS Grid、`dvh` 或 `<dialog>`。
- 不生成 PWA / service worker 引导代码，避免 IE 解析构建工具产生的现代 JavaScript。
- 现代浏览器同样可用；旧浏览器不支持的全屏能力会自动降级，不影响时钟刷新。

## 时钟功能

- 秒边界实时刷新，12/24 小时制，可隐藏秒数或使用小字号秒数。
- 冒号闪烁，淡入淡出、上滑、缩放和翻页切换动效。
- 横竖屏自适应；竖屏可使用时、分、秒三行大字。
- 公历日期格式表达式，中国农历，简体中文、繁體中文和 English。
- 本地时间、UTC、中国、日本、纽约和伦敦时区。
- 字体、粗细、字号、时间颜色和日期颜色设置。
- 纯色或本地图片背景，立即压暗或跨午夜定时压暗。
- 自定义留言，整点视觉报时及勿扰时段。
- 通过 XHR 请求天气代理，显示城市、天气、温度、体感、湿度和风力，并缓存最后一次结果。
- 通过 HTTP `Date` 响应头进行可选网络校时。
- `localStorage` 设置持久化，并迁移现代 `web` 分支中常用的 `clock_prefs.*` 设置。

天气服务建议使用同源服务端代理。IE 中不保存和风天气私钥；代理可返回以下任一结构：

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

也兼容对象位于 `data`、`now` 或 `results[0].now` 的返回结构。代理地址中的 `{location}` 会被替换为设置的城市 ID；没有占位符时会追加 `location` 查询参数。

## 开发与构建

```bash
npm install
npm run dev
npm test
npm run build
npm run preview
```

生产文件输出到 `dist/`。兼容入口由以下文件组成：

```text
index.html                  单时钟页面和设置面板
public/legacy-clock.css     IE11 兼容样式
public/legacy-clock.js      ES5 时钟、设置和网络逻辑
public/legacy-lunar.js      lunar-javascript 1.7.7 UMD 农历引擎
```

需要用 IE 内核验收时，请先执行 `npm run build`，再使用 `npm run preview` 打开生产文件。Vite 开发服务器会注入仅供热更新使用的模块脚本；生产构建不会包含该脚本。

`npm test` 会排除已经从此分支入口移除的月历页面集成测试，但继续运行日期、农历、时钟和共享模型测试，并包含 legacy HTML/CSS/JavaScript 的兼容契约检查。

农历引擎采用 MIT 许可证，见 `public/licenses/lunar-LICENSE.txt`。
