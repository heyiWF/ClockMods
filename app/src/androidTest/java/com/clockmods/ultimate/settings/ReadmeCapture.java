package com.clockmods.ultimate.settings;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;
import com.clockmods.background.ClockPreferences;
import com.clockmods.ultimate.clock.UltimateClockPreferences;
import com.clockmods.ultimate.clock.UltimateClockStyles;
import java.io.File;
import java.io.FileOutputStream;

/** Capture only the isolated audit install; never changes a user's production settings. */
public final class ReadmeCapture {
    public static void run(Instrumentation test) {
        Bundle report = new Bundle();
        try {
            String pkg = test.getTargetContext().getPackageName();
            if (!pkg.contains("predictive")) throw new IllegalStateException("Audit package required");
            test.getTargetContext().getSharedPreferences("clockmods_ultimate_style", 0).edit()
                    .remove(com.clockmods.background.ThemeLibrary.ORDER).commit();
            ClockPreferences prefs = new ClockPreferences(test.getTargetContext());
            prefs.setClockLanguage("zh");
            prefs.setScreenOrientation(ClockPreferences.ORIENTATION_LANDSCAPE);
            prefs.setDateLunarDualLine(true);
            prefs.setWeatherEnabled(true);
            prefs.setManualWeatherLocation("101010100", "北京", "北京", "北京", 39.90, 116.40);
            prefs.setWeatherLocationMode(ClockPreferences.WEATHER_LOCATION_MANUAL);
            prefs.setCalendarTheme("calendar.graphite");
            new UltimateClockPreferences(test.getTargetContext()).setStyleId(UltimateClockStyles.STYLE_DUAL_BLOCKS);
            test.getTargetContext().getSharedPreferences("clockmods_onboarding", 0)
                    .edit().putBoolean("completed", true).commit();
            open(test, "com.clockmods.ultimate.UltimateMainActivity", null);
            awaitClockFrame(test);
            SystemClock.sleep(2500);
            shot(test, "clock");
            android.os.ParcelFileDescriptor recording = test.getUiAutomation().executeShellCommand(
                    "screenrecord --bit-rate 6000000 --time-limit 26 /sdcard/" + pkg + "-overview.mp4");
            SystemClock.sleep(3200);
            swipe(test);
            SystemClock.sleep(3200);
            shot(test, "calendar");
            open(test, UltimateSettingsActivity.class.getName(), null);
            SystemClock.sleep(2800);
            shot(test, "settings");
            open(test, CalendarManagerActivity.class.getName(), null);
            SystemClock.sleep(2800);
            shot(test, "calendar-library");
            test.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);
            SystemClock.sleep(1500);
            open(test, FontManagerActivity.class.getName(), null);
            SystemClock.sleep(2800);
            shot(test, "font-library");
            test.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);
            SystemClock.sleep(1500);
            test.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);
            SystemClock.sleep(2500);
            try (java.io.InputStream stream = new android.os.ParcelFileDescriptor.AutoCloseInputStream(recording)) {
                while (stream.read() != -1) { }
            }
            report.putString("result", "PASS current app screenshots and recording captured");
            test.finish(Activity.RESULT_OK, report);
        } catch (Throwable error) {
            report.putString("failure", android.util.Log.getStackTraceString(error));
            test.finish(Activity.RESULT_CANCELED, report);
        }
    }
    private static void awaitClockFrame(Instrumentation test) {
        long deadline = SystemClock.uptimeMillis() + 40000;
        do {
            SystemClock.sleep(500);
            Bitmap frame = test.getUiAutomation().takeScreenshot();
            if (frame == null) continue;
            java.util.Set<Integer> colors = new java.util.HashSet<>();
            for (int y = frame.getHeight() / 5; y < frame.getHeight() * 4 / 5; y += 13)
                for (int x = frame.getWidth() / 5; x < frame.getWidth() * 4 / 5; x += 13)
                    colors.add(frame.getPixel(x, y));
            frame.recycle();
            if (colors.size() > 6) return;
        } while (SystemClock.uptimeMillis() < deadline);
        throw new IllegalStateException("Clock content did not render before capture");
    }
    private static void open(Instrumentation test, String name, String page) {
        Intent intent = new Intent().setClassName(test.getTargetContext(), name)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (page != null) intent.putExtra("page", page);
        test.getTargetContext().startActivity(intent);
    }
    private static void shot(Instrumentation test, String name) throws Exception {
        Bitmap bitmap = test.getUiAutomation().takeScreenshot();
        if (bitmap == null) throw new IllegalStateException("No screenshot");
        File directory = new File(test.getTargetContext().getExternalFilesDir(null), "readme");
        directory.mkdirs();
        try (FileOutputStream out = new FileOutputStream(new File(directory, name + ".png"))) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        }
        bitmap.recycle();
    }
    private static void swipe(Instrumentation test) {
        Bitmap frame = test.getUiAutomation().takeScreenshot();
        int width = frame.getWidth(), height = frame.getHeight(); frame.recycle();
        String command = "input swipe " + (int) (width * .85f) + " " + height / 2
                + " " + (int) (width * .15f) + " " + height / 2 + " 450";
        try (java.io.InputStream input = new android.os.ParcelFileDescriptor.AutoCloseInputStream(
                test.getUiAutomation().executeShellCommand(command))) {
            while (input.read() != -1) { }
        } catch (java.io.IOException error) { throw new IllegalStateException(error); }
    }
}
