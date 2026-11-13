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

New editor, date and shift activities inherit system predictive Back. Dialogs handle their own preview and cancellation. No HyperOS-specific animation is applied. Cancelling a gesture restores the current surface without applying its draft.

## Validation

`AlarmPlanTest` covers date precedence, statutory workdays, shift boundaries, distant dates and daylight-saving transitions. Stopwatch time-base tests cover pause, recreation, clock adjustments and the reboot fallback.

Device acceptance uses a separate `clockmods.auditApplicationId`, preserving the user's installation. `AlarmAcceptance` checks migration, independent scheduling, duplicate broadcasts, concurrent ringing, snooze, date editing and font geometry. Run with `-e mode tools` to check timer and lap state, or `-e mode typography` to capture real font rendering separately from ringing notifications. Screenshots are stored in the test application's external `alarm-acceptance` directory. Record and restore resolution, density and rotation after each device session.

Application callback tests do not establish system edge-gesture acceptance. Verify predictive Back initiation, cancellation and completion on an Android device with gesture navigation enabled.

## References

- [AOSP DeskClock alarm model](https://android.googlesource.com/platform/packages/apps/DeskClock/+/6ac43d8368e8b09c4d683d6d993e1862b4f09464/src/com/android/deskclock/provider/Alarm.java)
- [Android AlarmManager](https://developer.android.com/reference/android/app/AlarmManager#setAlarmClock(android.app.AlarmManager.AlarmClockInfo,android.app.PendingIntent))
- [Android RingtoneManager](https://developer.android.com/reference/android/media/RingtoneManager)

Interactions reference Google Clock; implementation uses each branch's own UI architecture and the application's calendar data.
