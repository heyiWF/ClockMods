package com.clockmods.pro.alarm;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.clockmods.R;
import com.clockmods.ui.ClockTimeText;
import com.clockmods.ui.TemperatureText;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;

/** Run only under a disposable clockmods.auditApplicationId; never clears an installed user's data. */
public final class AlarmAcceptance extends Instrumentation {
    private int checks;
    private boolean toolsOnly, typographyOnly;
    private String captureLabel;
    private Context context;
    private void check(boolean value, String message) { if (!value) throw new AssertionError(message); checks++; }
    @Override public void onCreate(Bundle args) { super.onCreate(args); typographyOnly = args != null && "typography".equals(args.getString("mode")); toolsOnly = args != null && "tools".equals(args.getString("mode")); captureLabel = args == null ? "default" : args.getString("label", "default"); start(); }
    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
            context = getTargetContext();
            check(context.getPackageName().startsWith("com.clockmods.nov11"), "Use the isolated audit app");
            if (typographyOnly) {
            typography();
            AlarmEditorActivity sample = (AlarmEditorActivity) launch(AlarmEditorActivity.class, new Intent());
            runOnMainSync(() -> TemperatureSamples.show(sample));
            SystemClock.sleep(1200);
            screenshot("temperature-rendered");
            runOnMainSync(sample::finish);
                result.putString("stream", "PASS " + checks + " rendered typography checks\n");
                finish(Activity.RESULT_OK, result); return;
            }
            if (toolsOnly) { tools(); result.putString("stream", "PASS " + checks + " timer and stopwatch checks\n"); finish(Activity.RESULT_OK, result); return; }
            SharedPreferences prefs = context.getSharedPreferences("pro_alarm", 0);
            prefs.edit().clear().putInt("hour", 9).putInt("minute", 12).putBoolean("enabled", true).commit();
            AlarmStore store = new AlarmStore(context); AlarmSpec migrated = store.all().get(0);
            check(migrated.hour == 9 && migrated.minute == 12 && migrated.enabled && migrated.weekdays == 127, "daily alarm migration");
            check(new AlarmStore(context).all().size() == 1, "migration happens once");
            store.delete(migrated.id); check(new AlarmStore(context).all().isEmpty(), "deletion does not remigrate");
            AlarmSpec first = new AlarmSpec(); first.onceDate = LocalDate.now().plusDays(2); first.ringtone = "silent"; first.vibrate = false;
            AlarmSpec second = new AlarmSpec(first); second.minute = 35;
            store.save(first); store.save(second); check(first.id != second.id, "unique IDs");
            AlarmScheduler.schedule(context, first); AlarmScheduler.schedule(context, second);
            AlarmManager manager = context.getSystemService(AlarmManager.class);
            check(manager.getNextAlarmClock() != null && manager.getNextAlarmClock().getTriggerTime() == first.scheduledAt, "earliest independent alarm");
            AlarmAccess.enabled(context, first, false);
            check(manager.getNextAlarmClock().getTriggerTime() == second.scheduledAt, "disabling one preserves the next");
            check(store.find(second.id).enabled, "peer remains enabled");
            runOnMainSync(() -> new AlarmReceiver().onReceive(context, delivery(second.id, second.scheduledAt - 1)));
            SystemClock.sleep(250); check(!store.isRinging(), "stale broadcast ignored");
            AlarmEditorActivity editor = (AlarmEditorActivity) launch(AlarmEditorActivity.class, new Intent().putExtra(AlarmScheduler.EXTRA_ID, second.id));
            runOnMainSync(() -> { editor.draft.extraDates.add(LocalDate.now().plusDays(7)); editor.finish(); });
            SystemClock.sleep(250); check(store.find(second.id).extraDates.isEmpty(), "editor cancellation preserves saved data");
            AlarmSpec draft = new AlarmSpec(second); draft.cycleLength = 62; draft.cycleMinutes[61] = 1425;
            AlarmShiftActivity shift = (AlarmShiftActivity) launch(AlarmShiftActivity.class, new Intent().putExtra(AlarmUi.DRAFT, draft.toJson().toString()));
            check(shift.draft.cycleLength == 62 && shift.draft.cycleMinutes[61] == 1425, "62-day editor preserves final-day time");
            screenshot("shift"); runOnMainSync(shift::finish);
            AlarmDatesActivity dates = (AlarmDatesActivity) launch(AlarmDatesActivity.class, new Intent().putExtra(AlarmUi.DRAFT, draft.toJson().toString()).putExtra(AlarmUi.MODE, "extra"));
            clickAccessible(java.time.YearMonth.now().atDay(10).format(java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM).withLocale(context.getResources().getConfiguration().getLocales().get(0)))); clickAccessible(java.time.YearMonth.now().atDay(12).format(java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM).withLocale(context.getResources().getConfiguration().getLocales().get(0))));
            check(dates.draft.extraDates.size() == 2, "calendar multi-selection"); screenshot("dates");
            Bundle state = new Bundle(); runOnMainSync(() -> dates.onSaveInstanceState(state));
            AlarmSpec restored = AlarmSpec.fromJson(new org.json.JSONObject(state.getString(AlarmUi.DRAFT)));
            check(restored.extraDates.equals(dates.draft.extraDates), "dates survive state recreation");
            runOnMainSync(dates::finish);
            AlarmEditorActivity visible = (AlarmEditorActivity) launch(AlarmEditorActivity.class, new Intent().putExtra(AlarmScheduler.EXTRA_ID, second.id));
            screenshot("editor");
            clickAccessible(context.getString(R.string.alarm_choose_time));
            SystemClock.sleep(1200);
            screenshot("time-picker-" + captureLabel);
            sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);
            SystemClock.sleep(350);
            check(!visible.isFinishing(), "Back dismisses picker without closing editor");
            check(visible.draft.hour == second.hour && visible.draft.minute == second.minute, "picker cancellation preserves time");
            // Exercise actual service/notification/snooze delivery without waiting two days.
            first.enabled = true; first.scheduledAt = System.currentTimeMillis(); second.scheduledAt = first.scheduledAt;
            store.save(first); store.save(second);
            runOnMainSync(() -> { new AlarmReceiver().onReceive(context, delivery(first.id, first.scheduledAt));
                new AlarmReceiver().onReceive(context, delivery(second.id, second.scheduledAt)); });
            awaitRing(store, first.id); check(store.pendingRings().size() == 2, "simultaneous alarms persist independently");
            long fired = store.find(first.id).lastFiredAt;
            runOnMainSync(() -> new AlarmReceiver().onReceive(context, delivery(first.id, fired)));
            check(store.find(first.id).lastFiredAt == fired && store.pendingRings().size() == 2, "duplicate delivery ignored");
            context.startService(AlarmDelivery.command(context, first.id, AlarmDelivery.STOP));
            awaitRing(store, second.id); check(store.pendingRings().size() == 1, "dismiss advances the queue");
            AlarmRingingActivity ringing = (AlarmRingingActivity) launch(AlarmRingingActivity.class, new Intent()); screenshot("ringing");
            context.startService(AlarmDelivery.command(context, second.id, AlarmDelivery.SNOOZE));
            awaitRing(store, 0); check(store.find(second.id).snoozeAt > System.currentTimeMillis(), "snooze saved and scheduled");
            check(store.pendingRings().isEmpty(), "queue cleared after snooze");
            // Reopening a pending occurrence must wait for asynchronous service startup.
            context.stopService(new Intent(context, AlarmRingingService.class)); SystemClock.sleep(250);
            store.rememberRing(second.id); store.setRingingId(0);
            AlarmRingingActivity recovered = (AlarmRingingActivity) launch(AlarmRingingActivity.class, new Intent());
            awaitRing(store, second.id); check(!recovered.isFinishing(), "ringing UI survives service recovery");
            context.startService(AlarmDelivery.command(context, second.id, AlarmDelivery.STOP)); awaitRing(store, 0);
            runOnMainSync(recovered::finish);
            typography();
            runOnMainSync(() -> { visible.finish(); ringing.finish(); });
            AlarmScheduler.cancel(context); context.stopService(new Intent(context, AlarmRingingService.class));
            prefs.edit().clear().commit();
            result.putString("stream", "PASS " + checks + " alarm and typography checks\n"); finish(Activity.RESULT_OK, result);
        } catch (Throwable failure) {
            try { screenshot("failure-" + captureLabel); } catch (Exception ignored) { }
            result.putString("stream", android.util.Log.getStackTraceString(failure)); finish(Activity.RESULT_CANCELED, result);
        }
    }
    private Activity launch(Class<? extends Activity> type, Intent extras) {
        Intent intent = new Intent(context, type).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtras(extras);
        ActivityMonitor monitor = addMonitor(type.getName(), null, false);
        context.startActivity(intent);
        Activity result = waitForMonitorWithTimeout(monitor, 8000);
        removeMonitor(monitor);
        check(result != null, "activity starts: " + type.getSimpleName());
        SystemClock.sleep(500); return result;
    }
    private Intent delivery(long id, long trigger) { return new Intent(context, AlarmReceiver.class).putExtra(AlarmScheduler.EXTRA_ID, id).putExtra(AlarmScheduler.EXTRA_TRIGGER, trigger); }
    private void awaitRing(AlarmStore store, long id) {
        long deadline = SystemClock.uptimeMillis() + 5000;
        while (store.ringingId() != id && SystemClock.uptimeMillis() < deadline) SystemClock.sleep(50);
        check(store.ringingId() == id, "active ringing ID " + id);
    }
    private boolean clickText(View view, String text) {
        if (view instanceof TextView && text.contentEquals(((TextView) view).getText())) { view.performClick(); return true; }
        if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++)
            if (clickText(((ViewGroup) view).getChildAt(i), text)) return true;
        return false;
    }
    private void tools() throws Exception {
        context.getSharedPreferences("clockmods_onboarding", 0).edit().putBoolean("completed", true).commit();
        context.getSharedPreferences("clock_prefs", 0).edit().putInt("screen_orientation", 0).commit();
        context.getSharedPreferences("pro_stopwatch", 0).edit().clear().commit();
        context.getSharedPreferences("pro_timers", 0).edit().clear().commit();
        Activity main = mainActivity();
        clickAccessible(context.getString(R.string.pro_page_alarm));
        screenshot("alarm-list-" + captureLabel);
        clickAccessible(context.getString(R.string.pro_page_stopwatch));
        SystemClock.sleep(700);
        screenshot("stopwatch-empty-" + captureLabel);
        pressStart();
        SystemClock.sleep(1100);
        screenshot("stopwatch-running-" + captureLabel);
        check(context.getSharedPreferences("pro_stopwatch", 0).getBoolean("running", false), "visible stopwatch starts");
        clickAccessible(context.getString(R.string.stopwatch_lap));
        SystemClock.sleep(600);
        clickAccessible(context.getString(R.string.stopwatch_lap));
        clickAccessible(context.getString(R.string.timer_pause));
        SharedPreferences stopwatch = context.getSharedPreferences("pro_stopwatch", 0);
        long before = stopwatch.getLong("accumulated", 0);
        check(before >= 1700, "stopwatch advances while visible");
        check(stopwatch.getString("laps", "").split(",").length == 2, "two laps persist");
        screenshot("stopwatch-laps-" + captureLabel);
        clickAccessible(context.getString(R.string.timer_start));
        Activity first = main;
        runOnMainSync(first::finish);
        SystemClock.sleep(1000);
        main = mainActivity();
        clickAccessible(context.getString(R.string.pro_page_stopwatch));
        clickAccessible(context.getString(R.string.timer_pause));
        check(stopwatch.getLong("accumulated", 0) >= before + 800, "stopwatch includes time away from recreated page");
        check(stopwatch.getString("laps", "").split(",").length == 2, "laps survive activity recreation");
        clickAccessible(context.getString(R.string.pro_page_countdown));
        SystemClock.sleep(500);
        screenshot("countdown-idle-" + captureLabel);
        clickAccessible(context.getString(R.string.timer_start));
        SystemClock.sleep(500);
        clickAccessible(context.getString(R.string.timer_minutes, 5));
        SharedPreferences timers = context.getSharedPreferences("pro_timers", 0);
        check(!timers.getBoolean("countdown_running", true), "preset stops previous countdown");
        check(timers.getLong("countdown_remaining", 0) == 300000, "preset sets new duration");
        clickAccessible(context.getString(R.string.timer_start));
        SystemClock.sleep(500);
        clickAccessible(context.getString(R.string.timer_pause));
        screenshot("countdown-paused-" + captureLabel);
        check(!timers.getBoolean("countdown_running", true) && timers.getLong("countdown_remaining", 0) < 300000, "countdown pause retains remaining time");
        Activity last = main;
        runOnMainSync(last::finish);
        com.clockmods.pro.timer.TimerScheduler.cancel(context, "countdown");
    }
    private Activity mainActivity() {
        String name = "com.clockmods.ultimate.UltimateMainActivity";
        ActivityMonitor monitor = addMonitor(name, null, false);
        context.startActivity(new Intent().setClassName(context, name).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        Activity activity = waitForMonitorWithTimeout(monitor, 8000);
        removeMonitor(monitor);
        check(activity != null, "launcher starts");
        SystemClock.sleep(2500);
        int[] center = new int[2];
        runOnMainSync(() -> { center[0] = activity.getWindow().getDecorView().getWidth() / 2; center[1] = activity.getWindow().getDecorView().getHeight() / 3; });
        long down = SystemClock.uptimeMillis();
        android.view.MotionEvent press = android.view.MotionEvent.obtain(down, down, android.view.MotionEvent.ACTION_DOWN, center[0], center[1], 0);
        android.view.MotionEvent release = android.view.MotionEvent.obtain(down, down + 40, android.view.MotionEvent.ACTION_UP, center[0], center[1], 0);
        press.setSource(android.view.InputDevice.SOURCE_TOUCHSCREEN); release.setSource(android.view.InputDevice.SOURCE_TOUCHSCREEN);
        sendPointerSync(press); sendPointerSync(release); press.recycle(); release.recycle();
        SystemClock.sleep(600);
        return activity;
    }
    private void pressStart() throws Exception {
        getUiAutomation().clearCache();
        android.view.accessibility.AccessibilityNodeInfo button = findNode(getUiAutomation().getRootInActiveWindow(), context.getString(R.string.timer_start));
        check(button != null, "start button is visible");
        Rect bounds = new Rect(); button.getBoundsInScreen(bounds);
        long down = SystemClock.uptimeMillis();
        android.view.MotionEvent press = android.view.MotionEvent.obtain(down, down, android.view.MotionEvent.ACTION_DOWN, bounds.centerX(), bounds.centerY(), 0);
        press.setSource(android.view.InputDevice.SOURCE_TOUCHSCREEN); sendPointerSync(press); press.recycle();
        try { screenshot("stopwatch-pressed-" + captureLabel); }
        finally {
            android.view.MotionEvent release = android.view.MotionEvent.obtain(down, SystemClock.uptimeMillis(), android.view.MotionEvent.ACTION_UP, bounds.centerX(), bounds.centerY(), 0);
            release.setSource(android.view.InputDevice.SOURCE_TOUCHSCREEN); sendPointerSync(release); release.recycle();
        }
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
    private void clickAccessible(String text) {
        long deadline = SystemClock.uptimeMillis() + 5000;
        while (SystemClock.uptimeMillis() < deadline) {
            getUiAutomation().clearCache();
            android.view.accessibility.AccessibilityNodeInfo root = getUiAutomation().getRootInActiveWindow();
            if (clickNode(root, text)) { SystemClock.sleep(200); return; }
            SystemClock.sleep(100);
        }
        throw new AssertionError("Cannot click " + text);
    }
    private boolean clickNode(android.view.accessibility.AccessibilityNodeInfo node, String text) {
        if (node == null) return false;
        if ((node.getText() != null && text.equals(node.getText().toString().split("\n", 2)[0]))
                || (node.getContentDescription() != null && (text.contentEquals(node.getContentDescription()) || node.getContentDescription().toString().startsWith(text + ", ")))) {
            android.view.accessibility.AccessibilityNodeInfo target = node;
            while (target != null && !target.isClickable()) target = target.getParent();
            if (target != null && target.isVisibleToUser() && target.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)) return true;
        }
        for (int i = 0; i < node.getChildCount(); i++) if (clickNode(node.getChild(i), text)) return true;
        return false;
    }

    private void typography() {
        for (Typeface typeface : new Typeface[]{Typeface.DEFAULT, Typeface.SERIF, Typeface.MONOSPACE}) {
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG); p.setTypeface(typeface); p.setTextSize(100);
            Rect digit = new Rect(), colon = new Rect(); p.getTextBounds("0", 0, 1, digit); p.getTextBounds(":", 0, 1, colon);
            check(Math.abs(digit.exactCenterY() - colon.exactCenterY() - ClockTimeText.colonBaselineOffset(p)) < .01, "colon ink centre");
            Paint unit = new Paint(p); unit.setTextSize(100 * TemperatureText.unitScale(p, "℃"));
            Rect symbol = new Rect(); unit.getTextBounds("℃", 0, 1, symbol);
            check(Math.abs(symbol.height() - digit.height() / 2f) <= 1, "unit half-height");
            check(symbol.top + TemperatureText.topOffset(p, unit, "℃") == digit.top, "unit top edge");
        }
    }
    private void screenshot(String name) throws Exception {
        SystemClock.sleep(250); // Running clocks intentionally never become idle.
        Bitmap image = getUiAutomation().takeScreenshot();
        File directory = new File(context.getExternalFilesDir(null), "alarm-acceptance"); directory.mkdirs();
        try (FileOutputStream out = new FileOutputStream(new File(directory, name + ".png"))) { image.compress(Bitmap.CompressFormat.PNG, 100, out); }
        image.recycle();
    }
}
