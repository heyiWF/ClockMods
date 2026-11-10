# README 录屏

- 录制日期：2026-11-10（Asia/Shanghai）。
- 应用来源：`main` 的 `25c56dd`，Pro Debug 构建。
- 环境：Android 15 模拟器，1920 × 1080 横屏，中文界面。
- `pro-features-overview.mp4`：时钟、月历、番茄钟、闹钟、倒计时、秒表及设置面板。
- `pro-chime-12h-zh.mp4`：12 小时制中文视觉报时动画。录制脚本主动调用应用现有报时入口预览下一个整点，不修改系统时间；这是视觉动画，无音轨。

视频直接录制应用运行画面，只做 H.264 编码压缩，不拼绘界面、不替换日期或天气。天气来自手动选择的北京，日期来自设备自动时间。

## 重新录制

录制入口为 `app/src/androidTest/java/com/clockmods/ui/ReadmeCapture.java`。在隔离工作树中临时将 `defaultConfig.applicationId` 改为 `com.clockmods.readmemain`，避免覆盖正式安装；构建后恢复原值，不提交此临时改动。

```powershell
.\gradlew.bat --no-daemon '-Pclockmods.testRunner=com.clockmods.ui.ReadmeCapture' assembleProDebug assembleProDebugAndroidTest
adb -s emulator-5554 install -r -g app/build/outputs/apk/pro/debug/app-pro-debug.apk
adb -s emulator-5554 install -r app/build/outputs/apk/androidTest/pro/debug/app-pro-debug-androidTest.apk
adb -s emulator-5554 shell am instrument -w -e mode overview com.clockmods.readmemain.pro.test/com.clockmods.ui.ReadmeCapture
adb -s emulator-5554 shell am instrument -w -e mode chime-zh com.clockmods.readmemain.pro.test/com.clockmods.ui.ReadmeCapture
```

原始视频输出到设备 `/sdcard/com.clockmods.readmemain.pro-<mode>.mp4`；各页面截图输出到此隔离安装的外部文件目录 `readme/`。录制前后保存、恢复设备显示配置，完成后卸载隔离应用和测试包。天气配置只用于本地构建，不提交凭据。

README 使用 GitHub 上传附件提供嵌入播放器，同时保留仓库内 MP4 下载入口。
