# Timing Live Updates / 计时实时活动

Pomodoro, countdown, and stopwatch publish separate, silent ongoing notifications while running. Android owns the chronometer, so it continues updating without a per-second background service. On supported Android 16 / QPR systems these notifications request Live Updates promotion; the OS, notification permission, the app’s Live Updates setting, and manufacturer policy decide the actual presentation. Earlier systems retain standard ongoing notifications.

番茄钟、倒计时和秒表运行时，各自显示独立的静默常驻计时通知。秒数由 Android 系统更新，无需每秒唤醒后台服务。支持的 Android 16 / QPR 系统会收到 Live Updates 提升请求；实际显示取决于系统支持、通知权限、应用实时活动开关和厂商策略。旧系统显示普通常驻通知。

- Pause, reset, changing a duration, or skipping a Pomodoro phase removes that activity’s running notification. Resume creates a fresh time base. Tapping opens the corresponding tool.
- Timer expiry removes the running notification and posts the existing completion alert exactly once. Countdown notifications also time out at their deadline if an inexact alarm is delayed. Boot, app update, and time changes restore running timers and notifications.
- Stopwatch laps stay newest-first. Each insertion scrolls smoothly to the newest row, including after browsing older laps and during repeated taps. Existing rows keep their identity.

- 暂停、重置、更改时长或跳过番茄钟阶段会撤下对应的运行通知；继续时恢复正确计时。点击通知直接进入对应工具。
- 到期后撤下运行通知，并仅触发一次结束提醒。若非精确闹钟被系统延迟，倒计时运行通知仍会按时自动消失。重启、应用更新和时间变化后恢复运行中的计时及通知。
- 秒表最新记次位于顶部。浏览旧记录后新增、连续快速记次时，均平滑滚回最新行，保留已有行的身份和动画连续性。

## Device regression

Build a separate audit package and the `com.clockmods.pro.timer.TimingAcceptance` instrumentation runner using `-Pclockmods.auditApplicationId=com.clockmods.timingauditview` (or `com.clockmods.timingauditcompose`) and `-Pclockmods.testRunner=com.clockmods.pro.timer.TimingAcceptance`. Install both APKs, grant notification permission to the audit package, and run the instrumentation. The runner checks native notifications, simultaneous activities, time bases, expiry, completion idempotence, long-list insertion, repeated laps, and notification navigation. Never use the runner with the normal application ID.

On API 35, the platform cannot validate promoted system surfaces. On a supporting system, also check the status chip, notification shade, lock screen, permission denial, and the app’s Live Updates switch. The app does not override user notification choices.
