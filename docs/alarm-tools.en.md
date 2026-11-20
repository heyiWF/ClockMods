# Alarms and timing tools

[简体中文](alarm-tools.md)

Applies to `ultimate` (Material Components) and `ultimate-compose` (Compose Material 3).

## Alarm rules

Each alarm stores its own time, label, ringtone, vibration and snooze interval. The previous single daily alarm migrates once on first access; deleting it does not repeat migration.

- Once: choose a date.
- Weekly: select any Monday–Sunday combination.
- Mainland China statutory holidays or workdays: use the bundled annual holiday adjustments, including working weekends. Data currently covers 2025–2027; unknown years are not guessed.
- Shift cycle: 2–62 days from an anchor date, with an independent time or rest day for each position. The cycle does not apply before its anchor.
- Additional enabled dates: supplement the repeat rule. An active shift day keeps its own time; a rest day uses the alarm's main time.
- Excluded dates: override repeat rules, additional dates and snooze delivery.

Dates support multiple selection. Applying a child page only updates the parent draft; applying the alarm editor saves and reschedules it. Back discards the current page's unapplied edits. Time selection opens the clock dial by default and offers manual numeric input. Each alarm uses Android's ringtone picker for the default alarm sound, silence or an individual sound.

Scheduling uses Android `AlarmManager.setAlarmClock` with independent PendingIntents. Exact alarm permission enables exact delivery; without it, the app retains an inexact schedule and provides a permission entry. Reboot, time and time-zone changes trigger recalculation. Local calendar and zone rules handle day boundaries rather than fixed 24-hour offsets.

Simultaneous alarms queue independently and can be dismissed or snoozed separately. A foreground service owns sound and vibration; a silent notification channel avoids duplicate playback. The ringing queue persists, with a ten-minute maximum per occurrence. Full-screen presentation depends on system permission; notifications and the alarm page provide controls.

## Layout and motion

The layout follows available dp width and height, including spacious high-resolution, low-density windows and compact windows. Portrait stacks content; wide landscape uses two panes with a maximum content width. Stopwatch records show lap and total times, preserving records and elapsed time across activity recreation.

Material buttons tighten their corners while pressed and return to pills on release. Running timers use a yellow Pause button with dark text. Starting the stopwatch expands its secondary controls, and new lap rows slide in. Countdown digits roll only when changed, while separators retain font-measured alignment. Disabling system animations should apply final states immediately.

New editor, date and shift activities inherit system predictive Back. Dialogs handle their own preview and cancellation. When native cross-activity previews are unavailable, enable the compatible swipe preview in System settings. Cancelling a gesture restores the current surface without applying its draft.

## Validation

`AlarmPlanTest` covers date precedence, statutory workdays, shift boundaries, distant dates and daylight-saving transitions. Stopwatch time-base tests cover pause, recreation, clock adjustments and the reboot fallback.

Device acceptance uses a separate `clockmods.auditApplicationId`, preserving the user's installation. `AlarmAcceptance` checks migration, independent scheduling, duplicate broadcasts, concurrent ringing, snooze, date editing and font geometry. Run with `-e mode tools` to check timer and lap state, or `-e mode typography` to capture real font rendering separately from ringing notifications. Screenshots are stored in the test application's external `alarm-acceptance` directory. Record and restore resolution, density and rotation after each device session.

Application callback tests do not establish system edge-gesture acceptance. Verify predictive Back initiation, cancellation and completion on an Android device with gesture navigation enabled.

## References

- [AOSP DeskClock alarm model](https://android.googlesource.com/platform/packages/apps/DeskClock/+/6ac43d8368e8b09c4d683d6d993e1862b4f09464/src/com/android/deskclock/provider/Alarm.java)
- [Android AlarmManager](https://developer.android.com/reference/android/app/AlarmManager#setAlarmClock(android.app.AlarmManager.AlarmClockInfo,android.app.PendingIntent))
- [Android RingtoneManager](https://developer.android.com/reference/android/media/RingtoneManager)

Interactions reference Google Clock; implementation uses each branch's own UI architecture and the application's calendar data.

## Alternating weeks and alert controls (2026-11)

Repeat choices have larger text, descriptions, scrollable content and a minimum 80 dp row height. Alternating weeks start on the Monday of the selected anchor week, with a choice of long or short week first. Defaults are Monday–Saturday and Monday–Friday workdays; customize either set, including midweek rest days. Choose workday or complementary rest-day alarms. The recurrence does not apply before the anchor week, and continuous week arithmetic does not reset at month/year boundaries. Public holidays do not override this company schedule; use extra/excluded dates for adjustments, leave and overtime. Exclusions win.

Settings → Alarms and chimes provides three opt-in controls: shuffle alert buttons, volume-key snooze and snooze when the power key turns the screen off. The random order is persisted once per occurrence, survives recreation/recovery and matches notification actions. Volume keys work when delivered to the full-screen activity, including over keyguard; the system may intercept them.

Android never delivers the power key to ordinary apps. During ringing only, the service can observe the public ACTION_SCREEN_OFF sleep broadcast. Automatic sleep may also snooze; pressing power while already asleep is not guaranteed to snooze. No global key interception, accessibility service, overlay, device administrator or hidden API is used.

Countdown and Pomodoro completion share the alarm foreground service, queue, full-screen activity and snooze/stop controls, with default alarm sound, vibration and a five-minute snooze. Pomodoro advances phase once; snoozing only repeats the completion alert. Temporary alerts are stored separately from user alarms. The first foreground notification includes its full-screen intent and requests immediate display; a placeholder update must not consume the opportunity. A separate ringing task prevents lock-screen access to settings behind it.

## Platform and manufacturer limits

Exact alarm access, notifications, Android 14+ full-screen access and a high-importance notification channel remain user-controlled. Android decides full-screen delivery while locked/asleep and usually shows a banner while the device is in use. Restricted inexact/boot delivery falls back to the notification/full-screen entry instead of bypassing foreground-service restrictions.

HyperOS/MIUI and ColorOS/OnePlus/realme get conditional guidance. Settings links use public Android Intents with an app-details fallback. Xiaomi documents additional lock-screen and background-pop-up controls; their state cannot be inferred from the brand. No verified public OPPO replacement API for third-party alarm full-screen delivery or predictive back was found. Private components, reflection and forced Activity launches are not used.

Native/AndroidX predictive Back remains primary. Ordinary Back works without gesture progress, but apps cannot reconstruct system-withheld progress, home or cross-task previews. Callback tests do not establish ROM gesture support.

Sources: [AOSP full-screen limits](https://source.android.com/docs/core/permissions/fsi-limits), [notification full-screen intents](https://developer.android.com/reference/android/app/Notification.Builder#setFullScreenIntent(android.app.PendingIntent,boolean)), [PendingIntent background-launch delegation](https://developer.android.com/guide/components/activities/secure-bal), [power key](https://developer.android.com/reference/android/view/KeyEvent#KEYCODE_POWER), [sleep broadcast](https://developer.android.com/reference/android/content/Intent#ACTION_SCREEN_OFF), [Xiaomi Android 14 guidance](https://dev.mi.com/xiaomihyperos/documentation/detail?pId=1718), [Xiaomi extra permissions](https://www.mi.com/my/support/faq/details/KA-497760/), [OPPO Android 15 guidance](https://open.oppomobile.com/bbs/forum.php?mod=viewthread&tid=6274).

RingingAcceptance uses a separate ringingaudit package to check full-screen notification construction, stable button order, volume snooze, timer queues, duplicate delivery and temporary-record cleanup. Modes alarm/countdown/pomodoro schedule real system delivery without directly launching the alert activity; use shell sleep to observe full-screen behavior. Instrumentation accessibility inspection is test-only; the product declares no accessibility service.
