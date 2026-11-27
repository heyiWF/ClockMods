package com.clockmods.pro.timer;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.SystemClock;
import android.service.notification.StatusBarNotification;
import android.view.accessibility.AccessibilityNodeInfo;
import com.clockmods.R;
import com.clockmods.ultimate.UltimateMainActivity;

/** Device regression checks. Run only against a separate audit application ID. */
public final class TimingAcceptance extends Instrumentation {
    private int checks;
    private boolean countdownTest;
    private Activity activity;
    @Override public void onCreate(Bundle args) { super.onCreate(args); countdownTest = "countdowns".equals(args.getString("mode")); start(); }
    private void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
            check(getTargetContext().getPackageName().contains("timingaudit"), "isolated audit package required");
            getTargetContext().getSharedPreferences("clockmods_onboarding", 0).edit()
                    .putBoolean("completed", true).commit();
            getTargetContext().getSharedPreferences("clock_prefs", 0).edit()
                    .putString("clock_language", "en").commit();
            if (countdownTest) checkCountdowns();
            else { checkNotifications(); checkLaps(); }
            result.putString("stream", "PASS " + checks + " timing checks; SDK " + android.os.Build.VERSION.SDK_INT
                    + "; promoted system UI requires a supporting device.\n");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable error) {
            result.putString("stream", android.util.Log.getStackTraceString(error));
            finish(Activity.RESULT_CANCELED, result);
        }
    }
    private void checkCountdowns() throws Exception {
        android.content.Context context = getTargetContext();
        CountdownTimers store = new CountdownTimers(context);
        SharedPreferences prefs = store.preferences;
        long original = System.currentTimeMillis() + 420_000;
        prefs.edit().clear().putBoolean("countdown_running", true).putLong("countdown_deadline", original)
                .putLong("countdown_duration", 420_000).commit();
        getUiAutomation();
        activity = startActivitySync(new Intent(context, UltimateMainActivity.class)
                .putExtra(TimingLiveUpdates.EXTRA_DESTINATION, "countdown").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try {
            clickWhenReady(activity.getString(R.string.timer_custom));
            clickWhenReady("1");
            clickWhenReady("3");
            clickWhenReady("0");
            AccessibilityNodeInfo field = awaitEditable();
            Bundle name = new Bundle();
            name.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "Tea");
            check(field != null && field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, name), "entered a timer label");
            clickWhenReady(activity.getString(R.string.timer_start));
            SystemClock.sleep(300);
            CountdownTimers.Timer tea = store.all().get(0);
            check(store.all().size() == 2 && tea.running && tea.duration == 90_000 && "Tea".equals(tea.label),
                    "keypad starts a labeled 1:30 timer alongside a legacy timer");
            clickWhenReady(activity.getString(R.string.countdown_minute), "Tea");
            SystemClock.sleep(250);
            check(store.all().get(0).deadline == tea.deadline + 60_000 &&
                    prefs.getLong("countdown_deadline", 0) == original, "+1 minute changes only the chosen deadline");
            clickWhenReady(activity.getString(R.string.timer_pause), "Tea");
            SystemClock.sleep(250);
            CountdownTimers.Timer paused = store.all().get(0);
            check(!paused.running && paused.remaining > 140_000 && prefs.getBoolean("countdown_running", false),
                    "pause saves the extended remainder independently");
            clickWhenReady(activity.getString(R.string.timer_start), "Tea");
            clickWhenReady(activity.getString(R.string.timer_minutes, 5));
            SystemClock.sleep(500);
            check(store.all().size() == 3 && store.all().get(0).duration == 300_000, "preset adds a parallel timer");
            refresh();
            int notifications = 0;
            boolean named = false;
            for (StatusBarNotification item : context.getSystemService(NotificationManager.class).getActiveNotifications()) {
                if (item.getId() != TimingLiveUpdates.COUNTDOWN_ID) continue;
                notifications++;
                named |= "Tea".contentEquals(item.getNotification().extras.getCharSequence(Notification.EXTRA_TITLE));
            }
            check(notifications == 3 && named, "all three live notifications coexist with the saved label");
            String preset = store.all().get(0).name(activity);
            clickWhenReady(activity.getString(R.string.countdown_delete), preset);
            clickWhenReady("Tea");
            AccessibilityNodeInfo rename = awaitEditable();
            name.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "Tea leaves");
            check(rename != null && rename.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, name), "label opens its editor");
            clickWhenReady(activity.getString(R.string.apply));
            clickWhenReady(activity.getString(R.string.timer_reset), "Tea leaves");
            SystemClock.sleep(200);
            check(!prefs.getBoolean(tea.id + "_running", true) && prefs.getLong(tea.id + "_remaining", 0) == 150_000,
                    "reset restores just the chosen timer");
            clickWhenReady(activity.getString(R.string.countdown_delete), "Tea leaves");
            SystemClock.sleep(200);
            check(store.all().size() == 1 && !CountdownTimers.ids(prefs).contains(tea.id), "cross deletes just that timer");
            runOnMainSync(activity::finish);
            for (CountdownTimers.Timer timer : store.all()) store.remove(timer.id);
            com.clockmods.pro.alarm.AlarmStore alerts = new com.clockmods.pro.alarm.AlarmStore(context);
            long countBefore = alerts.timerAlerts().size();
            String first = store.startNew(3500, "One");
            String second = store.startNew(3500, "Two");
            TimerScheduler.cancel(context, first);
            TimerScheduler.cancel(context, second);
            new TimerRescheduleReceiver().onReceive(context, new Intent(Intent.ACTION_BOOT_COMPLETED));
            long limit = SystemClock.uptimeMillis() + 12_000;
            while (alerts.timerAlerts().size() < countBefore + 2 && SystemClock.uptimeMillis() < limit) SystemClock.sleep(100);
            check(alerts.timerAlerts().size() == countBefore + 2, "both alarms fire after schedule restoration without an open page");
            TimerReceiver.complete(context, first);
            TimerReceiver.complete(context, second);
            int matches = 0;
            for (com.clockmods.pro.alarm.AlarmSpec alarm : alerts.timerAlerts()) {
                if ("One".equals(alarm.timerLabel) || "Two".equals(alarm.timerLabel)) matches++;
            }
            check(matches == 2 && alerts.timerAlerts().size() == countBefore + 2, "two independent expiries retain labels exactly once");
        } finally {
            if (!activity.isFinishing()) runOnMainSync(activity::finish);
            for (CountdownTimers.Timer timer : store.all()) store.remove(timer.id);
            com.clockmods.pro.alarm.AlarmStore alerts = new com.clockmods.pro.alarm.AlarmStore(context);
            for (com.clockmods.pro.alarm.AlarmSpec alarm : alerts.timerAlerts()) {
                context.startService(com.clockmods.pro.alarm.AlarmDelivery.command(context, alarm.id, com.clockmods.pro.alarm.AlarmDelivery.STOP));
            }
        }
    }

    private AccessibilityNodeInfo awaitEditable() {
        long deadline = SystemClock.uptimeMillis() + 6000;
        do {
            if (android.os.Build.VERSION.SDK_INT >= 33) getUiAutomation().clearCache();
            AccessibilityNodeInfo field = editable(getUiAutomation().getRootInActiveWindow());
            if (field != null) return field;
            SystemClock.sleep(80);
        } while (SystemClock.uptimeMillis() < deadline);
        return null;
    }

    private AccessibilityNodeInfo editable(AccessibilityNodeInfo node) {
        if (node == null || node.isEditable()) return node;
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo found = editable(node.getChild(i));
            if (found != null) return found;
        }
        return null;
    }

    private void clickWhenReady(String text) { clickWhenReady(text, null); }

    private void clickWhenReady(String text, String timerLabel) {
        long limit = SystemClock.uptimeMillis() + 6000;
        do {
            if (android.os.Build.VERSION.SDK_INT >= 33) getUiAutomation().clearCache();
            AccessibilityNodeInfo root = getUiAutomation().getRootInActiveWindow();
            AccessibilityNodeInfo group = timerLabel == null ? root : find(root, timerLabel, false);
            AccessibilityNodeInfo node = null;
            while (group != null && node == null) {
                node = find(group, text, false);
                group = group.getParent();
            }
            while (node != null && !node.isClickable()) node = node.getParent();
            if (node != null && node.isEnabled() && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                SystemClock.sleep(120);
                return;
            }
            SystemClock.sleep(100);
        } while (SystemClock.uptimeMillis() < limit);
        throw new AssertionError("Missing countdown control: " + text);
    }
    private Notification notification(int id) {
        for (StatusBarNotification n : getTargetContext().getSystemService(NotificationManager.class).getActiveNotifications())
            if (n.getId() == id) return n.getNotification();
        return null;
    }
    private void refresh() {
        runOnMainSync(() -> TimingLiveUpdates.refreshAll(getTargetContext()));
        SystemClock.sleep(350);
    }
    private void checkNotifications() throws Exception {
        SharedPreferences timers = getTargetContext().getSharedPreferences("pro_timers", 0);
        SharedPreferences stopwatch = getTargetContext().getSharedPreferences("pro_stopwatch", 0);
        timers.edit().clear().commit();
        stopwatch.edit().clear().commit();
        long now = System.currentTimeMillis(), deadline = now + 120_000;
        timers.edit().putBoolean("pomodoro_running", true).putLong("pomodoro_deadline", deadline)
                .putInt("pomodoro_phase", 0).putBoolean("countdown_running", true)
                .putLong("countdown_deadline", deadline).commit();
        stopwatch.edit().putBoolean("running", true).putLong("accumulated", 15_000)
                .putLong("started_at_realtime", SystemClock.elapsedRealtime())
                .putLong("started_at_wall", now).commit();
        refresh();
        for (int id : new int[]{211, 212, 213}) {
            Notification n = notification(id);
            check(n != null, "all three timers coexist: " + id);
            check((n.flags & Notification.FLAG_ONGOING_EVENT) != 0, "ongoing flag");
            check(n.extras.getBoolean("android.requestPromotedOngoing"), "promotion requested");
            check(n.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER), "system chronometer");
            check(n.extras.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN) == (id != 213), "direction");
            check(n.contentView == null && n.bigContentView == null, "native template");
            check(id == 213 ? Math.abs(n.when - (now - 15_000)) < 2000 : n.when == deadline, "time base");
            if (android.os.Build.VERSION.SDK_INT >= 36 && android.os.Build.VERSION.SDK_INT_FULL >= android.os.Build.VERSION_CODES_FULL.BAKLAVA_1) {
                check(n.hasPromotableCharacteristics(), "promotable characteristics");
            }
        }
        timers.edit().putBoolean("countdown_running", false).commit();
        stopwatch.edit().putBoolean("running", false).commit();
        refresh();
        check(notification(212) == null && notification(213) == null, "pause removes live notification");
        check(notification(211) != null, "pause does not cancel another timer");
        timers.edit().putLong("pomodoro_deadline", System.currentTimeMillis() - 10).commit();
        runOnMainSync(() -> { TimerReceiver.complete(getTargetContext(), "pomodoro");
            TimerReceiver.complete(getTargetContext(), "pomodoro"); });
        SystemClock.sleep(350);
        check(timers.getInt("pomodoro_phase", -1) == 1, "completion advances once");
        check(!timers.getBoolean("pomodoro_running", true), "completion persists stopped state");
        check(notification(211) == null && notification(100) != null, "completion replaces live update with alert");
        timers.edit().putBoolean("countdown_running", true)
                .putLong("countdown_deadline", System.currentTimeMillis() + 600).commit();
        refresh();
        SystemClock.sleep(1100);
        check(notification(212) == null, "timeout clears live notification when alarm is delayed");
        timers.edit().clear().commit();
        stopwatch.edit().clear().commit();
        refresh();
    }
    private void checkLaps() throws Exception {
        SharedPreferences p = getTargetContext().getSharedPreferences("pro_stopwatch", 0);
        StringBuilder laps = new StringBuilder();
        for (int i = 60; i > 0; i--) { if (laps.length() > 0) laps.append(','); laps.append(i * 1000); }
        p.edit().putLong("accumulated", 60_000).putString("laps", laps.toString()).commit();
        getUiAutomation();
        activity = startActivitySync(new Intent(getTargetContext(), UltimateMainActivity.class)
                .putExtra(TimingLiveUpdates.EXTRA_DESTINATION, "stopwatch").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        SystemClock.sleep(1800);
        AccessibilityNodeInfo scroll = null;
        long readyBy = SystemClock.uptimeMillis() + 10_000;
        while (scroll == null && SystemClock.uptimeMillis() < readyBy) {
            scroll = find(getUiAutomation().getRootInActiveWindow(), null, true);
            if (scroll == null) SystemClock.sleep(200);
        }
        check(scroll != null, "lap list is scrollable");
        for (int i = 0; i < 4; i++) {
            scroll.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
            SystemClock.sleep(150);
            scroll = find(getUiAutomation().getRootInActiveWindow(), null, true);
        }
        SystemClock.sleep(400);
        check(find(getUiAutomation().getRootInActiveWindow(), "60", false) == null, "older laps are in view before insertion");
        String label = activity.getString(R.string.stopwatch_lap);
        click(label);
        SystemClock.sleep(700);
        check(awaitVisible("61"), "new lap scrolls into view");
        for (int i = 0; i < 4; i++) { click(label); SystemClock.sleep(70); }
        SystemClock.sleep(700);
        check(awaitVisible("65"), "rapid laps finish at newest row");
        check(p.getString("laps", "").split(",").length == 65, "all rapid laps retained");
        java.io.File screenshot = new java.io.File(getTargetContext().getExternalFilesDir(null), "timing-laps.png");
        try (java.io.FileOutputStream out = new java.io.FileOutputStream(screenshot)) {
            getUiAutomation().takeScreenshot().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out);
        }
        // Notification tap must navigate an existing activity to its own destination.
        TimingLiveUpdates.open(getTargetContext(), "countdown", 212).send();
        SystemClock.sleep(900);
        check(awaitVisible(activity.getString(R.string.timer_custom)),
                "notification opens countdown destination");
        runOnMainSync(() -> activity.finish());
    }
    private boolean awaitVisible(String label) {
        long deadline = SystemClock.uptimeMillis() + 5000;
        do {
            if (android.os.Build.VERSION.SDK_INT >= 33) getUiAutomation().clearCache();
            if (find(getUiAutomation().getRootInActiveWindow(), label, false) != null) return true;
            SystemClock.sleep(100);
        } while (SystemClock.uptimeMillis() < deadline);
        return false;
    }
    private void click(String label) {
        AccessibilityNodeInfo node = find(getUiAutomation().getRootInActiveWindow(), label, false);
        while (node != null && !node.isClickable()) node = node.getParent();
        check(node != null && node.performAction(AccessibilityNodeInfo.ACTION_CLICK), "click " + label);
    }
    private boolean hasClickableParent(AccessibilityNodeInfo node) {
        while (node != null) {
            if (node.isClickable()) return true;
            node = node.getParent();
        }
        return false;
    }
    private AccessibilityNodeInfo find(AccessibilityNodeInfo node, String label, boolean scroll) {
        if (node == null || !node.isVisibleToUser()) return null;
        if (label != null && (label.contentEquals(node.getText() == null ? "" : node.getText())
                || label.contentEquals(node.getContentDescription() == null ? "" : node.getContentDescription()))) {
            Rect bounds = new Rect(); node.getBoundsInScreen(bounds);
            if (!bounds.isEmpty() && (hasClickableParent(node) || !label.equals(activity.getString(R.string.stopwatch_lap)))) return node;
        }
        for (int i = node.getChildCount() - 1; i >= 0; i--) {
            AccessibilityNodeInfo found = find(node.getChild(i), label, scroll);
            if (found != null) return found;
        }
        if (scroll && node.isScrollable()) return node;
        return null;
    }
}
