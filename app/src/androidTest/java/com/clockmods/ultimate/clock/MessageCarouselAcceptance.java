package com.clockmods.ultimate.clock;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.SystemClock;
import com.clockmods.sdk.clock.*;
import com.clockmods.background.ClockPreferences;
import com.clockmods.weather.*;
import java.io.File;
import java.io.FileOutputStream;
import java.util.*;

/** Real Android font/Canvas checks plus two complete cycles in the installed clock screen. */
public final class MessageCarouselAcceptance {
    private static final String MESSAGE = "长留言保持原有字号，匀速滚动读完每一个文字，最后几个字也要清楚可见，继续播放天气，再次循环。末尾可见";
    private static int checks;
    private static void check(boolean ok, String description) {
        checks++;
        if (!ok) throw new AssertionError(description);
    }
    public static void run(Instrumentation instrumentation) {
        checks = 0;
        Context context = instrumentation.getTargetContext();
        Bundle report = new Bundle();
        int result = Activity.RESULT_OK;
        Activity activity = null;
        Map<String, Map<String, ?>> original = new HashMap<>();
        File out = new File(context.getExternalFilesDir(null), "message-carousel-acceptance");
        out.mkdirs();
        File[] prior = out.listFiles();
        if (prior != null) for (File file : prior) if (file.getName().endsWith(".png")) file.delete();
        try {
            ClockStyleRegistry registry = UltimateClockStyles.createRegistry();
            String[] styles = {"glass.atelier", "noir.instrument", "paper.station", "orbit.neon",
                    "digital.grid", "typographic.poster", "ultimate.dual_blocks", "ultimate.orbit",
                    "ultimate.bubbles", "ultimate.blend", "ultimate.ribbon"};
            for (String id : styles) for (int orientation = 0; orientation < 2; orientation++) {
                int width = orientation == 0 ? 720 : 1280, height = orientation == 0 ? 1280 : 720;
                ClockStyle style = registry.find(id);
                ClockRenderContext rc = new ClockRenderContext(0f, 0f, width, height, 2f, 2f, 1787633430000L);
                for (float scale : new float[] {1f, 2f}) {
                    MessageMarqueeLayout layout = new MessageMarqueeLayout();
                    ClockState state = messageState(0L, scale, ClockState.TimeTransition.FADE, 1f, false);
                    Bitmap head = render(style, rc, state, layout, width, height);
                    check(layout.distance > 0f, id + " measured overflowing message");
                    float size = layout.textSize;
                    long duration = layout.displayMillis;
                    Bitmap middle = render(style, rc, messageState(duration / 2, scale,
                            ClockState.TimeTransition.FADE, 1f, false), layout, width, height);
                    check(!head.sameAs(middle), id + " scrolling changes pixels");
                    head.recycle(); middle.recycle();
                    Bitmap tail = render(style, rc, messageState(duration - 1000L, scale,
                            ClockState.TimeTransition.FADE, 1f, false), layout, width, height);
                    check(Math.abs(layout.offset - layout.distance) < .1f, id + " clears tail fade");
                    check(Math.abs(layout.textSize - size) < .01f, id + " retains font size");
                    float tailX = layout.availableWidth * MessageMarqueeLayout.EDGE_FRACTION
                            + layout.textWidth - layout.offset;
                    check(tailX <= layout.availableWidth * (1f - MessageMarqueeLayout.EDGE_FRACTION) + .1f,
                            id + " last glyph outside gradient");
                    if (scale == 1f) save(tail, new File(out, id + "-" + orientation + "-tail.png"));
                    tail.recycle();
                    for (ClockState.TimeTransition transition : ClockState.TimeTransition.values()) {
                        for (boolean outgoing : new boolean[] {false, true}) {
                            if (!outgoing) layout.textSize = 0f;
                            Bitmap first = render(style, rc, messageState(duration, scale, transition,
                                    .25f, outgoing), layout, width, height);
                            Bitmap second = render(style, rc, messageState(duration, scale, transition,
                                    .75f, outgoing), layout, width, height);
                            check(!first.sameAs(second), id + " " + transition + " animation " + outgoing);
                            if (!outgoing) check(Math.abs(layout.textSize - size) < .01f,
                                    id + " animation retains message size");
                            first.recycle(); second.recycle();
                        }
                    }
                    WeatherCarousel carousel = new WeatherCarousel(Arrays.asList("北京 26℃ 晴", MESSAGE), true);
                    carousel.advance(0L, duration, 800L);
                    check(carousel.advance(3000L, duration, 800L) && carousel.messageActive(), id + " weather to message");
                    check(!carousel.advance(3800L + duration - 1L, duration, 800L), id + " full scroll reading window");
                    check(carousel.advance(3800L + duration, duration, 800L) && !carousel.messageActive(), id + " returns to weather");
                    check(carousel.advance(7600L + duration, duration, 800L) && carousel.messageActive(), id + " repeats message");
                }
            }
            // Exercise the actual installed screen with persisted settings and cached weather.
            for (String name : new String[] {"clock_prefs", "clockmods_ultimate_style", "weather_cache", "clockmods_onboarding"})
                original.put(name, new HashMap<>(context.getSharedPreferences(name, 0).getAll()));
            ClockPreferences settings = new ClockPreferences(context);
            settings.setCustomMessage(MESSAGE);
            context.getSharedPreferences("clock_prefs", 0).edit().putBoolean("weather_enabled", true)
                    .putBoolean("weather_detailed", false).putBoolean("show_seconds", false)
                    .putBoolean("show_status_icons", false).putBoolean("use_network_time", false)
                    .putBoolean("hourly_visual_chime", false).putBoolean("half_hour_visual_chime", false)
                    .putString("weather_location_mode", "manual").putString("weather_location_id", "101010100")
                    .putInt("screen_orientation", 2).commit();
            context.getSharedPreferences("clockmods_onboarding", 0).edit().putBoolean("completed", true).commit();
            new WeatherRepository(context).save(new WeatherModels.WeatherDisplayData("101010100", "北京", "北京", "晴", "100", "26", System.currentTimeMillis()), "manual");
            context.getSharedPreferences("clockmods_ultimate_style", 0).edit().putString("style_id", "glass.atelier")
                    .putString("weather_transition__glass.atelier", "slide_right").putBoolean("digit_animation__glass.atelier", false).commit();
            Intent intent = new Intent().setClassName(context.getPackageName(), "com.clockmods.ultimate.UltimateMainActivity")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(
                    "com.clockmods.ultimate.UltimateMainActivity", null, false);
            context.startActivity(intent);
            activity = monitor.waitForActivityWithTimeout(10000L);
            instrumentation.removeMonitor(monitor);
            check(activity != null, "live clock activity started");
            SystemClock.sleep(2000L);
            int messageEntries = 0, weatherReturns = 0;
            boolean wasMessage = false;
            for (int i = 0; i < 90; i++) {
                SystemClock.sleep(1000L);
                boolean message = containsMessage(instrumentation.getUiAutomation().getRootInActiveWindow());
                if (message && !wasMessage) messageEntries++;
                if (!message && wasMessage && containsDescription(
                        instrumentation.getUiAutomation().getRootInActiveWindow(), "北京")) weatherReturns++;
                wasMessage = message;
                Bitmap screen = instrumentation.getUiAutomation().takeScreenshot();
                check(screen != null, "live screen " + i);
                save(screen, new File(out, String.format(Locale.US, "live-%02d.png", i)));
                screen.recycle();
                if (messageEntries >= 2 && weatherReturns >= 1) break;
            }
            check(messageEntries >= 2 && weatherReturns >= 1, "live weather/message/weather/message cycle");
            report.putInt("live_message_entries", messageEntries);
            report.putInt("live_weather_returns", weatherReturns);
            report.putInt("message_carousel_checks", checks);
            report.putString("evidence", out.toString());
        } catch (Throwable error) {
            result = Activity.RESULT_CANCELED;
            report.putString("failure", android.util.Log.getStackTraceString(error));
        } finally {
            final Activity launched = activity;
            if (launched != null) instrumentation.runOnMainSync(launched::finish);
            for (Map.Entry<String, Map<String, ?>> entry : original.entrySet()) {
                SharedPreferences.Editor edit = context.getSharedPreferences(entry.getKey(), 0).edit().clear();
                for (Map.Entry<String, ?> value : entry.getValue().entrySet()) {
                    Object v = value.getValue(); String k = value.getKey();
                    if (v instanceof String) edit.putString(k, (String) v);
                    else if (v instanceof Boolean) edit.putBoolean(k, (Boolean) v);
                    else if (v instanceof Integer) edit.putInt(k, (Integer) v);
                    else if (v instanceof Long) edit.putLong(k, (Long) v);
                    else if (v instanceof Float) edit.putFloat(k, (Float) v);
                    else if (v instanceof Set) edit.putStringSet(k, (Set<String>) v);
                }
                edit.commit();
            }
        }
        instrumentation.finish(result, report);
    }
    private static boolean containsMessage(android.view.accessibility.AccessibilityNodeInfo node) {
        return containsDescription(node, MESSAGE);
    }
    private static boolean containsDescription(android.view.accessibility.AccessibilityNodeInfo node, String text) {
        if (node == null) return false;
        if (node.getContentDescription() != null && node.getContentDescription().toString().contains(text)) return true;
        for (int i = 0; i < node.getChildCount(); i++) if (containsDescription(node.getChild(i), text)) return true;
        return false;
    }
    private static ClockState messageState(long elapsed, float scale, ClockState.TimeTransition transition,
            float progress, boolean outgoing) {
        return ClockState.builder(1787633430000L).showSeconds(false).dateText("2026年9月30日 星期三")
                .weatherText(outgoing ? "北京 26℃ 晴" : MESSAGE).supportingScale(scale)
                .messageActive(!outgoing).messageContinuous(false).messageScrollElapsedMillis(elapsed)
                .previousWeatherText(outgoing ? MESSAGE : "北京 26℃ 晴")
                .previousMessageActive(outgoing).previousMessageScrollElapsedMillis(elapsed)
                .weatherTransition(transition).weatherTransitionProgress(progress).build();
    }
    private static Bitmap render(ClockStyle style, ClockRenderContext rc, ClockState state,
            MessageMarqueeLayout layout, int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        UltimateClockStyles.renderWithMessageLayout(style.getRenderer(), new Canvas(bitmap), rc,
                state, style.getThemeTokens(), layout);
        return bitmap;
    }
    private static void save(Bitmap bitmap, File file) throws Exception {
        try (FileOutputStream stream = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        }
    }
}
