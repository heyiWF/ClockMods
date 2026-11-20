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

新增编辑、日期、轮班页面继承应用的系统预测性返回；页面内弹窗单独处理预览与取消。取消手势只恢复当前页面，不提交草稿。

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

## 大小周与响铃操作（2026-11）

重复选项使用较大文字、说明文字和至少 80 dp 的行高，内容可滚动。大小周以所选日期所在周为起始周，周一换周；可以选择从大周或小周开始。默认大周工作日为周一至周六、小周为周一至周五；两个工作日集合均可自由选择，包括周中休息。关闭“休息日闹钟”时在工作日提醒，开启时在其余日期提醒。起始周之前不按该规则提醒。按连续周数计算，跨月、跨年不会重置，也不使用容易歧义的单／双周编号。

法定节假日不会自动覆盖企业大小周；使用预定启用日期和暂时禁用日期处理调休、请假与临时加班，禁用日期始终优先。现有单次、星期、法定日和轮班规则保持独立。

“设置 → 闹钟与报时”提供三个默认关闭的开关：随机交换提醒按钮、音量键稍后提醒、电源键息屏时稍后提醒。每次响铃只抽取一次按钮顺序，页面重建和服务恢复保持不变，通知操作与全屏页一致。音量键只在全屏提醒页收到事件时生效，包括该页显示在锁屏上时；系统截获按键或未显示提醒页时不能保证生效。

Android 的电源键由系统处理，不交给普通应用。本应用只在响铃服务存活时监听公开的 `ACTION_SCREEN_OFF` 休眠事件实现可选的稍后提醒：自动休眠也可能触发；已经休眠时按电源键唤醒无法保证稍后提醒。这不是全局电源键拦截，也不使用无障碍、悬浮窗、设备管理员或隐藏接口。

倒计时和番茄钟到时后进入与闹钟相同的前台响铃队列、全屏提醒页和停止／稍后提醒操作，使用默认闹钟铃声与振动，稍后提醒间隔为 5 分钟。番茄钟阶段只前进一次；稍后提醒只是再次提示已完成阶段，不会启动或跳过下一阶段。临时提醒与用户闹钟分开存储，同时到时不会互相覆盖。服务首次通知即包含完整全屏入口，并要求立即展示，避免占位通知随后更新丢失全屏启动机会。提醒页使用独立任务，锁屏上不暴露设置页面。

## 原生接口与厂商系统边界

精确闹钟、通知及 Android 14+ 全屏提醒权限必须由用户允许，通知通道需保持高重要性。锁屏/熄屏时由系统决定启动全屏提醒；设备正在使用时 Android 通常显示横幅。拒绝权限不会被绕过。非精确、重启等场景若不允许启动媒体播放前台服务，则保留通知／全屏入口，由用户可见页面恢复响铃。

HyperOS/MIUI 与 ColorOS/OnePlus/realme 设备按厂商显示设置指引；所有入口优先使用公开的 Android 设置 Intent，入口不可用时回退到应用详情。小米官方资料列出额外的锁屏显示、后台弹窗等用户控制；不同型号与版本不能通过品牌字符串判定其授权状态。OPPO 的公开适配资料未提供可核实的第三方全屏闹钟或预测返回替代 API，因此不加入私有组件名、反射或强行启动 Activity 的兜底。

预测性返回保留原生系统与 AndroidX 的处理；不支持手势进度的 ROM 仍可正常返回，但应用不能凭空恢复系统未派发的进度、返回桌面或跨任务预览。不能以应用内部回调测试声称厂商系统侧滑已验证。

官方依据：

- [Android 全屏提醒权限与锁屏行为](https://source.android.com/docs/core/permissions/fsi-limits)
- [Android 通知全屏入口](https://developer.android.com/reference/android/app/Notification.Builder#setFullScreenIntent(android.app.PendingIntent,boolean))
- [Android 15+ PendingIntent 创建方后台启动授权](https://developer.android.com/guide/components/activities/secure-bal)
- [Android 电源键事件](https://developer.android.com/reference/android/view/KeyEvent#KEYCODE_POWER) 与 [休眠广播](https://developer.android.com/reference/android/content/Intent#ACTION_SCREEN_OFF)
- [小米 Android 14 适配：全屏提醒及预测返回](https://dev.mi.com/xiaomihyperos/documentation/detail?pId=1718)
- [小米额外应用权限说明](https://www.mi.com/my/support/faq/details/KA-497760/)
- [OPPO Android 15 适配说明：前台服务与系统限制](https://open.oppomobile.com/bbs/forum.php?mod=viewthread&tid=6274)

`RingingAcceptance` 使用独立 `ringingaudit` 包验证通知入口、每次响铃的按钮顺序、音量键、到时队列、重复广播和临时提醒清理。`-e mode alarm|countdown|pomodoro` 安排真实系统闹钟，不直接启动提醒页；配合 shell 息屏检查系统是否实际展示全屏。测试获得的无障碍树仅用于 instrumentation 验收，产品不声明无障碍服务。
