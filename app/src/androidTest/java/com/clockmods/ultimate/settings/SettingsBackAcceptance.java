package com.clockmods.ultimate.settings;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.activity.BackEventCompat;
import androidx.activity.ComponentActivity;
import androidx.activity.OnBackPressedDispatcher;

import com.clockmods.R;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Real edge gestures: preview, cancel, commit, then leave settings through system Back.
 * Run on Android 15+ in a compact portrait window with gesture navigation enabled.
 * Build with -Pclockmods.testRunner=com.clockmods.ultimate.settings.SettingsBackAcceptance,
 * then run am instrument -w against that runner in the audit application's test package.
 * On emulators without an edge gesture handler, -e mode dispatcher tests Compose's
 * in-app progress/cancellation, or -e mode keys tests the native back stack only.
 * These explicit fallback modes do not validate platform cross-activity animations.
 */
public final class SettingsBackAcceptance extends Instrumentation {
    private int checks;
    private String label;
    private String mode;
    private String calendarLink;
    private Activity settingsActivity;

    @Override public void onCreate(Bundle arguments) {
        super.onCreate(arguments);
        label = arguments == null ? "back" : arguments.getString("label", "back");
        mode = arguments == null ? "gesture" : arguments.getString("mode", "gesture");
        calendarLink = arguments == null ? null : arguments.getString("calendarLink");
        start();
    }

    private void verify(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }

    private List<AccessibilityNodeInfo> nodes() {
        List<AccessibilityNodeInfo> result = new ArrayList<>();
        if (android.os.Build.VERSION.SDK_INT >= 33) getUiAutomation().clearCache();
        collect(getUiAutomation().getRootInActiveWindow(), result);
        return result;
    }

    private void collect(AccessibilityNodeInfo node, List<AccessibilityNodeInfo> result) {
        if (node == null) return;
        result.add(node);
        for (int i = 0; i < node.getChildCount(); i++) collect(node.getChild(i), result);
    }

    private boolean hasText(String text) {
        for (AccessibilityNodeInfo node : nodes()) {
            if (matchesTitle(node, text)) return true;
        }
        return false;
    }

    private boolean matchesTitle(AccessibilityNodeInfo node, String text) {
        // Compose merges a row's title and summary into one accessibility node.
        return node.getText() != null && text.equals(node.getText().toString().split("\n", 2)[0]);
    }

    private void clickText(String text) {
        for (AccessibilityNodeInfo node : nodes()) {
            if (!matchesTitle(node, text)) continue;
            while (node != null && !node.isClickable()) node = node.getParent();
            if (node != null && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return;
        }
        throw new AssertionError("Cannot click " + text);
    }

    // The native clock continuously draws; waiting for its message queue to become idle
    // can block forever before settings is opened. Activity monitors bound startup instead.
    private void settle() { SystemClock.sleep(700); }

    private Bitmap screenshot(String name) throws Exception {
        Bitmap image = getUiAutomation().takeScreenshot();
        if (image == null) throw new AssertionError("No screenshot");
        File dir = new File(getTargetContext().getExternalFilesDir(null), "settings-back");
        dir.mkdirs();
        try (FileOutputStream out = new FileOutputStream(new File(dir, label + "-" + name + ".png"))) {
            image.compress(Bitmap.CompressFormat.PNG, 100, out);
        }
        return image;
    }

    private void touch(long down, int action, float x, float y) {
        MotionEvent event = MotionEvent.obtain(down, SystemClock.uptimeMillis(), action, x, y, 0);
        event.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        try { verify(getUiAutomation().injectInputEvent(event, true), "Gesture injection failed"); }
        finally { event.recycle(); }
    }

    private void gesture(boolean right, boolean cancel, String name) throws Exception {
        Bitmap before = screenshot(name + "-before");
        float width = before.getWidth();
        float y = before.getHeight() * 0.55f;
        float start = right ? width - 2 : 2;
        float sign = right ? -1 : 1;
        long down = SystemClock.uptimeMillis();
        OnBackPressedDispatcher dispatcher = ((ComponentActivity) settingsActivity).getOnBackPressedDispatcher();
        int edge = right ? BackEventCompat.EDGE_RIGHT : BackEventCompat.EDGE_LEFT;
        boolean dispatch = "dispatcher".equals(mode);
        if (dispatch) runOnMainSync(() -> dispatcher.dispatchOnBackStarted(new BackEventCompat(start, y, 0, edge)));
        else touch(down, MotionEvent.ACTION_DOWN, start, y);
        for (int step = 1; step <= 18; step++) {
            float progress = 0.6f * step / 18;
            float x = start + sign * width * 0.4f * step / 18;
            if (dispatch) runOnMainSync(() -> dispatcher.dispatchOnBackProgressed(new BackEventCompat(x, y, progress, edge)));
            else touch(down, MotionEvent.ACTION_MOVE, x, y);
            SystemClock.sleep(20);
        }
        SystemClock.sleep(120);
        Bitmap preview = screenshot(name + "-preview");
        int changed = 0, samples = 0;
        // Exclude the edge arrow and system bars: the content itself must preview navigation.
        for (int py = before.getHeight() / 5; py < before.getHeight() * 4 / 5; py += 8) {
            for (int px = before.getWidth() / 4; px < before.getWidth() * 3 / 4; px += 8) {
                samples++;
                if (before.getPixel(px, py) != preview.getPixel(px, py)) changed++;
            }
        }
        if (dispatch) runOnMainSync(() -> {
            if (cancel) dispatcher.dispatchOnBackCancelled();
            else dispatcher.onBackPressed();
        });
        else {
            // A user cancels by returning to the edge before lifting their finger.
            // ACTION_CANCEL aborts injection, leaving some system gesture handlers latched.
            if (cancel) {
                for (int step = 17; step >= 0; step--) {
                    touch(down, MotionEvent.ACTION_MOVE, start + sign * width * 0.4f * step / 18, y);
                    SystemClock.sleep(20);
                }
            }
            touch(down, MotionEvent.ACTION_UP, cancel ? start : start + sign * width * 0.4f, y);
        }
        settle();
        screenshot(name + "-after").recycle();
        before.recycle(); preview.recycle();
        verify(changed > samples / 50, "Back preview did not animate screen content: " + name);
    }

    private void verifySystemBackAvailable() {
        boolean[] intercepted = new boolean[1];
        runOnMainSync(() -> intercepted[0] = ((ComponentActivity) settingsActivity)
                .getOnBackPressedDispatcher().hasEnabledCallbacks());
        verify(!intercepted[0], "Settings root intercepts platform predictive Back");
    }

    @Override public void onStart() {
        Bundle report = new Bundle();
        Activity activity = null;
        Activity clock = null;
        SharedPreferences onboarding = getTargetContext().getSharedPreferences("clockmods_onboarding", 0);
        boolean hadCompleted = onboarding.contains("completed");
        boolean completed = onboarding.getBoolean("completed", false);
        try {
            getUiAutomation().setServiceInfo(getUiAutomation().getServiceInfo());
            onboarding.edit().putBoolean("completed", true).commit();
            String clockClass = "com.clockmods.ultimate.UltimateMainActivity";
            ActivityMonitor clockMonitor = addMonitor(clockClass, null, false);
            getTargetContext().startActivity(new Intent().setClassName(getTargetContext(), clockClass)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            clock = clockMonitor.waitForActivityWithTimeout(10000);
            removeMonitor(clockMonitor);
            verify(clock != null, "Clock did not start");
            settle();
            ActivityMonitor monitor = addMonitor(UltimateSettingsActivity.class.getName(), null, false);
            Activity clockActivity = clock;
            runOnMainSync(() -> clockActivity.startActivity(UltimateSettingsActivity.createIntent(clockActivity)));
            activity = monitor.waitForActivityWithTimeout(10000);
            removeMonitor(monitor);
            verify(activity != null, "Settings did not open from the clock");
            settingsActivity = activity;
            settle();
            SystemClock.sleep(1000);
            String background = activity.getString(R.string.ultimate_category_background);
            String weather = activity.getString(R.string.ultimate_category_weather);
            verify(hasText(background) && hasText(weather), "Expected compact settings home");
            clickText(background);
            settle();
            verify(!hasText(weather), "Detail page did not open in compact mode");
            if ("keys".equals(mode)) {
                sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
                settle();
            } else {
                gesture(false, true, "left-cancel");
                verify(!hasText(weather), "Cancelled gesture popped the detail page");
                gesture(true, true, "right-cancel");
                verify(!hasText(weather), "Cancelled right-edge gesture popped the detail page");
                gesture(false, false, "detail-commit");
            }
            verify(hasText(background) && hasText(weather), "Committed gesture did not return home");
            verifySystemBackAvailable();
            // Every library is a real Activity destination. Leave its back callback disabled
            // so Android can preview the settings window underneath, including cancellation.
            for (Class<?> libraryClass : new Class<?>[]{CalendarManagerActivity.class, FontManagerActivity.class}) {
                ActivityMonitor libraryMonitor = addMonitor(libraryClass.getName(), null, false);
                Activity settings = activity;
                runOnMainSync(() -> settings.startActivity(new Intent(settings, libraryClass)));
                Activity library = libraryMonitor.waitForActivityWithTimeout(10000);
                removeMonitor(libraryMonitor);
                verify(library != null, "Library did not open: " + libraryClass.getSimpleName());
                settingsActivity = library;
                settle();
                verifySystemBackAvailable();
                String libraryName = libraryClass.getSimpleName();
                if (libraryClass == CalendarManagerActivity.class) {
                    clickText(library.getString(R.string.calendar_import_add));
                    verify(hasText(library.getString(R.string.calendar_import_link)), "Missing calendar link action");
                    clickText(library.getString(R.string.calendar_import_link));
                    settle();
                    verify(hasText(library.getString(R.string.calendar_import_link_hint)), "Missing link input dialog");
                    screenshot("calendar-link-dialog").recycle();
                    sendKeyDownUpSync(KeyEvent.KEYCODE_BACK); settle();
                    verify(!library.isFinishing(), "Dialog back exited the calendar library");
                    com.clockmods.calendar.imports.UserCalendarStore store = com.clockmods.calendar.imports.UserCalendarStore.get(library);
                    if (calendarLink != null) {
                        com.clockmods.calendar.imports.UserCalendarStore.Source downloaded =
                                com.clockmods.calendar.imports.CalendarLink.importFrom(store, calendarLink);
                        try {
                            verify(downloaded.eventCount > 0, "Link import did not store events");
                            verify(store.files().stream().anyMatch(file -> file.id.equals(downloaded.id)),
                                    "Downloaded calendar is missing from the library");
                        } finally { store.delete(downloaded.id); }
                    }
                    java.time.LocalDate date = java.time.LocalDate.now();
                    String compactDate = date.format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
                    String fixture = "BEGIN:VCALENDAR\r\nVERSION:2.0\r\nPRODID:-//ClockMods//Acceptance//EN\r\nBEGIN:VEVENT\r\nUID:launch-toggle-check\r\nDTSTAMP:20261025T000000Z\r\nDTSTART;VALUE=DATE:"
                            + compactDate + "\r\nSUMMARY:Toggle acceptance\r\nEND:VEVENT\r\nEND:VCALENDAR\r\n";
                    boolean oldToggle = store.showInDateCarousel();
                    com.clockmods.calendar.imports.UserCalendarStore.Source source = store.importStream(
                            new java.io.ByteArrayInputStream(fixture.getBytes(java.nio.charset.StandardCharsets.UTF_8)), "toggle-check.ics");
                    try {
                        java.util.TimeZone zone = java.util.TimeZone.getDefault();
                        long until = SystemClock.uptimeMillis() + 10000;
                        while (store.eventsOn(date.getYear(), date.getMonthValue()-1, date.getDayOfMonth(), zone).isEmpty()
                                && SystemClock.uptimeMillis() < until) SystemClock.sleep(100);
                        runOnMainSync(() -> store.setShowInDateCarousel(false));
                        verify(!store.showInDateCarousel(), "Carousel toggle did not persist");
                        verify(!com.clockmods.calendar.imports.ImportedSchedules.on(library, store,
                                date.getYear(), date.getMonthValue()-1, date.getDayOfMonth(), zone).isEmpty(),
                                "Disabling date labels removed Agenda events");
                        runOnMainSync(() -> store.setShowInDateCarousel(true));
                        verify(store.showInDateCarousel(), "Carousel toggle did not re-enable");
                    } finally {
                        store.delete(source.id);
                        runOnMainSync(() -> store.setShowInDateCarousel(oldToggle));
                    }

                }
                if ("gesture".equals(mode)) {
                    gesture(false, true, libraryName + "-cancel");
                    verify(!library.isFinishing(), "Cancelled library gesture finished its Activity");
                    gesture(false, false, libraryName + "-commit");
                } else { sendKeyDownUpSync(KeyEvent.KEYCODE_BACK); settle(); }
                verify(library.isFinishing() || library.isDestroyed(), "Library back did not return to settings");
                settingsActivity = activity;
                settle();
            }
            if ("gesture".equals(mode)) {
                gesture(false, true, "root-cancel");
                verify(hasText(weather), "Cancelled root gesture closed settings");
                gesture(false, false, "root-commit");
            } else {
                sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
                settle();
            }
            verify(activity.isFinishing() || activity.isDestroyed(),
                    "Committed root gesture did not return to the clock");
            report.putString("stream", "PASS " + label + " (" + mode + "): " + checks + " checks\n");
            finish(Activity.RESULT_OK, report);
        } catch (Throwable error) {
            try { screenshot("failure").recycle(); }
            catch (Exception screenshotError) { error.addSuppressed(screenshotError); }
            report.putString("stream", "FAIL " + label + ": " + android.util.Log.getStackTraceString(error));
            finish(Activity.RESULT_CANCELED, report);
        } finally {
            Activity started = activity;
            if (started != null) runOnMainSync(started::finish);
            Activity startedClock = clock;
            if (startedClock != null) runOnMainSync(startedClock::finish);
            SharedPreferences.Editor editor = onboarding.edit();
            if (hadCompleted) editor.putBoolean("completed", completed);
            else editor.remove("completed");
            editor.commit();
        }
    }
}
