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
    private Activity activity;
    @Override public void onCreate(Bundle args) { super.onCreate(args); start(); }
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
            checkNotifications();
            checkLaps();
            result.putString("stream", "PASS " + checks + " timing checks; SDK " + android.os.Build.VERSION.SDK_INT
                    + "; promoted system UI requires a supporting device.\n");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable error) {
            result.putString("stream", android.util.Log.getStackTraceString(error));
            finish(Activity.RESULT_CANCELED, result);
        }
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
        check(notification(211) == null && notification(201) != null, "completion replaces live update with alert");
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
        check(find(getUiAutomation().getRootInActiveWindow(), "61", false) != null, "new lap scrolls into view");
        for (int i = 0; i < 4; i++) { click(label); SystemClock.sleep(70); }
        SystemClock.sleep(700);
        check(find(getUiAutomation().getRootInActiveWindow(), "65", false) != null, "rapid laps finish at newest row");
        check(p.getString("laps", "").split(",").length == 65, "all rapid laps retained");
        java.io.File screenshot = new java.io.File(getTargetContext().getExternalFilesDir(null), "timing-laps.png");
        try (java.io.FileOutputStream out = new java.io.FileOutputStream(screenshot)) {
            getUiAutomation().takeScreenshot().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out);
        }
        // Notification tap must navigate an existing activity to its own destination.
        TimingLiveUpdates.open(getTargetContext(), "countdown", 212).send();
        SystemClock.sleep(900);
        check(find(getUiAutomation().getRootInActiveWindow(), activity.getString(R.string.timer_custom), false) != null,
                "notification opens countdown destination");
        runOnMainSync(() -> activity.finish());
    }
    private void click(String label) {
        AccessibilityNodeInfo node = find(getUiAutomation().getRootInActiveWindow(), label, false);
        while (node != null && !node.isClickable()) node = node.getParent();
        check(node != null && node.performAction(AccessibilityNodeInfo.ACTION_CLICK), "click " + label);
    }
    private AccessibilityNodeInfo find(AccessibilityNodeInfo node, String label, boolean scroll) {
        if (node == null || !node.isVisibleToUser()) return null;
        if (label != null && (label.contentEquals(node.getText() == null ? "" : node.getText())
                || label.contentEquals(node.getContentDescription() == null ? "" : node.getContentDescription()))) {
            Rect bounds = new Rect(); node.getBoundsInScreen(bounds);
            if (!bounds.isEmpty() && (node.isClickable() || !label.equals(activity.getString(R.string.stopwatch_lap)))) return node;
        }
        for (int i = node.getChildCount() - 1; i >= 0; i--) {
            AccessibilityNodeInfo found = find(node.getChild(i), label, scroll);
            if (found != null) return found;
        }
        if (scroll && node.isScrollable()) return node;
        return null;
    }
}
