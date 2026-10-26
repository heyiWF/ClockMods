package com.clockmods.ultimate.settings;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import com.clockmods.R;
import com.clockmods.background.ThemeLibrary;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Real touch and accessibility regression, with isolated preferences restored after the run. */
public final class ThemeLibraryAcceptance extends Instrumentation {
    private int checks;
    private Activity activity;
    private Bundle args;
    @Override public void onCreate(Bundle args) { super.onCreate(args); this.args = args; start(); }
    private void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
    @Override public void onStart() {
        Context context = getTargetContext();
        SharedPreferences general = context.getSharedPreferences("clock_prefs", 0);
        SharedPreferences clock = context.getSharedPreferences("clockmods_ultimate_style", 0);
        Map<String, ?> savedGeneral = new HashMap<>(general.getAll());
        Map<String, ?> savedClock = new HashMap<>(clock.getAll());
        Bundle report = new Bundle();
        int code = Activity.RESULT_OK;
        try {
            boolean calendar = args != null && "calendar".equals(args.getString("kind"));
            String first = calendar ? "calendar.graphite" : "pro.classic";
            String last = calendar ? "calendar.paper" : "glass.atelier";
            String scope = calendar ? "calendar:" + first : first;
            SharedPreferences membership = calendar ? general : clock;
            general.edit().clear().putString("clock_language", "zh")
                    .putBoolean("weather_enabled", false).putBoolean("use_network_time", false)
                    .putInt("screen_orientation", 0).putInt("font_weight__" + scope, 900)
                    .putFloat("time_font_scale__" + scope, 1.6f).commit();
            clock.edit().clear().commit();
            membership.edit().putString(ThemeLibrary.ORDER, first + "|" + last)
                    .putString(calendar ? "calendar_theme" : "style_id", first)
                    .putInt("palette_accent__" + first, 123)
                    .putString("second_motion__" + first, "off").commit();
            Intent intent = new Intent().setClassName(context.getPackageName(),
                    "com.clockmods.ultimate.settings.UltimateSettingsActivity")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    .putExtra("page", calendar ? "calendar_style" : "style")
                    .putExtra("com.clockmods.ultimate.settings.extra.PAGE_ID", calendar ? "calendar_style" : "style");
            activity = startActivitySync(intent);
            SystemClock.sleep(1200);
            String firstName = calendar ? "石墨月历" : context.getString(R.string.ultimate_style_pro_classic_name);
            // Native and Compose expose the same localized name; resolve calendar through resources.
            if (calendar) {
                int name = context.getResources().getIdentifier("ultimate_calendar_theme_graphite_name", "string", context.getPackageName());
                if (name != 0) firstName = context.getString(name);
            }
            ThemeLibrary library = new ThemeLibrary(context, calendar, Arrays.asList(first, last));
            check(library.selected().equals(first), "selected fixture");
            AccessibilityNodeInfo root = root();
            check(root != null, "settings visible");
            Rect screen = new Rect(); root.getBoundsInScreen(screen);
            report.putString("bounds", screen.toShortString());
            // Find the first theme by its resource name, with a stable tag fallback in Compose.
            AccessibilityNodeInfo card = find(root, firstName);
            if (card == null) card = findDescription(root, "选择");
            if (card == null) card = findResource(root, "theme-" + first);
            check(card != null, "first gallery theme: " + firstName + "\n" + texts(root));
            check(findDescription(root, "删除") == null, "delete badge hidden before long press");
            check(findText("管理") == null && findText("编辑") == null, "gallery has no edit button");
            longPress(card);
            snapshot(calendar ? "calendar-delete" : "clock-delete");
            clickContainsDescription("删除");
            check(findText("该主题的样式设置也会一并删除。重新安装后恢复默认设置。") != null, "destructive confirmation explains reset");
            click("取消");
            check(library.isInstalled(first), "cancel preserves membership");
            clickContainsDescription("删除");
            click("删除");
            check(!library.isInstalled(first), "confirmed deletion removes card");
            check(library.selected().equals(last), "selected theme falls back");
            check(!general.contains("font_weight__" + scope), "deleted typography removed");
            check(!general.contains("time_font_scale__" + scope), "deleted size removed");
            if (!calendar) {
                check(!membership.contains("palette_accent__" + first), "deleted palette removed");
                check(!membership.contains("second_motion__" + first), "deleted motion removed");
            }
            root = root();
            AccessibilityNodeInfo survivor = findResource(root, "theme-" + last);
            if (survivor == null) survivor = findDescription(root, "选择");
            check(survivor != null, "surviving card");
            longPress(survivor);
            clickContainsDescription("删除");
            check(findText("至少保留一个主题") != null, "last theme protected by UI");
            click("完成");
            check(library.installed().size() == 1, "last theme retained");
            click("主题商店");
            check(findText("主题商店") != null, "store opens");
            click("推荐");
            click("安装");
            check(library.isInstalled(first), "store reinstalls deleted built-in");
            check(general.getInt("font_weight__" + scope, 0) == 400, "reinstall restores default font weight");
            check(general.getFloat("time_font_scale__" + scope, -1) == 0f, "reinstall restores automatic size");
            clickContainsDescription("收藏主题");
            click("收藏");
            check(library.isFavorite(first), "favorite tab has theme");
            click("已安装");
            check(library.installed().equals(Arrays.asList(last, first)), "reinstalled theme appends after survivor");
            check(findDescription(root(), "向前移动") == null && findDescription(root(), "向后移动") == null, "store has no reorder controls");
            // Activity recreation must preserve the open store and installed list.
            runOnMainSync(() -> activity.recreate());
            SystemClock.sleep(1300);
            check(findText("主题商店") != null, "store survives recreation");
            check(library.installed().equals(Arrays.asList(last, first)), "installation order survives recreation");
            snapshot(calendar ? "calendar-store" : "clock-store");
            report.putString("stream", "PASS: " + checks + " theme library UI checks; bounds=" + screen.toShortString() + "\n");
        } catch (Throwable failure) {
            try { snapshot("failure"); } catch (Exception ignored) { }
            code = Activity.RESULT_CANCELED;
            report.putString("stream", "FAIL: " + android.util.Log.getStackTraceString(failure));
        } finally {
            if (activity != null) runOnMainSync(() -> activity.finish());
            restore(general, savedGeneral); restore(clock, savedClock);
        }
        finish(code, report);
    }
    private void snapshot(String name) throws Exception {
        waitForIdleSync();
        getUiAutomation().waitForIdle(800, 5000);
        android.graphics.Bitmap bitmap = getUiAutomation().takeScreenshot();
        check(bitmap != null, "screenshot available");
        java.io.File file = new java.io.File(getTargetContext().getExternalFilesDir(null), name + ".png");
        try (java.io.FileOutputStream stream = new java.io.FileOutputStream(file)) {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream);
        }
        bitmap.recycle();
    }
    private AccessibilityNodeInfo root() {
        for (int retry = 0; retry < 30; retry++) {
            AccessibilityNodeInfo node = getUiAutomation().getRootInActiveWindow();
            if (node != null) return node;
            SystemClock.sleep(200);
        }
        return null;
    }
    private AccessibilityNodeInfo findText(String text) {
        for (int retry = 0; retry < 30; retry++) {
            AccessibilityNodeInfo found = find(root(), text);
            if (found != null) return found;
            SystemClock.sleep(200);
        }
        return null;
    }
    private AccessibilityNodeInfo find(AccessibilityNodeInfo node, String text) {
        if (node == null) return null;
        if (text.contentEquals(node.getText() == null ? "" : node.getText())) return node;
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo found = find(node.getChild(i), text);
            if (found != null) return found;
        }
        return null;
    }
    private boolean enabled(AccessibilityNodeInfo node) {
        for (AccessibilityNodeInfo current = node; current != null; current = current.getParent())
            if (!current.isEnabled()) return false;
        return true;
    }
    private AccessibilityNodeInfo findDescription(AccessibilityNodeInfo node, String text) {
        if (node == null) return null;
        if (enabled(node) && node.getContentDescription() != null && node.getContentDescription().toString().contains(text)) return node;
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo found = findDescription(node.getChild(i), text);
            if (found != null) return found;
        }
        return null;
    }
    private AccessibilityNodeInfo findResource(AccessibilityNodeInfo node, String id) {
        if (node == null) return null;
        if (id.equals(node.getViewIdResourceName())) return node;
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo found = findResource(node.getChild(i), id);
            if (found != null) return found;
        }
        return null;
    }
    private String texts(AccessibilityNodeInfo node) {
        if (node == null) return "";
        String value = node.getText() + " " + node.getContentDescription() + "\n";
        for (int i = 0; i < node.getChildCount(); i++) value += texts(node.getChild(i));
        return value;
    }
    private void click(String text) {
        AccessibilityNodeInfo node = findText(text);
        check(node != null, "visible action: " + text + "\n" + texts(root()));
        tap(node, false);
    }
    private void clickContainsDescription(String text) {
        AccessibilityNodeInfo node = null;
        for (int retry = 0; retry < 30 && node == null; retry++) {
            node = findDescription(root(), text);
            if (node == null) SystemClock.sleep(200);
        }
        check(node != null, "visible description: " + text + "\n" + texts(root()));
        tap(node, false);
    }
    private void longPress(AccessibilityNodeInfo node) { tap(node, true); }
    private void tap(AccessibilityNodeInfo node, boolean hold) {
        Rect bounds = new Rect(); node.getBoundsInScreen(bounds);
        long now = SystemClock.uptimeMillis();
        MotionEvent down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, bounds.centerX(), bounds.centerY(), 0);
        sendPointerSync(down); down.recycle();
        SystemClock.sleep(hold ? 800 : 80);
        MotionEvent up = MotionEvent.obtain(now, SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, bounds.centerX(), bounds.centerY(), 0);
        sendPointerSync(up); up.recycle();
        waitForIdleSync();
        try { getUiAutomation().waitForIdle(200, 4000); } catch (java.util.concurrent.TimeoutException ignored) { }
        SystemClock.sleep(350);
    }
    @SuppressWarnings("unchecked") private void restore(SharedPreferences preferences, Map<String, ?> values) {
        SharedPreferences.Editor editor = preferences.edit().clear();
        for (Map.Entry<String, ?> entry : values.entrySet()) {
            String key = entry.getKey(); Object value = entry.getValue();
            if (value instanceof String) editor.putString(key, (String)value);
            else if (value instanceof Integer) editor.putInt(key, (Integer)value);
            else if (value instanceof Long) editor.putLong(key, (Long)value);
            else if (value instanceof Float) editor.putFloat(key, (Float)value);
            else if (value instanceof Boolean) editor.putBoolean(key, (Boolean)value);
            else if (value instanceof Set) editor.putStringSet(key, (Set<String>)value);
        }
        editor.commit();
    }
}
