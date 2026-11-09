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
        return (node.getText() != null && text.equals(node.getText().toString().split("\n", 2)[0]))
                || (node.getContentDescription() != null && text.equals(node.getContentDescription().toString()));
    }

    private void clickText(String text) {
        long deadline = SystemClock.uptimeMillis() + 6000;
        do {
            for (AccessibilityNodeInfo node : nodes()) {
                if (!matchesTitle(node, text)) continue;
                while (node != null && !node.isClickable()) node = node.getParent();
                if (node != null && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return;
            }
            SystemClock.sleep(150);
        } while (SystemClock.uptimeMillis() < deadline);
        throw new AssertionError("Cannot click " + text);
    }

    // The native clock continuously draws; waiting for its message queue to become idle
    // can block forever before settings is opened. Activity monitors bound startup instead.
    private void settle() { SystemClock.sleep(1400); }

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
        String motion = action == MotionEvent.ACTION_DOWN ? "DOWN"
                : action == MotionEvent.ACTION_UP ? "UP" : "MOVE";
        // Use the platform input command: this emulator cancels UiAutomation's direct
        // injected streams at the gesture monitor, while shell touch streams reach it.
        String command = "input motionevent " + motion + " " + Math.round(x) + " " + Math.round(y);
        try (java.io.InputStream result = new android.os.ParcelFileDescriptor.AutoCloseInputStream(
                getUiAutomation().executeShellCommand(command))) {
            while (result.read() != -1) { /* Wait for this event before sending the next. */ }
        } catch (java.io.IOException error) { throw new AssertionError("Touch injection failed", error); }
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
        // Setup content is at the top on tall tablets; its center is intentionally empty.
        int top = name.startsWith("wizard") ? before.getHeight() / 10 : before.getHeight() / 5;
        int bottom = name.startsWith("wizard") ? before.getHeight() / 3 : before.getHeight() * 4 / 5;
        for (int py = top; py < bottom; py += 8) {
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

    private void checkSurfaces() {
        Bundle report = new Bundle();
        mode = "gesture";
        Activity activity = null;
        try {
            verify(getTargetContext().getPackageName().contains("predictive"), "Audit package required");
            activity = startActivitySync(new Intent(getTargetContext(), com.clockmods.ultimate.SetupWizardActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            settingsActivity = activity; settle();
            clickText(activity.getString(R.string.ultimate_language_simplified)); settle();
            clickText(activity.getString(R.string.setup_wizard_next)); settle();
            gesture(false, true, "wizard-cancel");
            verify(hasText(activity.getString(R.string.setup_wizard_weather_title)), "Wizard cancel lost step");
            gesture(false, false, "wizard-commit");
            verify(hasText(activity.getString(R.string.setup_wizard_language_title)), "Wizard did not return to language");
            Activity wizard = activity; runOnMainSync(wizard::finish); settle();
            getTargetContext().getSharedPreferences("clockmods_ultimate_style", 0).edit()
                    .putString(com.clockmods.background.ThemeLibrary.ORDER, "pro.classic|glass.atelier").commit();
            activity = startActivitySync(UltimateSettingsActivity.createSubpageIntent(getTargetContext(), "style")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            settingsActivity = activity; settle();
            clickText(activity.getString(R.string.theme_store)); settle();
            verify(hasText(activity.getString(R.string.theme_recommended)), "Store did not open");
            gesture(false, true, "store-cancel");
            verify(hasText(activity.getString(R.string.theme_recommended)), "Store cancel dismissed it");
            gesture(false, false, "store-commit");
            verify(!hasText(activity.getString(R.string.theme_recommended)), "Store gesture did not dismiss it");
            report.putString("stream", "PASS additional surfaces: " + checks + " checks\n");
            finish(Activity.RESULT_OK, report);
        } catch (Throwable error) {
            try { screenshot("surfaces-failure").recycle(); } catch (Exception ignored) { }
            report.putString("stream", "FAIL surfaces: " + android.util.Log.getStackTraceString(error));
            finish(Activity.RESULT_CANCELED, report);
        } finally {
            Activity last = activity;
            if (last != null) runOnMainSync(last::finish);
        }
    }

    @Override public void onStart() {
        if ("media".equals(mode)) { ReadmeCapture.run(this); return; }
        if ("surfaces".equals(mode)) { checkSurfaces(); return; }
        Bundle report = new Bundle();
        Activity activity = null;
        Activity clock = null;
        SharedPreferences onboarding = getTargetContext().getSharedPreferences("clockmods_onboarding", 0);
        boolean hadCompleted = onboarding.contains("completed");
        boolean completed = onboarding.getBoolean("completed", false);
        SharedPreferences preferences = getTargetContext().getSharedPreferences("clock_prefs", 0);
        boolean hadOrientation = preferences.contains("screen_orientation");
        int orientation = preferences.getInt("screen_orientation", 0);
        boolean hadWeather = preferences.contains("weather_enabled");
        boolean weatherEnabled = preferences.getBoolean("weather_enabled", true);
        preferences.edit().putInt("screen_orientation", 0).putBoolean("weather_enabled", false).commit();
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
            String homeLabel = activity.getString(R.string.ultimate_category_clock_style);
            verify(hasText(background) && hasText(homeLabel), "Expected compact settings home");
            clickText(background);
            settle();
            verify(!hasText(homeLabel), "Detail page did not open in compact mode");
            if ("keys".equals(mode)) {
                sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
                settle();
            } else {
                gesture(false, true, "left-cancel");
                verify(!hasText(homeLabel), "Cancelled gesture popped the detail page");
                gesture(true, true, "right-cancel");
                verify(!hasText(homeLabel), "Cancelled right-edge gesture popped the detail page");
                gesture(false, false, "detail-commit");
            }
            verify(hasText(background) && hasText(homeLabel), "Committed gesture did not return home");
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
                    clickText(library.getString(R.string.calendar_import_add)); settle();
                    verify(hasText(library.getString(R.string.calendar_import_link)), "Missing calendar link action");
                    if ("gesture".equals(mode)) {
                        gesture(false, true, "import-menu-cancel");
                        verify(hasText(library.getString(R.string.calendar_import_link)), "Menu cancel lost actions");
                        gesture(false, false, "import-menu-commit");
                        verify(!hasText(library.getString(R.string.calendar_import_link)), "Menu commit did not close actions");
                        clickText(library.getString(R.string.calendar_import_add)); settle();
                    }
                    clickText(library.getString(R.string.calendar_import_link));
                    settle();
                    verify(hasText(library.getString(R.string.calendar_import_link_hint)), "Missing link input dialog");
                    screenshot("calendar-link-dialog").recycle();
                    if ("gesture".equals(mode)) {
                        gesture(false, true, "link-dialog-cancel");
                        verify(hasText(library.getString(R.string.calendar_import_link_hint)), "Dialog cancel lost input");
                        gesture(false, false, "link-dialog-commit");
                    } else { sendKeyDownUpSync(KeyEvent.KEYCODE_BACK); settle(); }
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
                verify(hasText(homeLabel), "Cancelled root gesture closed settings");
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
            SharedPreferences.Editor restore = preferences.edit();
            if (hadOrientation) restore.putInt("screen_orientation", orientation); else restore.remove("screen_orientation");
            if (hadWeather) restore.putBoolean("weather_enabled", weatherEnabled); else restore.remove("weather_enabled");
            restore.commit();
        }
    }
}
