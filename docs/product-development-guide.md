# 开发与配置参考

[返回产品介绍](../README.md)



## 开发与构建

推荐使用 Android Studio 或 JDK 21，安装 Android SDK Platform 36.1 与 Build Tools 36.1.0，使用仓库的 Gradle Wrapper。构建 SDK 与最低运行系统是不同概念；本分支只保留兼容版，最低运行系统为 API 14。

```powershell
.\gradlew.bat --no-daemon testCompatDebugUnitTest lintCompatDebug assembleCompatDebug
```

Debug APK 位于 `app/build/outputs/apk/compat/debug/`。

### 天气构建配置

天气为可选功能。自行构建时，将 [`qweather.properties.example`](../qweather.properties.example) 复制为 `qweather.properties`，填写 `apiHost`、`credentialId`、`developerId`、`projectId` 和 PKCS#8 Ed25519 私钥的 Base64 内容。

本分支不配置 CI，天气配置只在本地构建时从仓库根目录的 `qweather.properties` 读取；该文件已被 `.gitignore` 忽略，不会进入版本库。未提供有效配置时，基础时钟和离线工具仍可使用。

### 源码与贡献

```text
app/src/main/    时钟、农历、网络与背景逻辑
app/src/compat/  旧系统平台控件、设置面板与适配
app/src/test/    共享逻辑单元测试
```

反馈问题时请附 Android 版本、复现步骤及相关设置；涉及显示效果可附截图。提交修复时应维持最低 API 14，并运行 `testCompatDebugUnitTest`、`lintCompatDebug` 与 `assembleCompatDebug`；权限与旧系统行为还需设备验证。
