# 开发与配置参考

[返回产品介绍](../README.md)



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

天气为可选功能。自行构建时，将 [`qweather.properties.example`](../qweather.properties.example) 复制为 `qweather.properties`，填写 `apiHost`、`credentialId`、`developerId`、`projectId` 和 PKCS#8 Ed25519 私钥的 Base64 内容。

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
