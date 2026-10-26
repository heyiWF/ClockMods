package com.clockmods.ui;

import android.app.Activity;
import android.app.Dialog;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;
import com.clockmods.R;
import com.clockmods.background.ClockPreferences;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/** Exercises the real choose-image click, activity result, async import and Apply path. */
@android.annotation.TargetApi(18)
public final class ImagePickerSettingsAcceptance extends Instrumentation {
    private int checks;
    private Activity activity;
    @Override public void onCreate(Bundle args) { super.onCreate(args); start(); }
    private void check(boolean ok, String message) { checks++; if (!ok) throw new AssertionError(message); }
    @Override public void onStart() {
        Context context = getTargetContext();
        SharedPreferences prefs = context.getSharedPreferences("clock_prefs", 0);
        Map<String, ?> saved = new HashMap<>(prefs.getAll());
        File fixture = new File(context.getCacheDir(), "picker-regression.png");
        File background = new File(context.getFilesDir(), "clock_background.image");
        File backup = new File(context.getFilesDir(), "picker-regression.backup");
        boolean hadImage = background.isFile();
        int oldAutoRotation = android.provider.Settings.System.getInt(context.getContentResolver(),
                android.provider.Settings.System.ACCELEROMETER_ROTATION, 0);
        int oldRotation = android.provider.Settings.System.getInt(context.getContentResolver(),
                android.provider.Settings.System.USER_ROTATION, 0);
        Bundle report = new Bundle(); int code = Activity.RESULT_OK;
        try {
            if (hadImage && !background.renameTo(backup)) throw new AssertionError("backup background");
            Bitmap bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888);
            bitmap.eraseColor(Color.BLUE);
            try (FileOutputStream stream = new FileOutputStream(fixture)) { bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream); }
            bitmap.recycle();
            for (int orientation : new int[]{1, 2}) {
                getUiAutomation().setRotation(orientation == 1 ? 0 : 1);
                prefs.edit().putInt("screen_orientation", orientation)
                        .putBoolean("weather_enabled", false).putBoolean("use_network_time", false)
                        .putFloat("time_font_scale", 0.8f).putBoolean("bold_text", false).commit();
                launch(orientation);
                onMain(() -> invoke(activity, "showSettings"));
                Dialog dialog = (Dialog) field(activity, "settingsDialog").get(activity);
                onMain(() -> {
                    Object slider = field(dialog, "timeSizeBar").get(dialog);
                    if (slider instanceof SeekBar) ((SeekBar) slider).setProgress(90);
                    else slider.getClass().getMethod("setValue", float.class).invoke(slider, 110f);
                    ((CompoundButton) field(dialog, "boldTextSwitch").get(dialog)).setChecked(true);
                });
                check(!new ClockPreferences(context).isBoldText(), "draft remains unapplied before picker");
                pickRealAndCancel(dialog);
                verifyDraft(dialog, "real picker cancellation");
                pick(dialog, new ActivityResult(Activity.RESULT_CANCELED, null));
                verifyDraft(dialog, "cancel");
                // A failed import must preserve the same draft too.
                pick(dialog, new ActivityResult(Activity.RESULT_OK,
                        new Intent().setData(Uri.fromFile(new File(context.getCacheDir(), "missing-image.png")))));
                SystemClock.sleep(400); verifyDraft(dialog, "invalid image");
                pick(dialog, new ActivityResult(Activity.RESULT_OK, new Intent().setData(Uri.fromFile(fixture))));
                boolean imported = false;
                for (int attempt = 0; attempt < 100; attempt++) {
                    final boolean[] ready = {false};
                    onMain(() -> {
                        ViewGroup controls = (ViewGroup) field(dialog, "imageControls").get(dialog);
                        View preview = controls.getChildAt(0);
                        ready[0] = preview instanceof android.widget.ImageView
                                && ((android.widget.ImageView) preview).getDrawable() != null;
                    });
                    if (ready[0]) { imported = true; break; }
                    SystemClock.sleep(50);
                }
                check(imported, "selected image is imported and preview refreshed");
                verifyDraft(dialog, "successful import");
                check(!new ClockPreferences(context).isBoldText(), "image import does not apply unrelated settings");
                onMain(() -> invoke(dialog, "applySelection"));
                check(new ClockPreferences(context).isBoldText(), "Apply saves the original bold draft");
                check(Math.abs(new ClockPreferences(context).getTimeFontScale() - 1.1f) < 0.001f,
                        "Apply saves the original oversized font draft");
                onMain(() -> check(!dialog.isShowing(), "only Apply dismisses settings"));
                finishActivity();
                background.delete();
            }
            report.putString("stream", "PASS: " + checks + " image picker/draft checks in portrait and landscape\n");
        } catch (Throwable error) {
            code = Activity.RESULT_CANCELED;
            report.putString("stream", "FAIL: " + android.util.Log.getStackTraceString(error));
        } finally {
            try { finishActivity(); } catch (Throwable ignored) { }
            fixture.delete(); background.delete(); if (hadImage) backup.renameTo(background);
            SharedPreferences.Editor edit = prefs.edit().clear();
            for (Map.Entry<String, ?> entry : saved.entrySet()) {
                Object value = entry.getValue(); String key = entry.getKey();
                if (value instanceof String) edit.putString(key, (String) value);
                else if (value instanceof Boolean) edit.putBoolean(key, (Boolean) value);
                else if (value instanceof Integer) edit.putInt(key, (Integer) value);
                else if (value instanceof Long) edit.putLong(key, (Long) value);
                else if (value instanceof Float) edit.putFloat(key, (Float) value);
            }
            edit.commit();
            getUiAutomation().setRotation(oldAutoRotation == 1 ? -2 : oldRotation);
        }
        finish(code, report);
    }
    private void launch(int orientation) throws Exception {
        String name = getTargetContext().getPackageName().endsWith(".pro")
                ? "com.clockmods.pro.ProMainActivity" : "com.clockmods.MainActivity";
        ActivityMonitor monitor = addMonitor(name, null, false);
        try {
            getTargetContext().startActivity(new Intent().setClassName(getTargetContext(), name)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            activity = monitor.waitForActivityWithTimeout(10000);
            check(activity != null, "host activity launches");
            for (int attempt = 0; attempt < 100; attempt++) {
                if (monitor.getLastActivity() != null) activity = monitor.getLastActivity();
                final boolean[] ready = {false};
                onMain(() -> ready[0] = !activity.isDestroyed() && activity.hasWindowFocus()
                        && activity.getResources().getConfiguration().orientation == orientation);
                if (ready[0]) return;
                SystemClock.sleep(100);
            }
            throw new AssertionError("host orientation=" + activity.getResources().getConfiguration().orientation
                    + ", requested=" + orientation + ", focus=" + activity.hasWindowFocus()
                    + ", destroyed=" + activity.isDestroyed());
        } finally { removeMonitor(monitor); }
    }
    private void pickRealAndCancel(Dialog dialog) throws Exception {
        onMain(() -> {
            View button = findText(dialog.getWindow().getDecorView(), getTargetContext().getString(R.string.choose_image));
            check(button != null && button.performClick(), "real image picker opens");
        });
        boolean opened = false;
        for (int attempt = 0; attempt < 100; attempt++) {
            android.view.accessibility.AccessibilityNodeInfo root = getUiAutomation().getRootInActiveWindow();
            if (root != null && !getTargetContext().getPackageName().contentEquals(root.getPackageName())) {
                opened = true; break;
            }
            SystemClock.sleep(100);
        }
        check(opened, "external picker becomes foreground");
        getUiAutomation().performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK);
        boolean returned = false;
        for (int attempt = 0; attempt < 100; attempt++) {
            android.view.accessibility.AccessibilityNodeInfo root = getUiAutomation().getRootInActiveWindow();
            if (root != null && getTargetContext().getPackageName().contentEquals(root.getPackageName())) {
                returned = true; break;
            }
            SystemClock.sleep(100);
        }
        check(returned, "real picker returns to settings host");
        waitForIdleSync();
    }
    private void pick(Dialog dialog, ActivityResult result) throws Exception {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_OPEN_DOCUMENT); filter.addAction(Intent.ACTION_GET_CONTENT);
        filter.addAction("android.provider.action.PICK_IMAGES");
        filter.addCategory(Intent.CATEGORY_OPENABLE); filter.addDataType("image/*");
        ActivityMonitor monitor = addMonitor(filter, result, true);
        try {
            onMain(() -> {
                View button = findText(dialog.getWindow().getDecorView(), getTargetContext().getString(R.string.choose_image));
                check(button != null, "choose-image control exists");
                check(button.performClick(), "choose-image click handled");
            });
            waitForIdleSync();
            check(monitor.getHits() == 1, "host launches image picker exactly once");
        } finally { removeMonitor(monitor); }
    }
    private void verifyDraft(Dialog dialog, String result) throws Exception {
        onMain(() -> {
            check(dialog == field(activity, "settingsDialog").get(activity) && dialog.isShowing(), result + " retains the same dialog");
            Object slider = field(dialog, "timeSizeBar").get(dialog);
            float value = slider instanceof SeekBar ? ((SeekBar) slider).getProgress() + 20
                    : ((Number) slider.getClass().getMethod("getValue").invoke(slider)).floatValue();
            check(value == 110f, result + " preserves font draft");
            check(((CompoundButton) field(dialog, "boldTextSwitch").get(dialog)).isChecked(), result + " preserves bold draft");
        });
    }
    private void finishActivity() throws Exception {
        if (activity != null) { onMain(() -> activity.finish()); waitForIdleSync(); activity = null; }
    }
    private static View findText(View view, String text) {
        if (view instanceof TextView && text.contentEquals(((TextView) view).getText())) return view;
        if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
            View found = findText(((ViewGroup) view).getChildAt(i), text); if (found != null) return found;
        }
        return null;
    }
    private static Field field(Object object, String name) throws Exception {
        Field result = object.getClass().getDeclaredField(name); result.setAccessible(true); return result;
    }
    private static void invoke(Object object, String name) throws Exception {
        Method method = object.getClass().getDeclaredMethod(name); method.setAccessible(true); method.invoke(object);
    }
    private interface Action { void run() throws Exception; }
    private void onMain(Action action) throws Exception {
        Throwable[] failure = {null};
        runOnMainSync(() -> { try { action.run(); } catch (Throwable error) { failure[0] = error; } });
        if (failure[0] != null) throw new AssertionError("main-thread check", failure[0]);
    }
}
