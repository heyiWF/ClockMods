# 开发与配置参考

[返回产品介绍](../README.md)



## 开发与构建

推荐使用 Android Studio 或 JDK 21（CI 使用版本），安装 Android SDK Platform 36.1 与 Build Tools 36.1.0，使用仓库的 Gradle Wrapper。构建 SDK 与最低运行系统是不同概念；本分支只保留兼容版，最低运行系统为 API 14。

```powershell
.\gradlew.bat --no-daemon testCompatDebugUnitTest lintCompatDebug assembleCompatDebug
```

Debug APK 位于 `app/build/outputs/apk/compat/debug/`。

### 天气构建配置

天气为可选功能。自行构建时，将 [`qweather.properties.example`](../qweather.properties.example) 复制为 `qweather.properties`，填写 `apiHost`、`credentialId`、`developerId`、`projectId` 和 PKCS#8 Ed25519 私钥的 Base64 内容。

GitHub Actions 的非 PR 构建会写入天气配置：`QWEATHER_API_HOST` 来自仓库 Variables，其余四项分别来自 `QWEATHER_CREDENTIAL_ID`、`QWEATHER_DEVELOPER_ID`、`QWEATHER_PROJECT_ID`、`QWEATHER_PRIVATE_KEY_BASE64` Secrets。PR 构建跳过这一步，任务结束后清理临时配置文件。该配置参与 APK 构建；未提供有效配置时，基础时钟和离线工具仍可使用。

### 源码与贡献

```text
app/src/main/    时钟、农历、网络与背景逻辑
app/src/compat/  旧系统平台控件、设置面板与适配
app/src/test/    共享逻辑单元测试
```

反馈问题时请附 Android 版本、复现步骤及相关设置；涉及显示效果可附截图。提交修复时应维持最低 API 14，并运行 `testCompatDebugUnitTest`、`lintCompatDebug` 与 `assembleCompatDebug`；权限与旧系统行为还需设备验证。
