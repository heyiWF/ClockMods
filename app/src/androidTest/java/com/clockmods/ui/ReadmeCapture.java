package com.clockmods.ui;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.view.View;
import com.clockmods.background.BackgroundRepository;
import com.clockmods.background.ClockPreferences;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Calendar;

/** Records the real Pro UI in a disposable, separately identified installation. */
public final class ReadmeCapture extends Instrumentation {
    private String mode;
    private volatile Activity activity;
    @Override public void callActivityOnResume(Activity resumed) {
        super.callActivityOnResume(resumed);
        if (resumed.getClass().getName().equals("com.clockmods.pro.ProMainActivity")) activity = resumed;
    }
    @Override public void onCreate(Bundle args) {
        super.onCreate(args);
        mode = args == null ? "overview" : args.getString("mode", "overview");
        start();
    }
    @Override public void onStart() {
        Bundle report = new Bundle();
        try {
            String pkg = getTargetContext().getPackageName();
            if (!pkg.equals("com.clockmods.readmemain.pro"))
                throw new IllegalStateException("Use the isolated README capture package");
            ClockPreferences prefs = new ClockPreferences(getTargetContext());
            prefs.setClockLanguage(mode.equals("chime-en") ? "en" : "zh-Hans");
            prefs.setScreenOrientation(ClockPreferences.ORIENTATION_LANDSCAPE);
            prefs.setDateLunarDualLine(true);
            prefs.setUse24Hour(mode.equals("overview"));
            prefs.setWeatherEnabled(true);
            prefs.setManualWeatherLocation("101010100", "北京", "北京", "北京", 39.90, 116.40);
            prefs.setWeatherLocationMode(ClockPreferences.WEATHER_LOCATION_MANUAL);
            Intent intent = new Intent().setClassName(getTargetContext(), "com.clockmods.pro.ProMainActivity")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getTargetContext().startActivity(intent);
            long deadline = SystemClock.uptimeMillis() + 30000;
            while (activity == null && SystemClock.uptimeMillis() < deadline) SystemClock.sleep(100);
            if (activity == null) throw new IllegalStateException("Pro activity did not resume");
            SystemClock.sleep(18000);
            if (mode.equals("overview")) overview(pkg); else chime(pkg);
            report.putString("result", "PASS: recorded " + mode);
            finish(Activity.RESULT_OK, report);
        } catch (Throwable error) {
            report.putString("failure", android.util.Log.getStackTraceString(error));
            finish(Activity.RESULT_CANCELED, report);
        }
    }
    private void overview(String pkg) throws Exception {
        shot("clock");
        ParcelFileDescriptor recording = record(pkg, 49);
        SystemClock.sleep(4500);
        String[] pages = {"clock", "calendar", "pomodoro", "alarm", "countdown", "stopwatch"};
        for (int page = 1; page < pages.length; page++) {
            final int destination = page;
            runOnMainSync(() -> {
                try {
                    View pager = activity.findViewById(id("pro_pager"));
                    pager.getClass().getMethod("setCurrentItem", int.class, boolean.class)
                            .invoke(pager, destination, true);
                } catch (Exception error) { throw new IllegalStateException(error); }
            });
            SystemClock.sleep(5500);
            shot(pages[page]);
        }
        runOnMainSync(() -> {
            try { activity.getClass().getMethod("showSettings").invoke(activity); }
            catch (Exception error) { throw new IllegalStateException(error); }
        });
        SystemClock.sleep(6000);
        shot("settings");
        sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);
        awaitRecording(recording);
    }
    private void chime(String pkg) throws Exception {
        ParcelFileDescriptor recording = record(pkg, 11);
        SystemClock.sleep(2200);
        Calendar hour = Calendar.getInstance();
        hour.add(Calendar.HOUR_OF_DAY, 1);
        hour.set(Calendar.MINUTE, 0);
        hour.set(Calendar.SECOND, 0);
        hour.set(Calendar.MILLISECOND, 0);
        runOnMainSync(() -> {
            try {
                View chime = activity.findViewById(id("radial_chime"));
                chime.getClass().getMethod("startChime", BackgroundRepository.class, long.class)
                        .invoke(chime, new BackgroundRepository(getTargetContext()), hour.getTimeInMillis());
            } catch (Exception error) { throw new IllegalStateException(error); }
        });
        SystemClock.sleep(2500);
        shot(mode);
        awaitRecording(recording);
    }
    private int id(String name) {
        return getTargetContext().getResources().getIdentifier(name, "id", getTargetContext().getPackageName());
    }
    private ParcelFileDescriptor record(String pkg, int seconds) {
        return getUiAutomation().executeShellCommand("screenrecord --bit-rate 6000000 --time-limit "
                + seconds + " /sdcard/" + pkg + "-" + mode + ".mp4");
    }
    private void awaitRecording(ParcelFileDescriptor recording) throws Exception {
        try (InputStream input = new ParcelFileDescriptor.AutoCloseInputStream(recording)) {
            while (input.read() != -1) { }
        }
    }
    private void shot(String name) throws Exception {
        Bitmap frame = getUiAutomation().takeScreenshot();
        if (frame == null) throw new IllegalStateException("No screenshot for " + name);
        File dir = new File(getTargetContext().getExternalFilesDir(null), "readme");
        dir.mkdirs();
        try (FileOutputStream output = new FileOutputStream(new File(dir, name + ".png"))) {
            frame.compress(Bitmap.CompressFormat.PNG, 100, output);
        }
        frame.recycle();
    }
}
