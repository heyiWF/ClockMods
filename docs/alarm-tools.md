# 闹钟与计时工具

[English](alarm-tools.en.md)

适用于 `ultimate`（Material Components）和 `ultimate-compose`（Compose Material 3）。

## 闹钟规则

每个闹钟独立保存时间、名称、铃声、振动和稍后提醒间隔。旧版单个每日闹钟在首次读取时迁移，删除后不会再次迁移。

- 单次：指定响铃日期。
- 每周：自由选择周一至周日。
- 中国大陆法定节假日、法定工作日：使用应用内年度调休数据；调休上班的周末属于工作日。当前数据覆盖 2025–2027 年，未提供的年份不会猜测。
- 轮班：2–62 天一个周期，指定开始日，每天可设置独立时间或休息。开始日前不会按轮班规则响铃。
- 预定启用日期：在上述规则之外补充响铃日；轮班已有时间的当天保留该时间，休息日采用闹钟主时间。
- 暂时禁用日期：优先于重复规则、补充日期和稍后提醒。

日期支持多选。子页面点击“应用”只返回编辑草稿；闹钟编辑页点击“应用”才保存并重新排程。直接返回会放弃当前页的未应用改动。时间选择器默认表盘，可手动切换数字输入。铃声使用 Android 系统选择器，每个闹钟可以独立选择默认、静音或具体铃声。

使用 Android `AlarmManager.setAlarmClock` 与独立 PendingIntent；获得精确闹钟权限后精确调度。缺少精确权限时保留非精确排程，并在界面提供权限入口。重启、时间或时区改变后重新计算。日期计算使用本地日历与时区规则，避免用固定 24 小时模拟跨天。

同时响铃的闹钟分别排队、停止或稍后提醒。铃声和振动由前台服务管理，通知通道保持静音以免重复播放。响铃队列持久化；每次响铃最多持续 10 分钟。全屏显示依赖系统权限，通知与页面均提供处理入口。

## 计时界面与动效

界面依据实际可用 dp 宽高调整，兼容高分辨率、低密度的宽敞空间以及紧凑窗口。竖屏上下布局，宽横屏两栏，内容宽度设上限。秒表计次显示分段与累计时间，页面重建后保留记录和运行时间。

Material 主按钮按压时收紧圆角，释放后恢复药丸形；运行时“暂停”使用黄色与深色文字。秒表开始时展开辅助按钮，新增计次条目滑入。倒计时只滚动发生变化的数位，冒号保持与实际字体的数字居中。系统关闭动画时应直接到达最终状态。

新增编辑、日期、轮班页面继承应用的系统预测性返回；页面内弹窗单独处理预览与取消。没有 HyperOS 专用返回动画。取消手势只恢复当前页面，不提交草稿。

## 验证

`AlarmPlanTest` 覆盖多日期优先级、法定工作日、轮班边界、跨年与夏令时等排程；秒表时间基准测试覆盖暂停、页面重建、时钟变化与重启回退。

设备验收使用独立 `clockmods.auditApplicationId`，避免覆盖用户应用。`AlarmAcceptance` 验证迁移、多闹钟调度、重复广播、同时响铃、稍后提醒、日期编辑以及字体几何；`-e mode tools` 验证计时与计次状态。截图在测试包的外部文件目录 `alarm-acceptance` 中。每次设备验收记录并恢复分辨率、密度和旋转设置。

应用回调测试不能代替系统侧滑手势验收。系统预测性返回动画仍需在启用了手势导航的 Android 设备上检查开始、取消和提交全过程。

## 参考

- [AOSP DeskClock Alarm 模型](https://android.googlesource.com/platform/packages/apps/DeskClock/+/6ac43d8368e8b09c4d683d6d993e1862b4f09464/src/com/android/deskclock/provider/Alarm.java)
- [Android AlarmManager](https://developer.android.com/reference/android/app/AlarmManager#setAlarmClock(android.app.AlarmManager.AlarmClockInfo,android.app.PendingIntent))
- [Android RingtoneManager](https://developer.android.com/reference/android/media/RingtoneManager)

交互参考 Google Clock，代码使用各分支自身的 UI 架构与应用内日历数据实现。

使用 `-e mode typography` 可单独检查三种字体的实际温度排版，避免响铃通知遮挡截图。
