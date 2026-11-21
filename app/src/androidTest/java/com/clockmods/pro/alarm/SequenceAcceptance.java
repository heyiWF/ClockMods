package com.clockmods.pro.alarm;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityNodeInfo;

import com.clockmods.pro.timer.PomodoroSequence;
import com.clockmods.pro.timer.TimerCompletion;
import com.clockmods.pro.timer.TimerReceiver;
import com.clockmods.pro.timer.TimerScheduler;
import com.clockmods.pro.timer.TimingLiveUpdates;
import com.clockmods.ultimate.UltimateMainActivity;
import java.util.ArrayList;
import java.util.List;

final class SequenceAcceptance {
    private final Instrumentation test;
    private final Context context;
    private final SharedPreferences prefs;

    SequenceAcceptance(Instrumentation test) {
        this.test = test;
        context = test.getTargetContext();
        prefs = context.getSharedPreferences("pro_timers", 0);
    }

    void run() throws Exception {
        context.getSharedPreferences("clockmods_onboarding", 0).edit().putBoolean("completed", true).commit();
        context.getSharedPreferences("clock_prefs", 0).edit().putString("clock_language", "en").commit();
        prefs.edit().clear().putInt("pomodoro_phase", 2).commit();
        check(PomodoroSequence.index(prefs, PomodoroSequence.load(prefs)) == 3, "legacy long break migrates to the fourth slot");
        prefs.edit().putInt("pomodoro_phase", 0).putLong("pomodoro_duration", 1500000)
                .putLong("pomodoro_remaining", 1500000).commit();
        context.startActivity(new Intent(context, UltimateMainActivity.class)
                .putExtra(TimingLiveUpdates.EXTRA_DESTINATION, "pomodoro")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        try {
            click("Edit cycle", 0);
            List<AccessibilityNodeInfo> handles = await("Drag to reorder", 2);
            Rect from = bounds(handles.get(0)), to = bounds(handles.get(1));
            long down = SystemClock.uptimeMillis();
            int x = from.centerX(), y = from.centerY(), distance = to.centerY() - y;
            touch(down, x, y, android.view.MotionEvent.ACTION_DOWN);
            try {
                for (int i = 1; i <= 10; i++) {
                    touch(down, x, y + distance * i / 10, android.view.MotionEvent.ACTION_MOVE);
                    SystemClock.sleep(20);
                }
                SystemClock.sleep(260);
                check(bounds(await("5 min", 1).get(0)).centerY() < y + distance / 2,
                        "the second capsule makes room before the finger lifts");
            } finally {
                touch(down, x, y + distance, android.view.MotionEvent.ACTION_UP);
            }
            SystemClock.sleep(250);
            click("Apply", 0);
            check(saved().equals("1:5,0:25,0:25,2:15"), "drag swaps the first two phases: " + saved());
            await("Start", 1);
            check(!prefs.getBoolean("pomodoro_running", true) && prefs.getInt("pomodoro_phase", -1) == 0
                    && prefs.getLong("pomodoro_remaining", 0) == 300000L && prefs.getLong("pomodoro_deadline", -1) == 0,
                    "apply leaves the full first phase ready to start");
            click("Start", 0);
            await("Pause", 1);
            click("Reset", 0);
            check(!prefs.getBoolean("pomodoro_running", true) && prefs.getLong("pomodoro_remaining", 0) == 300000L
                    && saved().startsWith("1:5,"), "timer reset keeps the sequence");
            click("Edit cycle", 0);
            click("Short break", 0);
            chooseMenuItem("Focus");
            awaitGone("Short break");
            await("25 min", 3);
            click("Focus", 0);
            chooseMenuItem("Short break");
            await("5 min", 1);
            click("Short break", 0);
            chooseMenuItem("Long break");
            awaitGone("Short break");
            await("15 min", 2);
            click("15 min", 0);
            AccessibilityNodeInfo wheel = await("android.widget.NumberPicker", 1).get(0);
            check(wheel.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD), "scroll the duration wheel");
            SystemClock.sleep(450);
            click("Apply", 0);
            awaitGone("android.widget.NumberPicker");
            await("Drag to reorder", 1);
            click("Apply", 0);
            awaitGone("Drag to reorder");
            PomodoroSequence.Step changed = PomodoroSequence.load(prefs).get(0);
            check(changed.label == 2 && changed.minutes == 16, "phase choice and wheel change the first step: " + saved());
            click("Start", 0);
            await("Pause", 1);
            click("Edit cycle", 0);
            click("Add phase", 0);
            click("Remove phase", 0);
            click("Apply", 0);
            awaitGone("Drag to reorder");
            List<PomodoroSequence.Step> edited = PomodoroSequence.load(prefs);
            check(edited.size() == 4, "add and remove preserve the expected count");
            await("Start", 1);
            check(!prefs.getBoolean("pomodoro_running", true) && prefs.getLong("pomodoro_deadline", -1) == 0
                    && prefs.getLong("pomodoro_remaining", 0) == edited.get(0).duration(),
                    "applying during a running session cancels its deadline and resets the duration");
            click("Edit cycle", 0);
            for (int i = 4; i < 15; i++) activate("Add phase");
            check(!await("Add phase", 1).get(0).isEnabled(), "maximum of 15 steps");
            for (int i = 15; i > 2; i--) activate("Remove phase");
            check(!await("Remove phase", 1).get(0).isEnabled(), "minimum of two steps");
            click("Reset", 0);
            click("Cancel", 0);
            awaitGone("Drag to reorder");
            check(saved().equals(PomodoroSequence.encode(edited)), "cancel discards reset draft: " + saved());
            click("Edit cycle", 0);
            click("Reset", 0);
            click("Apply", 0);
            awaitGone("Drag to reorder");
            check(saved().equals(PomodoroSequence.DEFAULT), "applying reset restores default sequence");
        } finally {
            shell("input keyevent KEYCODE_HOME");
        }
        // Two identical labels at opposite duration limits exercise index-based transitions.
        test.runOnMainSync(() -> PomodoroSequence.apply(context, PomodoroSequence.decode("2:3,2:60")));
        openTool("pomodoro");
        click("Start", 0);
        await("Pause", 1);
        shell("input keyevent KEYCODE_HOME");
        AlarmStore alarms = new AlarmStore(context);
        for (int i = 0; i < 2; i++) {
            long deadline = System.currentTimeMillis() - 10;
            prefs.edit().putLong("pomodoro_deadline", deadline).commit();
            test.runOnMainSync(() -> TimerReceiver.complete(context, "pomodoro"));
            long end = SystemClock.uptimeMillis() + 5000;
            while (alarms.ringingId() == 0 && SystemClock.uptimeMillis() < end) SystemClock.sleep(30);
            long id = alarms.ringingId();
            AlarmSpec alert = alarms.find(id);
            check(alert != null && alert.completedPhase == 2 && alert.timerDuration == (i == 0 ? 180000L : 3600000L),
                    "alert snapshots the completed step");
            context.startService(AlarmDelivery.command(context, id, AlarmDelivery.NEXT));
            end = SystemClock.uptimeMillis() + 5000;
            while (alarms.ringingId() != 0 && SystemClock.uptimeMillis() < end) SystemClock.sleep(30);
            check(prefs.getInt("pomodoro_phase", -1) == (i + 1) % 2 && prefs.getBoolean("pomodoro_running", false),
                    "next starts the correct phase, including wraparound");
            check(!prefs.contains(TimerCompletion.PENDING_PHASE), "continuation consumed once");
        }
        TimerScheduler.cancel(context, "pomodoro");
        prefs.edit().clear().commit();
    }

    void checkScreenOn() throws Exception {
        context.getSharedPreferences("clockmods_onboarding", 0).edit().putBoolean("completed", true).commit();
        SharedPreferences settings = com.clockmods.pro.timer.TimingScreenOn.settings(context);
        String key = com.clockmods.pro.timer.TimingScreenOn.KEY;
        settings.edit().putString("clock_language", "en").remove(key).commit();
        prefs.edit().clear().putLong("countdown_duration", 300000L).putLong("countdown_remaining", 300000L).commit();
        context.getSharedPreferences("pro_stopwatch", 0).edit().clear().commit();
        Instrumentation.ActivityMonitor monitor = test.addMonitor(UltimateMainActivity.class.getName(), null, false);
        try {
            openTool("countdown");
            android.app.Activity host = test.waitForMonitorWithTimeout(monitor, 10000);
            check(host != null, "main activity launched");
            click("Start", 0);
            await("Pause", 1);
            checkWindow(host, false, "disabled by default for running countdown");
            settings.edit().putBoolean(key, true).commit();
            checkWindow(host, true, "enabled for running countdown");
            openTool("stopwatch");
            await("Start", 1);
            checkWindow(host, false, "idle stopwatch while countdown runs");
            click("Start", 0);
            checkWindow(host, true, "running stopwatch");
            click("Pause", 0);
            checkWindow(host, false, "paused stopwatch");
            openTool("pomodoro");
            click("Start", 0);
            checkWindow(host, true, "running Pomodoro");
            shell("input keyevent KEYCODE_HOME");
            checkWindow(host, false, "app went into background");
            openTool("pomodoro");
            await("Pause", 1);
            checkWindow(host, true, "resumed running Pomodoro");
            click("Reset", 0);
            checkWindow(host, false, "reset Pomodoro");
            openTool("countdown");
            await("Pause", 1);
            checkWindow(host, true, "return to active countdown");
            settings.edit().putBoolean(key, false).commit();
            checkWindow(host, false, "disabling releases window immediately");
        } finally {
            test.removeMonitor(monitor);
            TimerScheduler.cancel(context, "pomodoro");
            TimerScheduler.cancel(context, "countdown");
            prefs.edit().clear().commit();
            shell("input keyevent KEYCODE_HOME");
        }
    }

    private void openTool(String tool) {
        context.startActivity(new Intent(context, UltimateMainActivity.class)
                .putExtra(TimingLiveUpdates.EXTRA_DESTINATION, tool)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP));
    }

    private void checkWindow(android.app.Activity activity, boolean expected, String scenario) {
        long end = SystemClock.uptimeMillis() + 5000;
        boolean[] active = new boolean[1];
        do {
            test.runOnMainSync(() -> active[0] = (activity.getWindow().getAttributes().flags
                    & android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0);
            if (active[0] == expected) return;
            SystemClock.sleep(50);
        } while (SystemClock.uptimeMillis() < end);
        throw new AssertionError("Unexpected window flag for " + scenario);
    }

    private String saved() { return prefs.getString(PomodoroSequence.KEY, ""); }
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }

    private void activate(String label) {
        idle();
        AccessibilityNodeInfo control = await(label, 1).get(0);
        check(control.isEnabled() && control.performAction(AccessibilityNodeInfo.ACTION_CLICK), "activate " + label);
        test.runOnMainSync(() -> { });
        // Wait for the disappearing holder to leave the accessibility tree.
        SystemClock.sleep(300);
    }

    private void click(String label, int index) throws Exception {
        idle();
        Rect rect = bounds(await(label, index + 1).get(index));
        shell("input tap " + rect.centerX() + " " + rect.centerY());
        SystemClock.sleep(160);
    }

    private void idle() {
        try { test.getUiAutomation().waitForIdle(260, 1800); }
        catch (java.util.concurrent.TimeoutException tickingTimer) {
            // A running timer may emit accessibility events continuously.
        }
    }

    private void awaitGone(String label) {
        long end = SystemClock.uptimeMillis() + 8000;
        do {
            List<AccessibilityNodeInfo> found = new ArrayList<>();
            collect(root(), label, found);
            if (found.isEmpty()) return;
            SystemClock.sleep(60);
        } while (SystemClock.uptimeMillis() < end);
        throw new AssertionError("Control remained visible: " + label);
    }

    private void touch(long down, int x, int y, int action) {
        android.view.MotionEvent event = android.view.MotionEvent.obtain(down, SystemClock.uptimeMillis(), action, x, y, 0);
        event.setSource(android.view.InputDevice.SOURCE_TOUCHSCREEN);
        try { check(test.getUiAutomation().injectInputEvent(event, true), "touch delivery"); }
        finally { event.recycle(); }
    }

    private void chooseMenuItem(String label) throws Exception {
        idle();
        long end = SystemClock.uptimeMillis() + 10000;
        do {
            List<AccessibilityNodeInfo> matches = new ArrayList<>();
            collect(root(), label, matches);
            for (AccessibilityNodeInfo match : matches) {
                if (!"android.widget.TextView".contentEquals(match.getClassName())) continue;
                AccessibilityNodeInfo option = match;
                while (option != null && !option.isClickable()) option = option.getParent();
                if (option != null && option.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return;
            }
            SystemClock.sleep(60);
        } while (SystemClock.uptimeMillis() < end);
        throw new AssertionError("Missing popup item: " + label);
    }

    private static Rect bounds(AccessibilityNodeInfo node) {
        Rect rect = new Rect(); node.getBoundsInScreen(rect); return rect;
    }

    private List<AccessibilityNodeInfo> await(String label, int count) {
        long end = SystemClock.uptimeMillis() + 15000;
        do {
            List<AccessibilityNodeInfo> found = new ArrayList<>();
            collect(root(), label, found);
            if (found.size() >= count) return found;
            SystemClock.sleep(80);
        } while (SystemClock.uptimeMillis() < end);
        throw new AssertionError("Missing visible control: " + label);
    }

    private AccessibilityNodeInfo root() {
        if (android.os.Build.VERSION.SDK_INT >= 34) test.getUiAutomation().clearCache();
        return test.getUiAutomation().getRootInActiveWindow();
    }

    private void collect(AccessibilityNodeInfo node, String label, List<AccessibilityNodeInfo> found) {
        if (node == null || !node.isVisibleToUser()) return;
        if (label.equalsIgnoreCase(String.valueOf(node.getText())) || label.contentEquals(String.valueOf(node.getContentDescription()))
                || label.contentEquals(String.valueOf(node.getClassName()))) found.add(node);
        for (int i = 0; i < node.getChildCount(); i++) collect(node.getChild(i), label, found);
    }

    private void shell(String command) throws Exception {
        try (ParcelFileDescriptor.AutoCloseInputStream stream = new ParcelFileDescriptor.AutoCloseInputStream(
                test.getUiAutomation().executeShellCommand(command))) { while (stream.read() != -1) { } }
    }
}
