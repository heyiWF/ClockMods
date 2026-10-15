# 开发与配置参考

[返回产品介绍](../README.md)

## 天气与校时配置

### QWeather 直连或代理

直连模式在设置中填写 API Host、凭据 ID、项目 ID 与 PKCS#8 Ed25519 私钥 Base64，浏览器生成 JWT 请求天气。自动定位需要浏览器位置权限，也可手动选城市。天气支持缓存、更新间隔、详细轮播与摄氏 / 华氏切换。

也可配置自己的代理 URL，将签名留在服务端。仓库提供 [Cloudflare Worker 示例](../proxy/cloudflare-worker.js)，部署前按文件说明填写环境变量与允许的来源；其他平台需要适配各自请求入口。代理负责跨域响应与天气接口转换，页面本身仍是静态站点。

### HTTP 校时

填写允许 HEAD 请求且返回 `Date` 头的 HTTP / HTTPS 地址。跨域地址还需允许站点来源，并通过 `Access-Control-Expose-Headers: Date` 公开该响应头；HTTPS 页面应使用 HTTPS 校时服务。失败时基础时钟继续使用本地时间或已有有效偏移。


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

### 源码与贡献

```text
src/              TypeScript 页面、设置、时间与天气逻辑
src/styles/       Material 令牌、交互样式与主题布局
public/           图标、静态资源与许可文件
proxy/            可选天气代理示例
tests/            核心逻辑、DOM 与主题测试
```

Material 风格由共享颜色、字阶、形状和动效令牌构成，界面同时保留键盘聚焦与状态反馈。构建使用 Vite 与 TypeScript，PWA 缓存由构建插件生成。

反馈问题时请附浏览器 / 系统版本、主题、窗口尺寸及复现步骤。提交变更应运行类型检查、测试和生产构建；布局与动效需检查窄屏和横屏，离线与安装体验需用生产预览验证。
