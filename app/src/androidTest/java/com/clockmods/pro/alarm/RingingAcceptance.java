package com.clockmods.pro.alarm;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.KeyEvent;
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
            check(notice.fullScreenIntent != null, "native full-screen pending intent attached");
            check(notice.visibility == Notification.VISIBILITY_PUBLIC, "lock-screen notification visibility");
            check(notice.actions.length == 2, "two accessible notification actions");
            check(notice.actions[0].title.toString().equals(context.getString(swapped ? com.clockmods.R.string.alarm_dismiss : com.clockmods.R.string.alarm_snooze)), "notification follows saved button order");
            runOnMainSync(() -> AlarmDelivery.ring(context, first)); await(first.id);
            AlarmRingingActivity screen = (AlarmRingingActivity) startActivitySync(new Intent(context, AlarmRingingActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            SystemClock.sleep(300);
            AlarmPreferences.settings(context).edit().putBoolean(AlarmPreferences.VOLUME, true).commit();
            runOnMainSync(() -> screen.onKeyDown(KeyEvent.KEYCODE_VOLUME_DOWN, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_DOWN)));
            await(0);
            check(store.find(first.id).snoozeAt > System.currentTimeMillis(), "volume key schedules snooze");
            AlarmScheduler.cancel(context, first.id);
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
            timers.edit().putBoolean("countdown_running", true).putLong("countdown_deadline", System.currentTimeMillis() - 1).commit();
            runOnMainSync(() -> { TimerReceiver.complete(context, "countdown"); TimerReceiver.complete(context, "countdown"); });
            SystemClock.sleep(700);
            long countdown = store.ringingId(); check(countdown < 0, "countdown rings through shared service");
            check(store.all().size() == originalCount && store.timerAlerts().size() == 1, "timer alert is isolated and completion is idempotent");
            timers.edit().putBoolean("pomodoro_running", true).putLong("pomodoro_deadline", System.currentTimeMillis() - 1).putInt("pomodoro_phase", 0).commit();
            runOnMainSync(() -> { TimerReceiver.complete(context, "pomodoro"); TimerReceiver.complete(context, "pomodoro"); });
            SystemClock.sleep(400);
            check(store.pendingRings().size() == 2 && timers.getInt("pomodoro_phase", -1) == 1, "Pomodoro advances once and queues independently");
            command(countdown, AlarmDelivery.SNOOZE); SystemClock.sleep(500);
            long pomodoro = store.ringingId(); check(pomodoro < 0 && pomodoro != countdown, "next timer becomes active");
            check(store.find(countdown).snoozeAt > System.currentTimeMillis(), "timer snooze persists separately");
            AlarmScheduler.cancel(context, countdown); store.delete(countdown);
            command(pomodoro, AlarmDelivery.STOP); await(0);
            check(store.timerAlerts().isEmpty(), "dismiss removes temporary timer records");
            result.putString("stream", "PASS " + checks + " ringing checks\n"); finish(Activity.RESULT_OK, result);
        } catch (Throwable error) { result.putString("stream", android.util.Log.getStackTraceString(error)); finish(Activity.RESULT_CANCELED, result); }
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
        boolean visible = findNode(root, com.clockmods.LocaleManager.wrap(context).getString(com.clockmods.R.string.alarm_snooze)) != null;
        java.io.File dir = new java.io.File(context.getExternalFilesDir(null), "ringing-audit"); dir.mkdirs();
        try (FileOutputStream out = new FileOutputStream(new java.io.File(dir, mode + ".png"))) {
            getUiAutomation().takeScreenshot().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out);
        }
        Bundle result = new Bundle(); result.putString("stream", "Scheduled " + mode + ": ringing=true, fullScreenVisible=" + visible + ", keyguardLocked=" + context.getSystemService(KeyguardManager.class).isKeyguardLocked() + "\n");
        finish(Activity.RESULT_OK, result);
    }
}
