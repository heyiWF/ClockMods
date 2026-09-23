# Ultimate 月历主题迁移

参考版本：`ultimate` 分支的 `1abc6b8`。目标：`ultimate-compose`。

Compose 原有替代布局已移除，主题 ID、顺序、名称和配色恢复为参考版本：

| ID | 主题 | 布局 |
| --- | --- | --- |
| `calendar.graphite` | 石墨仪表盘 | 时钟、天气和三日预报；竖屏上方读数占 46%，下方月历占 54%；横屏左右各半 |
| `calendar.carbon` | 纯黑碳素 | 与石墨仪表盘相同布局，使用纯黑配色 |
| `calendar.paper` | 宣纸水墨 | 整屏月历、红色今日光晕、农历和宜忌页脚 |
| `calendar.poster` | 墨白排版 | 大月份与年份、无边框数字月历、今日标记和选中短线 |
| `calendar.agenda` | 靛蓝周程 | 七天周程与当日详情；竖屏上方横条，横屏左侧竖列；日程和天气 |

`CalendarDashboardSizing` 保留参考版的尺寸计算。Compose 重建了日期数字、班休标记、今日光晕、月份字标、农历轮播和固定宜忌前缀的滚动页脚。天气关闭时仪表盘读数区域只显示时钟。主题预览和三种语言的主题说明也随之更新。

## 验证

- 构建：`assembleUltimateDebug`、`assembleUltimateDebugAndroidTest`。
- 单元测试：`testUltimateDebugUnitTest`，199 项通过；包含主题目录与配色约束、原版响应式尺寸测试。
- ADB：`emulator-5554`，PHY110 / pangu，Android 15（API 35），1440 × 2560，density 360。
- 五套主题分别检查横屏和竖屏：真实 Activity 启动、42 格月历或 7 格周程、日期选择、跨月日期、返回今天、触摸滑动翻页、月份选择器，以及截图对照。
- 天气关闭的 10 个场景、天气开启的 6 个场景全部通过。天气测试使用固定缓存和手动城市，验证当前温度与三日预报，不依赖定位权限或外部接口可用性。测试结束恢复原偏好与缓存。

设备测试命令：

```text
adb shell am instrument -w -e calendar true com.clockmods.ultimate.test/com.clockmods.widget.WidgetAcceptanceInstrumentation
adb shell am instrument -w -e calendar true -e weather true com.clockmods.ultimate.test/com.clockmods.widget.WidgetAcceptanceInstrumentation
```

截图与检查结果写入应用外部文件目录的 `calendar-acceptance/`。参考版与新版截图已拉取至本地忽略目录 `.tmp-calendar-validation/`，用于人工核对布局、配色、文字、标记与裁切。

完整 Android Lint 未完成：运行停留在 `BidirectionalTextDetector` 的 Kotlin UAST 注释遍历中，线程栈显示 `KotlinUFile.getAllCommentsInFile` / `PsiWalkingState.next`。未通过修改项目检查配置来跳过该问题；上述构建、单元测试和设备验收独立执行。

## 对齐修复复验

- 两个分支的仪表盘在横屏关闭天气时改为垂直居中；天气开启时保留原来的底部对齐。
- Compose 的静态农历与轮播文字共用相同行高和居中的容器，避免轮播与非轮播日期的文字出现高度差。
- 两个分支均构建成功，并在同一 ADB 设备核对横竖屏截图。Compose 的 16 个设备场景与 199 项单元测试再次通过。
- 连续截图确认轮播停留在农历和节日文字时，与同一行的静态文字基线一致；截图保存在本地忽略目录 `.tmp-center-validation/`。
