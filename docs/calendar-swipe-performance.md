# 月历翻页性能验证（2026-09-24）

分支：`ultimate-compose`。修改前基线：`0083d5b`。

## 原因与修改

- 原先在开始拖动时同步为相邻页面逐日计算农历、节日和宜忌，翻页完成后又为当前页面重复计算。现在由页面持有最多 192 个日期的 LRU 缓存，并在后台预热前后相邻月份（周历则预热相邻周）。当前页和预览页共用日期信息，预览页也保留日程标记。
- 缓存只复用日期信息，`currentMonth` 和 `today` 标记始终取自当前日期网格。日程修改和页面刷新会更换缓存。后台计算不持有缓存锁，避免缓存命中被后台计算阻塞。
- 拖动只在方向改变时更新页面组合，逐帧位移在 `graphicsLayer` 中读取；页面或尺寸改变时取消旧的归位动画。
- 农历标签在停留期间挂起，过渡位移和跑马灯位移只更新绘制层。文字未溢出时不启动跑马灯动画。
- 时钟状态延迟到仪表盘内部读取，走秒不再使整个月历页面重组。

## 验证方法与实测

Android 15 模拟器 `emulator-5554`，Debug APK，1440 × 2560，五种主题分别测试横竖屏。通过 `Window.OnFrameMetricsAvailableListener` 采集 `TOTAL_DURATION`，采样窗口覆盖一次向前翻页手势及其后 700 ms。两次采样均在构建完成后执行；天气关闭。

以下为每个场景单次采样，单位 ms，不是物理设备基准或持续帧率保证。修改后的测试还额外检查了反向翻页和短拖动回弹，这些操作在采样窗口之外。

| 主题 / 方向 | 修改前 P95 | 修改后 P95 | 修改前最长帧 | 修改后最长帧 | >32 ms 帧数，前→后 |
| --- | ---: | ---: | ---: | ---: | ---: |
| graphite / 竖屏 | 35.2 | 21.3 | 851.5 | 40.2 | 12→5 |
| graphite / 横屏 | 34.6 | 9.6 | 437.6 | 206.7 | 7→1 |
| carbon / 竖屏 | 13.5 | 9.7 | 372.9 | 14.3 | 1→0 |
| carbon / 横屏 | 13.8 | 11.3 | 407.8 | 20.0 | 3→0 |
| paper / 竖屏 | 20.3 | 9.9 | 305.5 | 11.7 | 5→0 |
| paper / 横屏 | 35.8 | 8.5 | 882.2 | 18.7 | 8→0 |
| poster / 竖屏 | 23.6 | 10.6 | 668.2 | 60.8 | 3→1 |
| poster / 横屏 | 9.6 | 7.7 | 287.8 | 60.0 | 2→1 |
| agenda / 竖屏 | 6.7 | 10.2 | 7.1 | 13.8 | 0→0 |
| agenda / 横屏 | 7.8 | 7.6 | 246.1 | 188.7 | 1→1 |

合计 >32 ms 的帧由 42 降到 9；采样帧数由 911 降到 704（动画调度减少，因此分母不同）。主要月历翻页停顿明显下降，但 graphite / agenda 横屏仍出现孤立长帧，不能据此声称所有场景均无卡顿。首次进入页面、跳转到未缓存月份或预热未完成时仍有同步加载的兜底路径。

## 回归与复现

- `testUltimateDebugUnitTest`：204 项通过，0 失败、0 错误、0 跳过。
- `assembleUltimateDebug`、`assembleUltimateDebugAndroidTest`、`lintUltimateDebug`：通过。
- 天气关闭：五种主题 × 横竖屏，共 10 个场景通过；覆盖日期数、日期选择、返回今天、双向翻页、短拖动回弹、月份选择器及截图。graphite 另检查时钟走秒及垂直居中。
- 天气开启：graphite、carbon、agenda × 横竖屏，共 6 个场景通过，额外验证缓存天气显示；该组仍观察到 carbon 竖屏 228.4 ms 和 agenda 横屏 191.7 ms 的孤立长帧。
- 人工检查 graphite、paper 竖屏截图，月历、农历标签和选中状态布局正常。
- 验收脚本在 `finally` 恢复原偏好、天气缓存和旋转控制。

```powershell
adb shell am instrument -w -e calendar true com.clockmods.ultimate.test/com.clockmods.widget.WidgetAcceptanceInstrumentation
adb shell am instrument -w -e calendar true -e weather true com.clockmods.ultimate.test/com.clockmods.widget.WidgetAcceptanceInstrumentation
```

本次原始采样和截图保存在 `app/build/reports/calendar-performance/`（构建产物，不纳入版本控制）。
