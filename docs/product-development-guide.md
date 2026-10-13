# 开发与配置参考

[返回产品介绍](../README.md)



## 开发与构建

推荐使用 Android Studio 或 JDK 21（CI 使用版本），安装 Android SDK Platform 37 与 Build Tools 37.0.0，使用仓库的 Gradle Wrapper。最低运行系统保持为 API 31。

```powershell
.\gradlew.bat --no-daemon testUltimateDebugUnitTest lintUltimateDebug assembleUltimateDebug
```

Debug APK 位于 `app/build/outputs/apk/ultimate/debug/`。组合所需任务可复用共享依赖，避免为同一变更分别启动多次 Gradle；设备行为、动态画面和桌面小组件需在相应宿主中验证。

### 天气构建配置

天气为可选功能。自行构建时，将 [`qweather.properties.example`](../qweather.properties.example) 复制为 `qweather.properties`，填写 `apiHost`、`credentialId`、`developerId`、`projectId` 和 PKCS#8 Ed25519 私钥的 Base64 内容。

GitHub Actions 的非 PR 构建会写入天气配置：`QWEATHER_API_HOST` 来自仓库 Variables，其余四项分别来自 `QWEATHER_CREDENTIAL_ID`、`QWEATHER_DEVELOPER_ID`、`QWEATHER_PROJECT_ID`、`QWEATHER_PRIVATE_KEY_BASE64` Secrets。PR 构建跳过这一步，任务结束后清理临时配置文件。该配置参与 APK 构建；未提供有效配置时，基础时钟和离线工具仍可使用。

### 源码与扩展

```text
app/src/main/       Kotlin 核心逻辑、Compose 主题与共享资源
app/src/ultimate/   Compose 页面、Canvas 时钟、提醒、RemoteViews 与离线资产
app/src/main/java/com/clockmods/sdk/clock/  时钟样式契约
app/src/test/       核心逻辑单元测试
app/src/testUltimate/  主题与工具测试
```

时钟主题通过元数据、能力声明与渲染器接入，见 [Clock Style SDK](clock-style-sdk.md)；这是源码扩展机制，新增主题需要编译。桌面小组件继续使用 Android RemoteViews，见 [小组件契约](WIDGET_CONTRACT.md)。

Compose 日历的布局对应关系见 [日历主题说明](ultimate-calendar-parity.md)，缓存与滑动优化见 [日历性能说明](calendar-swipe-performance.md)。反馈问题时请注明本分支、主题、Android 版本和复现步骤；涉及走秒、滚动或切换动画请附短录像。
