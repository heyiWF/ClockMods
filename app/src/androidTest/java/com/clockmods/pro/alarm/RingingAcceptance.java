package com.clockmods.pro.alarm;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.KeyEvent;
import android.graphics.Rect;
import com.clockmods.R;
import com.clockmods.ultimate.settings.UltimateSettingsActivity;
import com.clockmods.pro.timer.*;
import java.io.FileOutputStream;

/** Isolated device checks; never clear data in the user's installed app. */
public final class RingingAcceptance extends Instrumentation {
    private Context context;
    private int checks;
    private String mode;
    @Override public void onCreate(Bundle args) { super.onCreate(args); mode = args.getString("mode", "logic"); start(); }
    private void check(boolean value, String message) { checks++; if (!value) throw new AssertionError(message); }
    private void await(long id) {
        long end = SystemClock.uptimeMillis() + 6000;
        while (new AlarmStore(context).ringingId() != id && SystemClock.uptimeMillis() < end) SystemClock.sleep(50);
        check(new AlarmStore(context).ringingId() == id, "active occurrence " + id);
    }
    private void command(long id, String action) { context.startService(AlarmDelivery.command(context, id, action)); }
    @Override public void onStart() {
        Bundle result = new Bundle(); context = getTargetContext(); getUiAutomation();
        try {
            check(context.getPackageName().contains("ringingaudit"), "isolated audit app required");
            runOnMainSync(() -> context.stopService(new Intent(context, AlarmRingingService.class)));
            SystemClock.sleep(150);
            AlarmStore clean = new AlarmStore(context);
            for (AlarmSpec old : clean.all()) { AlarmScheduler.cancel(context, old.id); clean.delete(old.id); }
            for (AlarmSpec old : clean.timerAlerts()) { AlarmScheduler.cancel(context, old.id); clean.delete(old.id); }
            clean.clearRings();
            if ("swipe".equals(mode)) {
                checkSwipeControls(clean);
                result.putString("stream", "PASS full-screen alarm swipe and button controls\n");
                finish(Activity.RESULT_OK, result);
                return;
            }
            if ("screen".equals(mode)) {
                new SequenceAcceptance(this).checkScreenOn();
                result.putString("stream", "PASS timing screen-on window checks\n");
                finish(Activity.RESULT_OK, result);
                return;
            }
            if ("sequence".equals(mode)) {
                new SequenceAcceptance(this).run();
                result.putString("stream", "PASS Pomodoro sequence editor and cycle checks\n");
                finish(Activity.RESULT_OK, result);
                return;
            }
            if ("confirmation".equals(mode)) { confirmSchedule(); return; }
            if ("editor".equals(mode)) { captureEditor(); return; }
            if (!"logic".equals(mode)) { scheduleLocked(); return; }
            AlarmStore store = new AlarmStore(context);
            AlarmPreferences.settings(context).edit().putBoolean(AlarmPreferences.RANDOM, true).commit();
            AlarmSpec first = new AlarmSpec(); first.enabled = false; first.ringtone = "silent"; first.vibrate = false;
            store.save(first); store.rememberRing(first.id);
            boolean swapped = AlarmPreferences.swapped(context, first.id);
            store.rememberRing(first.id);
            check(swapped == AlarmPreferences.swapped(context, first.id), "duplicate delivery keeps button order");
            Notification notice = AlarmDelivery.notification(context, first, 1);
            Context alertLocale = com.clockmods.LocaleManager.wrap(context);
            check(alertLocale.getString(com.clockmods.R.string.alarm_ringing).contentEquals(
                    notice.extras.getCharSequence(Notification.EXTRA_TITLE)), "unnamed alarm title");
            AlarmSpec named = new AlarmSpec(first);
            named.label = "Morning run";
            check(alertLocale.getString(com.clockmods.R.string.alarm_named_ringing, named.label).contentEquals(
                    AlarmDelivery.notification(context, named, 1).extras.getCharSequence(Notification.EXTRA_TITLE)),
                    "named alarm title includes its name and ringing status");
            check(notice.fullScreenIntent != null, "native full-screen pending intent attached");
            check(notice.visibility == Notification.VISIBILITY_PUBLIC, "lock-screen notification visibility");
            check(notice.actions.length == 2, "two accessible notification actions");
            check(notice.actions[0].title.toString().equals(com.clockmods.LocaleManager.wrap(context).getString(swapped ? com.clockmods.R.string.alarm_dismiss : com.clockmods.R.string.alarm_snooze)), "notification follows saved button order");
            runOnMainSync(() -> AlarmDelivery.ring(context, first)); await(first.id);
            AlarmRingingActivity screen = (AlarmRingingActivity) startActivitySync(new Intent(context, AlarmRingingActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            SystemClock.sleep(300);
            AlarmPreferences.settings(context).edit().putBoolean(AlarmPreferences.VOLUME, true).commit();
            runOnMainSync(() -> screen.onKeyDown(KeyEvent.KEYCODE_VOLUME_DOWN, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_DOWN)));
            await(0);
            check(store.find(first.id).snoozeAt > System.currentTimeMillis(), "volume key schedules snooze");
            checkSnoozeNotification(store, first.id);
            check(store.pendingRings().isEmpty(), "snooze removes active occurrence");
            AlarmPreferences.settings(context).edit().putBoolean(AlarmPreferences.POWER, true).commit();
            runOnMainSync(() -> AlarmDelivery.ring(context, store.find(first.id))); await(first.id);
            SystemClock.sleep(700);
            getUiAutomation().executeShellCommand("input keyevent 223").close();
            await(0);
            check(store.find(first.id).snoozeAt > System.currentTimeMillis(), "system sleep event snoozes while ringing");
            AlarmPreferences.settings(context).edit().putBoolean(AlarmPreferences.POWER, false).commit();
            getUiAutomation().executeShellCommand("input keyevent 224").close();
            AlarmScheduler.cancel(context, first.id);
            int originalCount = store.all().size();
            SharedPreferences timers = context.getSharedPreferences("pro_timers", 0);
            timers.edit().putLong("countdown_duration", 300000L).putBoolean("countdown_running", true).putLong("countdown_deadline", System.currentTimeMillis() - 1).commit();
            runOnMainSync(() -> { TimerReceiver.complete(context, "countdown"); TimerReceiver.complete(context, "countdown"); });
            SystemClock.sleep(700);
            long countdown = store.ringingId(); check(countdown < 0, "countdown rings through shared service");
            check(store.all().size() == originalCount && store.timerAlerts().size() == 1, "timer alert is isolated and completion is idempotent");
            timers.edit().putBoolean("pomodoro_running", true).putLong("pomodoro_deadline", System.currentTimeMillis() - 1).putInt("pomodoro_phase", 0).putLong("pomodoro_duration", 1500000L).commit();
            runOnMainSync(() -> { TimerReceiver.complete(context, "pomodoro"); TimerReceiver.complete(context, "pomodoro"); });
            SystemClock.sleep(400);
            check(store.pendingRings().size() == 2 && timers.getInt("pomodoro_phase", -1) == 1, "Pomodoro advances once and queues independently");
            Notification countdownNotice = notification(null, 100);
            Context localized = com.clockmods.LocaleManager.wrap(context);
            String stopTimer = localized.getString(com.clockmods.R.string.countdown_dismiss);
            String title = localized.getString(com.clockmods.R.string.countdown_finished_duration, "00:05:00");
            check(title.contentEquals(countdownNotice.extras.getCharSequence(Notification.EXTRA_TITLE)), "countdown notification title");
            check(localized.getString(com.clockmods.R.string.countdown_ringing_hint).contentEquals(
                    countdownNotice.extras.getCharSequence(Notification.EXTRA_TEXT)), "queued countdown notification body");
            check(localized.getString(com.clockmods.R.string.pro_page_countdown).contentEquals(
                    context.getSystemService(NotificationManager.class).getNotificationChannel(countdownNotice.getChannelId()).getName()),
                    "dedicated countdown channel");
            check(countdownNotice.actions.length == 1 && stopTimer.contentEquals(countdownNotice.actions[0].title),
                    "countdown notification only offers stop");
            AlarmRingingActivity countdownScreen = (AlarmRingingActivity) startActivitySync(
                    new Intent(context, AlarmRingingActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            awaitText("00:05:00");
            awaitText(stopTimer);
            check(findNode(getUiAutomation().getRootInActiveWindow(),
                    localized.getString(com.clockmods.R.string.alarm_snooze)) == null, "countdown screen has no snooze button");
            runOnMainSync(() -> countdownScreen.onKeyDown(KeyEvent.KEYCODE_VOLUME_DOWN,
                    new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_DOWN)));
            command(countdown, AlarmDelivery.SNOOZE);
            SystemClock.sleep(400);
            check(store.ringingId() == countdown && store.find(countdown).snoozeAt == 0,
                    "volume and stale snooze commands cannot postpone countdown");
            countdownNotice.actions[0].actionIntent.send();
            SystemClock.sleep(500);
            long pomodoro = store.ringingId(); check(pomodoro < 0 && pomodoro != countdown, "next timer becomes active");
            check(store.find(countdown) == null, "stopping countdown removes its alert");
            awaitText(localized.getString(com.clockmods.R.string.pomodoro_focus));
            awaitText("25:00");
            awaitText(localized.getString(com.clockmods.R.string.pomodoro_next));
            awaitText(localized.getString(com.clockmods.R.string.pomodoro_dismiss));
            Notification phaseNotice = notification(null, 100);
            check(phaseNotice.actions.length == 2 && localized.getString(com.clockmods.R.string.pomodoro_dismiss)
                    .contentEquals(phaseNotice.actions[0].title), "Pomodoro offers stop first");
            check(localized.getString(com.clockmods.R.string.pomodoro_ringing_hint).contentEquals(
                    phaseNotice.extras.getCharSequence(Notification.EXTRA_TEXT)), "Pomodoro notification body");
            command(pomodoro, AlarmDelivery.SNOOZE);
            SystemClock.sleep(250);
            check(store.ringingId() == pomodoro && store.find(pomodoro).snoozeAt == 0, "Pomodoro cannot be snoozed");
            check(localized.getString(com.clockmods.R.string.pomodoro_next).contentEquals(phaseNotice.actions[1].title), "next phase action");
            check(localized.getString(com.clockmods.R.string.pomodoro_finished_duration,
                    localized.getString(com.clockmods.R.string.pomodoro_focus), "25:00").contentEquals(
                    phaseNotice.extras.getCharSequence(Notification.EXTRA_TITLE)), "finished phase and duration");
            phaseNotice.actions[1].actionIntent.send(); await(0);
            check(timers.getBoolean("pomodoro_running", false) && timers.getInt("pomodoro_phase", -1) == 1,
                    "next starts the prepared phase");
            TimerScheduler.cancel(context, "pomodoro");
            timers.edit().putBoolean("pomodoro_running", false).commit();
            check(store.timerAlerts().isEmpty(), "dismiss removes temporary timer records");
            result.putString("stream", "PASS " + checks + " ringing checks\n"); finish(Activity.RESULT_OK, result);
        } catch (Throwable error) { result.putString("stream", android.util.Log.getStackTraceString(error)); finish(Activity.RESULT_CANCELED, result); }
    }
    private Rect bounds(String text) {
        Rect rect = new Rect();
        awaitText(text).getBoundsInScreen(rect);
        return rect;
    }

    private void toggleSwipeSetting(boolean expected) throws Exception {
        Activity settings = startActivitySync(UltimateSettingsActivity.createSubpageIntent(context, "chime")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        getUiAutomation().waitForIdle(450, 8000);
        Rect row = bounds(settings.getString(R.string.alarm_swipe_controls));
        shell("input tap " + row.centerX() + " " + row.centerY());
        waitForIdleSync();
        check(AlarmPreferences.enabled(context, AlarmPreferences.SWIPE) == expected, "settings switch saved");
        runOnMainSync(settings::finish);
    }

    private void checkSwipeControls(AlarmStore store) throws Exception {
        AlarmPreferences.settings(context).edit().remove(AlarmPreferences.SWIPE)
                .putBoolean(AlarmPreferences.RANDOM, false).commit();
        toggleSwipeSetting(true);
        for (int attempt = 0; attempt < 4; attempt++) {
            AlarmSpec alarm = new AlarmSpec();
            alarm.enabled = false;
            alarm.ringtone = "silent";
            alarm.vibrate = false;
            store.save(alarm);
            store.rememberRing(alarm.id);
            boolean swapped = attempt == 2;
            context.getSharedPreferences("pro_alarm", 0).edit().putBoolean("ring_swap_" + alarm.id, swapped).commit();
            runOnMainSync(() -> AlarmDelivery.ring(context, alarm));
            await(alarm.id);
            Activity screen = startActivitySync(new Intent(context, AlarmRingingActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            getUiAutomation().waitForIdle(450, 8000);
            Rect snooze = bounds(screen.getString(R.string.alarm_snooze));
            Rect stop = bounds(screen.getString(R.string.alarm_dismiss));
            if (attempt == 3) {
                shell("input tap " + stop.centerX() + " " + stop.centerY());
            } else {
                check((stop.centerX() < snooze.centerX()) == swapped, "targets follow the occurrence order");
                int center = (snooze.centerX() + stop.centerX()) / 2;
                int y = snooze.centerY();
                check(Math.abs(stop.centerY() - y) < 2, "icons share a centerline");
                shell("input tap " + center + " " + y);
                awaitText(screen.getString(R.string.alarm_swipe_hint));
                shell("input tap " + stop.centerX() + " " + y);
                shell("input swipe " + center + " " + y + " " + (center + (stop.centerX() - center) / 3) + " " + y + " 200");
                check(store.ringingId() == alarm.id, "a tap or partial swipe leaves the alarm ringing");
                int target = attempt == 0 ? snooze.centerX() : stop.centerX();
                shell("input swipe " + center + " " + y + " " + target + " " + y + " 250");
            }
            await(0);
            if (attempt < 3) {
                awaitText(attempt == 0 ? screen.getString(R.string.alarm_swipe_snoozed, alarm.snoozeMinutes)
                        : screen.getString(R.string.alarm_swipe_stopped));
                long end = SystemClock.uptimeMillis() + 4000;
                while (!screen.isFinishing() && SystemClock.uptimeMillis() < end) SystemClock.sleep(50);
                check(screen.isFinishing(), "confirmation closes after display");
            }
            check((store.find(alarm.id).snoozeAt > System.currentTimeMillis()) == (attempt == 0),
                    "control applied the chosen action");
            AlarmScheduler.cancel(context, alarm.id);
            if (attempt == 2) toggleSwipeSetting(false);
        }
    }

    private Notification notification(String tag, int id) {
        for (android.service.notification.StatusBarNotification item :
                context.getSystemService(NotificationManager.class).getActiveNotifications()) {
            if (item.getId() == id && java.util.Objects.equals(tag, item.getTag())) return item.getNotification();
        }
        return null;
    }
    private void checkSnoozeNotification(AlarmStore store, long id) throws Exception {
        Context localized = com.clockmods.LocaleManager.wrap(context);
        AlarmSpec alarm = store.find(id);
        String tag = Long.toString(id);
        Notification notice = notification(tag, 101);
        String when = android.text.format.DateUtils.formatDateTime(localized, alarm.snoozeAt,
                android.text.format.DateUtils.FORMAT_SHOW_DATE | android.text.format.DateUtils.FORMAT_SHOW_TIME);
        check(notice != null && (notice.flags & Notification.FLAG_ONGOING_EVENT) != 0, "ongoing snooze notification");
        check(localized.getString(com.clockmods.R.string.alarm_next_trigger, when).contentEquals(
                notice.extras.getCharSequence(Notification.EXTRA_TEXT)), "snooze notification shows the scheduled date and time");
        check(notice.actions.length == 1 && localized.getString(com.clockmods.R.string.alarm_dismiss)
                .contentEquals(notice.actions[0].title), "snooze notification has a stop action");
        AlarmSpec other = new AlarmSpec();
        other.enabled = false;
        other.snoozeAt = alarm.snoozeAt + 60000;
        store.save(other);
        AlarmScheduler.schedule(context, other);
        alarm.enabled = true;
        alarm.repeat = AlarmSpec.Repeat.WEEKLY;
        alarm.weekdays = 127;
        alarm.snoozeAt += 120000;
        AlarmScheduler.schedule(context, alarm);
        notice.actions[0].actionIntent.send();
        SystemClock.sleep(250);
        check(store.find(id).snoozeAt == alarm.snoozeAt, "old stop action cannot cancel a newer snooze");
        notification(tag, 101).actions[0].actionIntent.send();
        SystemClock.sleep(350);
        alarm = store.find(id);
        check(alarm.snoozeAt == 0 && alarm.enabled && alarm.scheduledAt > System.currentTimeMillis(),
                "stop cancels the snooze and retains the recurring alarm");
        check(notification(tag, 101) == null && notification(Long.toString(other.id), 101) != null,
                "stopping one snooze leaves the other notification");
        AlarmScheduler.cancel(context, other.id);
        store.delete(other.id);
        AlarmScheduler.cancel(context, id);
    }
    private void shell(String command) throws Exception {
        try (ParcelFileDescriptor descriptor = getUiAutomation().executeShellCommand(command);
             java.io.FileInputStream stream = new java.io.FileInputStream(descriptor.getFileDescriptor())) {
            byte[] buffer = new byte[1024]; while (stream.read(buffer) >= 0) { }
        }
    }
    private void confirmSchedule() throws Exception {
        AlarmSpec draft = new AlarmSpec();
        java.time.LocalDateTime target = java.time.LocalDateTime.now().plusDays(2).plusHours(22).plusMinutes(4);
        draft.onceDate = target.toLocalDate();
        draft.hour = target.getHour();
        draft.minute = target.getMinute();
        Activity editor = startActivitySync(new Intent(context, AlarmEditorActivity.class)
                .putExtra(AlarmUi.DRAFT, draft.toJson().toString()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        SystemClock.sleep(1200);
        String apply = editor.getString(com.clockmods.R.string.apply);
        android.view.accessibility.AccessibilityNodeInfo save = awaitText(apply);
        android.graphics.Rect bounds = new android.graphics.Rect();
        save.getBoundsInScreen(bounds);
        shell("input tap " + bounds.centerX() + " " + bounds.centerY());
        long end = SystemClock.uptimeMillis() + 5000;
        while (!editor.isFinishing() && SystemClock.uptimeMillis() < end) SystemClock.sleep(30);
        SystemClock.sleep(400);
        screenshot("alarm-confirmation");
        AlarmStore store = new AlarmStore(context);
        check(store.all().size() == 1 && store.all().get(0).scheduledAt > System.currentTimeMillis(), "editor saved and scheduled alarm");
        AlarmSpec alarm = store.all().get(0);
        Activity visible = startActivitySync(new Intent(context, AlarmEditorActivity.class)
                .putExtra(AlarmScheduler.EXTRA_ID, alarm.id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        awaitText(visible.getString(com.clockmods.R.string.apply));
        alarm.onceDate = target.toLocalDate();
        runOnMainSync(() -> {
            AlarmAccess.enabled(visible, alarm, false);
            AlarmAccess.enabled(visible, alarm, true);
        });
        SystemClock.sleep(900);
        screenshot("alarm-enabled");
        check(store.find(alarm.id).scheduledAt == alarm.scheduledAt, "switch confirmation uses the saved occurrence");
        runOnMainSync(visible::finish);
        AlarmScheduler.cancel(context, alarm.id);
        Bundle result = new Bundle();
        result.putString("stream", "Saved alarm and captured scheduling confirmation\n");
        finish(Activity.RESULT_OK, result);
    }
    private android.view.accessibility.AccessibilityNodeInfo findNode(android.view.accessibility.AccessibilityNodeInfo node, String text) {
        if (node == null) return null;
        if (node.isVisibleToUser() && ((node.getText() != null && text.contentEquals(node.getText()))
                || (node.getContentDescription() != null && text.contentEquals(node.getContentDescription())))) return node;
        for (int i = 0; i < node.getChildCount(); i++) {
            android.view.accessibility.AccessibilityNodeInfo found = findNode(node.getChild(i), text);
            if (found != null) return found;
        }
        return null;
    }
    private android.view.accessibility.AccessibilityNodeInfo awaitText(String text) {
        long end = SystemClock.uptimeMillis() + 8000;
        do {
            android.view.accessibility.AccessibilityNodeInfo root = getUiAutomation().getRootInActiveWindow();
            if (root != null) {
                android.view.accessibility.AccessibilityNodeInfo match = findNode(root, text);
                if (match != null) return match;
            }
            SystemClock.sleep(100);
        } while (SystemClock.uptimeMillis() < end);
        throw new AssertionError("Missing control: " + text);
    }
    private void captureEditor() throws Exception {
        context.getSharedPreferences("clock_prefs", 0).edit().putString("clock_language", "zh").commit();
        AlarmSpec draft = new AlarmSpec(); draft.repeat = AlarmSpec.Repeat.ALTERNATING;
        Activity activity = startActivitySync(new Intent(context, AlarmEditorActivity.class).putExtra(AlarmUi.DRAFT, draft.toJson().toString()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        SystemClock.sleep(1500); screenshot("alternating-editor");
        android.view.accessibility.AccessibilityNodeInfo choice = awaitText(activity.getString(com.clockmods.R.string.alarm_repeat));
        android.graphics.Rect rect = new android.graphics.Rect(); choice.getBoundsInScreen(rect);
        getUiAutomation().executeShellCommand("input tap " + rect.centerX() + " " + rect.centerY()).close();
        SystemClock.sleep(2500); screenshot("repeat-dialog");
        runOnMainSync(activity::finish);
        Bundle result = new Bundle(); result.putString("stream", "Captured alternating editor and repeat dialog\n"); finish(Activity.RESULT_OK, result);
    }
    private void screenshot(String name) throws Exception {
        java.io.File dir = new java.io.File(context.getExternalFilesDir(null), "ringing-audit"); dir.mkdirs();
        try (FileOutputStream out = new FileOutputStream(new java.io.File(dir, name + ".png"))) {
            getUiAutomation().takeScreenshot().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out);
        }
    }
    private void scheduleLocked() throws Exception {
        AlarmStore store = new AlarmStore(context);
        AlarmPreferences.settings(context).edit().putBoolean(AlarmPreferences.VOLUME, true).putBoolean(AlarmPreferences.POWER, false).commit();
        long when = System.currentTimeMillis() + 7000;
        if ("alarm".equals(mode)) {
            AlarmSpec alarm = new AlarmSpec(); alarm.enabled = false; alarm.snoozeAt = when; alarm.label = "Lock-screen alarm test"; store.save(alarm); AlarmScheduler.schedule(context, alarm);
        } else {
            String prefix = mode + "_";
            context.getSharedPreferences("pro_timers", 0).edit().putBoolean(prefix + "running", true).putLong(prefix + "deadline", when).commit();
            TimerScheduler.schedule(context, mode, when);
        }
        // Shell drives actual sleep outside this test; this does not directly launch the ringing UI.
        SystemClock.sleep(13000);
        check(store.ringingId() != 0, "scheduled " + mode + " delivered while asleep");
        android.view.accessibility.AccessibilityNodeInfo root = getUiAutomation().getRootInActiveWindow();
        int expectedAction = "countdown".equals(mode) ? com.clockmods.R.string.countdown_dismiss
                : "pomodoro".equals(mode) ? com.clockmods.R.string.pomodoro_dismiss : com.clockmods.R.string.alarm_snooze;
        boolean visible = findNode(root, com.clockmods.LocaleManager.wrap(context).getString(expectedAction)) != null;
        java.io.File dir = new java.io.File(context.getExternalFilesDir(null), "ringing-audit"); dir.mkdirs();
        try (FileOutputStream out = new FileOutputStream(new java.io.File(dir, mode + ".png"))) {
            getUiAutomation().takeScreenshot().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out);
        }
        Bundle result = new Bundle(); result.putString("stream", "Scheduled " + mode + ": ringing=true, fullScreenVisible=" + visible + ", keyguardLocked=" + context.getSystemService(KeyguardManager.class).isKeyguardLocked() + "\n");
        finish(Activity.RESULT_OK, result);
    }
}
