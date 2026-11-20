<div align="center">

<img src="docs/media/app-icon-playstore.png" alt="ClockMods" width="120" />

# ClockMods Nano

**让闲置手机，再次成为每天用得上的时钟。**

全屏常亮 · 横竖屏适配 · 离线农历 · 个性化外观

</div>

ClockMods Nano 是一款可定制的 Android 全屏时钟，最低支持到 Android 1.0（API 1）。把手机放在书桌、床头或客厅，时间、公历和农历便能清楚地呈现在眼前，让旧设备继续陪伴日常生活。

## 远一点，也能看清时间

全屏大字搭配屏幕常亮，横放、竖放都能使用。你可以隐藏秒数，让画面更安静；也可以选择竖排时、分、秒，把屏幕变成一块醒目的数字时钟。

12 / 24 小时制、字号、颜色和字体都能按习惯调整。日期与时间可分别设置，既适合近处阅读，也适合放在房间另一侧。

## 从白天到夜晚，调成喜欢的样子

选一张本机照片作为背景，或者用简单的纯色衬托时间。立即压暗和定时压暗让显示亮度随作息变化，跨过午夜也能按设定运行。

## 日期、地区与一句想看见的话

公历、星期和中国农历可离线查看。你可以放上一句自定义留言，文字过长时会自动滚动展示。网络校时可以按需开启，地区时区可自由选择；基础时钟和离线日期不依赖网络。

状态栏可显示网络信号与电量图标，开机可设为默认桌面自动进入时钟。设置面板随横竖屏自动适配。

## 开始使用

1. 下载并安装 `app-debug-api1-signed.apk`（Android 1.0 / API 1 及以上）。
2. **双击时钟区域**打开设置（Android 1.0 和 1.1 上单击），调整样式和功能，点击“应用”保存。
3. 将设备横放或竖放，按需要开启常亮、压暗和自定义留言。

## 下载体验

[![Build APK](https://github.com/heyiWF/ClockMods/actions/workflows/build-apk.yml/badge.svg?branch=nano)](https://github.com/heyiWF/ClockMods/actions/workflows/build-apk.yml?query=branch%3Anano)

在成功构建的 **Artifacts** 中下载并解压 APK。[Nano 构建列表](https://github.com/heyiWF/ClockMods/actions/workflows/build-apk.yml?query=branch%3Anano)

Nano 使用独立的包名 `com.clockmods.nano`，可与现有 ClockMods 版本并存。

## 从源码构建

准备 JDK 8、Python 3.8 及以上、Android SDK Platform 28 和 Build Tools 28.0.3。将 `JAVA_HOME` 指向 JDK 8，将 `ANDROID_HOME` 指向 Android SDK。

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
python3 tools/api1/package_api1.py --build-tools "$ANDROID_HOME/build-tools/28.0.3"
```

在 Windows 上可用 `gradlew.bat` 和 `python` 执行同样的命令，PowerShell 中 SDK 环境变量写作 `$env:ANDROID_HOME`。如果使用自定义调试密钥目录，可用 `--keystore` 指定 Gradle 的调试密钥。

最终 APK 位于 `app/build/outputs/apk/debug/app-debug-api1-signed.apk`。打包脚本将应用资源的字符串池转换为 Android 1.0 可读取的 UTF-16，并重新对齐、签名和验证 APK。

如需检查应用对 Android 框架成员的引用，用 `./gradlew compileReleaseJavaWithJavac` 编译后，执行 `python3 tools/api1/api1_check.py /path/to/android-1.0/android.jar`。

## 本地使用，按需联网

设置与选取的背景图片保存在设备上，打开应用即可使用和调整。

应用不申请任何位置权限，也不请求天气服务；仅网络校时会按需连接 NTP 服务。

## 更多

- [反馈问题与建议](https://github.com/heyiWF/ClockMods/issues)

## 许可与致谢

项目采用 [MIT 许可证](LICENSE)。农历引擎 [tyme4j](https://github.com/6tail/tyme4j) 采用 MIT。字体及其他资源以各自随附许可为准。
