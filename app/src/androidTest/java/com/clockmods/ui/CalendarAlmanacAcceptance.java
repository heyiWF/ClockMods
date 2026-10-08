package com.clockmods.ui;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;

/** Focused real-font and live calendar acceptance, without unrelated calendar navigation tests. */
public final class CalendarAlmanacAcceptance extends Instrumentation {
    private static final String BODY = "出行 · 会友 · 祭祀 · 祈福 · 嫁娶 · 纳采 · 开市 · 交易 · 立券 · 移徙 · 安床 · 扫舍";
    private int checks;
    private File out;
    private int wantedOrientation;
    private String requestedTheme;
    private int requestedOrientation;
    private final Set<Activity> activities = new HashSet<>();
    @Override public void callActivityOnCreate(Activity activity, Bundle state) {
        activities.add(activity); super.callActivityOnCreate(activity, state);
    }
    @Override public void callActivityOnDestroy(Activity activity) {
        activities.remove(activity); super.callActivityOnDestroy(activity);
    }
    @Override public void callActivityOnResume(Activity activity) {
        super.callActivityOnResume(activity);
        activity.setRequestedOrientation(wantedOrientation == 1
                ? android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                : android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        int id = activity.getResources().getIdentifier("pro_pager", "id", activity.getPackageName());
        View pager = activity.findViewById(id);
        if (pager != null) try {
            pager.getClass().getMethod("setCurrentItem", int.class, boolean.class).invoke(pager, 1, false);
        } catch (Exception e) { throw new RuntimeException(e); }
    }
    private void finishActivities() { for (Activity activity : new ArrayList<>(activities)) activity.finish(); }
    private void check(boolean ok, String message) { checks++; if (!ok) throw new AssertionError(message); }
    @Override public void onCreate(Bundle args) {
        super.onCreate(args);
        if (args != null) {
            requestedTheme = args.getString("theme");
            requestedOrientation = Integer.parseInt(args.getString("orientation", "0"));
        }
        start();
    }
    @Override public void onStart() {
        Context context = getTargetContext();
        int originalRotation = android.provider.Settings.System.getInt(context.getContentResolver(), "user_rotation", 0);
        int originalAutoRotation = android.provider.Settings.System.getInt(context.getContentResolver(), "accelerometer_rotation", 1);
        Map<String, Map<String, ?>> original = new HashMap<>();
        Activity activity = null;
        Bundle report = new Bundle(); int result = Activity.RESULT_OK;
        try {
            out = new File(context.getExternalFilesDir(null), "calendar-almanac-acceptance"); out.mkdirs();
            File[] previousImages = out.listFiles();
            if (previousImages != null) for (File file : previousImages)
                if (file.isFile() && file.getName().endsWith(".png")) file.delete();
            java.util.concurrent.atomic.AtomicReference<Throwable> probeError = new java.util.concurrent.atomic.AtomicReference<>();
            runOnMainSync(() -> { try { probeViews(context); } catch (Throwable e) { probeError.set(e); } });
            if (probeError.get() != null) throw probeError.get();
            if (requestedTheme == null || ("graphite".equals(requestedTheme) && requestedOrientation == 1)) {
                try {
                    Class<?> probe = Class.forName("com.clockmods.ultimate.clock.ResponsiveClockProbe");
                    checks += (int) probe.getMethod("run", Context.class, File.class).invoke(null, context, out);
                } catch (ClassNotFoundException absentInMain) { }
            }
            android.accessibilityservice.AccessibilityServiceInfo service = getUiAutomation().getServiceInfo();
            service.flags |= android.accessibilityservice.AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
            getUiAutomation().setServiceInfo(service);
            for (String name : new String[] {"clock_prefs", "clockmods_onboarding"})
                original.put(name, new HashMap<>(context.getSharedPreferences(name, 0).getAll()));
            com.clockmods.background.ClockPreferences settings = new com.clockmods.background.ClockPreferences(context);
            com.clockmods.sdk.style.MarqueeSpec custom = new com.clockmods.sdk.style.MarqueeSpec(80, 2000, 48);
            settings.setCalendarMarquee("calendar.audit", custom);
            check(custom.equals(new com.clockmods.background.ClockPreferences(context)
                    .getCalendarMarquee("calendar.audit")), "motion settings survive host recreation");
            check(!custom.equals(settings.getCalendarMarquee("calendar.other")), "motion settings are theme scoped");
            settings.restoreDefaults();
            check(com.clockmods.sdk.style.MarqueeSpec.DEFAULT.equals(settings.getCalendarMarquee("calendar.audit")),
                    "restore defaults clears scoped motion settings");
            if (!context.getPackageName().endsWith(".pro")) {
                check(settings.getDateFontScale("audit.style") == settings.getSupportingFontScale("audit.style"),
                        "theme date and supporting scales share a balanced default");
            }
            for (Map.Entry<String, Map<String, ?>> entry : original.entrySet()) restore(context, entry.getKey(), entry.getValue());
            boolean pro = context.getPackageName().endsWith(".pro");
            String activityName = pro ? "com.clockmods.pro.ProMainActivity" : "com.clockmods.ultimate.UltimateMainActivity";
            String[] themes = pro ? new String[] {"pro"} : new String[] {"agenda", "graphite", "carbon", "paper"};
            for (int orientation : new int[] {1, 2}) for (String theme : themes) {
                if (requestedTheme != null && !requestedTheme.equals(theme)) continue;
                if (requestedOrientation != 0 && requestedOrientation != orientation) continue;
                wantedOrientation = orientation;
                runOnMainSync(this::finishActivities);
                SystemClock.sleep(300L);
                getUiAutomation().setRotation(orientation == 1
                        ? UiAutomation.ROTATION_FREEZE_0 : UiAutomation.ROTATION_FREEZE_90);
                context.getSharedPreferences("clock_prefs", 0).edit()
                        .putString("calendar_theme", "calendar." + theme).putInt("screen_orientation", orientation)
                        .putFloat("supporting_font_scale__calendar:calendar." + theme, 2f)
                        .putBoolean("weather_enabled", false).putBoolean("show_status_icons", false)
                        .putBoolean("hourly_visual_chime", false).putBoolean("half_hour_visual_chime", false)
                        .putBoolean("use_network_time", false).commit();
                settings.setCalendarMarquee("calendar." + theme, custom);
                context.getSharedPreferences("clockmods_onboarding", 0).edit().putBoolean("completed", true).commit();
                ActivityMonitor monitor = addMonitor(activityName, null, false);
                context.startActivity(new Intent().setClassName(context.getPackageName(), activityName)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        .putExtra("com.clockmods.ultimate.extra.DESTINATION", "calendar"));
                activity = monitor.waitForActivityWithTimeout(10000L); removeMonitor(monitor);
                check(activity != null, theme + " activity started");
                final Activity current = activity;
                runOnMainSync(() -> {
                    int id = context.getResources().getIdentifier("pro_pager", "id", context.getPackageName());
                    View pager = current.findViewById(id);
                    if (pager != null) try {
                        pager.getClass().getMethod("setCurrentItem", int.class, boolean.class).invoke(pager, 1, false);
                    } catch (Exception e) { throw new RuntimeException(e); }
                });
                AccessibilityNodeInfo node = awaitAlmanac(theme, orientation, activityName);
                if (theme.equals("agenda") && orientation == 1) observeLap(node);
            }
            report.putInt("calendar_almanac_checks", checks);
            report.putString("evidence", out.toString());
        } catch (Throwable error) {
            result = Activity.RESULT_CANCELED; report.putString("failure", android.util.Log.getStackTraceString(error));
        } finally {
            runOnMainSync(this::finishActivities);
            for (Map.Entry<String, Map<String, ?>> entry : original.entrySet()) restore(context, entry.getKey(), entry.getValue());
            getUiAutomation().setRotation(originalRotation);
            if (originalAutoRotation != 0) getUiAutomation().setRotation(UiAutomation.ROTATION_UNFREEZE);
        }
        finish(result, report);
    }

    /** Wait for visible coloured almanac ink, not a startup frame or the date-only footer phase. */
    private AccessibilityNodeInfo awaitAlmanac(String theme, int orientation, String activityName) throws Exception {
        long deadline = SystemClock.uptimeMillis() + 30000L;
        int visibleFrames = 0;
        while (SystemClock.uptimeMillis() < deadline) {
            getUiAutomation().setRotation(orientation == 1
                    ? UiAutomation.ROTATION_FREEZE_0 : UiAutomation.ROTATION_FREEZE_90);
            runOnMainSync(() -> {
                for (Activity running : new ArrayList<>(activities)) {
                    if (running.isFinishing() || running.isDestroyed()) continue;
                    running.setRequestedOrientation(orientation == 1
                            ? android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            : android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                }
            });
            AccessibilityNodeInfo node = findAlmanac(getUiAutomation().getRootInActiveWindow(), theme.equals("agenda"));
            if (node != null) {
                Bitmap screen = getUiAutomation().takeScreenshot();
                Rect bounds = new Rect(); node.getBoundsInScreen(bounds);
                int coloured = 0;
                for (int y = Math.max(0, bounds.top); y < Math.min(screen.getHeight(), bounds.bottom); y += 2)
                    for (int x = Math.max(0, bounds.left); x < Math.min(screen.getWidth(), bounds.right); x += 2) {
                        int pixel = screen.getPixel(x, y);
                        int r = Color.red(pixel), g = Color.green(pixel), b = Color.blue(pixel);
                        if ((g > r + 20 && g > b + 20) || (r > g + 20 && r > b + 20)) coloured++;
                    }
                boolean ready = (screen.getWidth() > screen.getHeight()) == (orientation == 2) && coloured > 20;
                visibleFrames = ready ? visibleFrames + 1 : 0;
                if (visibleFrames >= 2) {
                    save(screen, theme + "-" + orientation + ".png"); screen.recycle();
                    check(true, theme + " dotted almanac visible in orientation " + orientation);
                    return node;
                }
                screen.recycle();
            } else {
                visibleFrames = 0;
                // An orientation recreation can happen before the initial navigation effect runs.
                runOnMainSync(() -> {
                    for (Activity running : new ArrayList<>(activities)) {
                        if (!running.getClass().getName().equals(activityName) || running.isFinishing() || running.isDestroyed()) continue;
                        callActivityOnNewIntent(running, new Intent(running.getIntent())
                                .putExtra("com.clockmods.ultimate.extra.DESTINATION", "calendar"));
                    }
                });
            }
            SystemClock.sleep(500L);
        }
        Bitmap failed = getUiAutomation().takeScreenshot();
        save(failed, theme + "-" + orientation + "-failed.png"); failed.recycle();
        throw new AssertionError(theme + " dotted almanac did not become visible in orientation " + orientation);
    }

    private void probeViews(Context context) throws Exception {
        for (int size : new int[] {28, 56}) for (String prefix : new String[] {"宜 ", "忌 ", "Good: ", "Avoid: "}) {
            Paint glyph = new Paint(Paint.ANTI_ALIAS_FLAG); glyph.setTextSize(size);
            Bitmap badge = Bitmap.createBitmap(size * 3, size * 3, Bitmap.Config.ARGB_8888);
            AlmanacBadge.draw(new Canvas(badge), prefix, size * 1.5f, size * 1.5f,
                    size * 1.55f, Color.GREEN, glyph, new Paint(Paint.ANTI_ALIAS_FLAG), new Rect());
            check(Color.alpha(badge.getPixel(0, 0)) == 0, "circular badge has no rectangular background");
            Rect bounds = new Rect(); String text = prefix.trim(); glyph.getTextBounds(text, 0, text.length(), bounds);
            check(bounds.width() <= size * 1.55f * .72f + 1f, "localized glyph fits its badge"); badge.recycle();
            for (String name : new String[] {"AlmanacLineView", "CalendarFooterCarouselView"}) {
                Class<?> type;
                try { type = Class.forName("com.clockmods.ui." + name); } catch (ClassNotFoundException unused) { continue; }
                View view = (View) type.getConstructor(Context.class).newInstance(context);
                type.getMethod("setTextSizePx", float.class).invoke(view, (float) size);
                type.getMethod("setActive", boolean.class).invoke(view, true);
                if (name.equals("AlmanacLineView")) type.getMethod("setLine", String.class, String.class).invoke(view, prefix + BODY, prefix);
                else {
                    Class<?> itemType = Class.forName("com.clockmods.ui.CalendarFooterCarouselView$Item");
                    Object item = itemType.getConstructor(String.class, int.class).newInstance(prefix + BODY, Color.GREEN);
                    itemType.getMethod("withPinnedPrefix", String.class).invoke(item, prefix);
                    type.getMethod("setItems", List.class).invoke(view, Collections.singletonList(item));
                }
                int width = 320, height = size * 2;
                view.layout(0, 0, width, height);
                String startField = name.equals("AlmanacLineView") ? "shownAt" : "cycleStartedAt";
                Bitmap head = frame(view, startField, 200L, width, height);
                Bitmap pause = frame(view, startField, 800L, width, height);
                check(head.sameAs(pause), name + " pauses at the head"); pause.recycle();
                Bitmap moving = frame(view, startField, 1700L, width, height);
                check(!head.sameAs(moving), name + " scrolls without reducing type"); moving.recycle();
                if (name.equals("AlmanacLineView")) {
                    Paint paint = new Paint(); paint.setTextSize(size);
                    long cycle = 1000L + CalendarMarqueeTiming.scrollMillis(
                            CalendarMarqueeTiming.loopDistance(paint.measureText(BODY), size,
                                    context.getResources().getDisplayMetrics().density), context.getResources().getDisplayMetrics().density);
                    Bitmap lap = frame(view, startField, cycle + 300L, width, height);
                    check(head.sameAs(lap), "belt returns seamlessly then pauses"); lap.recycle();
                } else {
                    Method hold = type.getDeclaredMethod("holdDurationFor", Class.forName("com.clockmods.ui.CalendarFooterCarouselView$Item")); hold.setAccessible(true);
                    Field items = type.getDeclaredField("items"); items.setAccessible(true);
                    long duration = (long) hold.invoke(view, ((List<?>) items.get(view)).get(0));
                    Bitmap tail = frame(view, startField, duration - 800L, width, height);
                    Bitmap tailPause = frame(view, startField, duration - 100L, width, height);
                    check(tail.sameAs(tailPause), "footer holds its readable tail"); tailPause.recycle(); tail.recycle();
                }
                save(head, name + "-" + size + "-" + prefix.trim().replace(':', '_') + ".png"); head.recycle();
            }
        }
    }
    private Bitmap frame(View view, String name, long phase, int width, int height) throws Exception {
        Field field = view.getClass().getDeclaredField(name); field.setAccessible(true);
        field.setLong(view, SystemClock.uptimeMillis() - phase); view.invalidate();
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888); view.draw(new Canvas(bitmap)); return bitmap;
    }
    private void observeLap(AccessibilityNodeInfo node) throws Exception {
        Rect bounds = new Rect(); node.getBoundsInScreen(bounds);
        Bitmap previous = null; long stationary = 0L; boolean moved = false, paused = false, resumed = false;
        long end = SystemClock.uptimeMillis() + 45000L;
        while (SystemClock.uptimeMillis() < end) {
            Bitmap screen = getUiAutomation().takeScreenshot();
            int left = Math.max(0, bounds.left), top = Math.max(0, bounds.top);
            int right = Math.min(screen.getWidth(), bounds.right), bottom = Math.min(screen.getHeight(), bounds.bottom);
            check(right > left && bottom > top, "almanac stays inside the live screen");
            Bitmap crop = Bitmap.createBitmap(screen, left, top, right - left, bottom - top); screen.recycle();
            if (previous != null) {
                if (crop.sameAs(previous)) {
                    if (stationary == 0L) stationary = SystemClock.uptimeMillis();
                    if (moved && SystemClock.uptimeMillis() - stationary >= 400L) { paused = true; save(crop, "agenda-lap-pause.png"); }
                } else {
                    moved = true;
                    if (paused) { resumed = true; crop.recycle(); previous.recycle(); break; }
                    stationary = 0L;
                }
                previous.recycle();
            }
            previous = crop; SystemClock.sleep(100L);
        }
        if (!resumed && previous != null) previous.recycle();
        check(moved && paused && resumed, "live almanac moves, pauses after a complete lap, then resumes"
                + " (moved=" + moved + ", paused=" + paused + ", resumed=" + resumed + ")");
    }
    private AccessibilityNodeInfo findAlmanac(AccessibilityNodeInfo node, boolean agenda) {
        if (node == null) return null;
        String description = String.valueOf(node.getContentDescription());
        String text = String.valueOf(node.getText()); String id = String.valueOf(node.getViewIdResourceName());
        boolean dotted = description.contains(" · ") || text.contains(" · ");
        boolean almanac = description.startsWith("宜") || description.startsWith("忌")
                || description.startsWith("Good:") || description.startsWith("Avoid:")
                || id.contains("almanac:") || id.contains("calendar_agenda_suitable") || id.contains("calendar_agenda_avoid");
        if (dotted && (!agenda || almanac)) return node;
        for (int i = 0; i < node.getChildCount(); i++) { AccessibilityNodeInfo found = findAlmanac(node.getChild(i), agenda); if (found != null) return found; }
        return null;
    }
    private void save(Bitmap bitmap, String name) throws IOException {
        try (FileOutputStream stream = new FileOutputStream(new File(out, name))) { bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream); }
    }
    private void restore(Context context, String name, Map<String, ?> values) {
        SharedPreferences.Editor edit = context.getSharedPreferences(name, 0).edit().clear();
        for (Map.Entry<String, ?> value : values.entrySet()) {
            Object v = value.getValue(); String k = value.getKey();
            if (v instanceof String) edit.putString(k, (String)v); else if (v instanceof Boolean) edit.putBoolean(k, (Boolean)v);
            else if (v instanceof Integer) edit.putInt(k, (Integer)v); else if (v instanceof Float) edit.putFloat(k, (Float)v);
            else if (v instanceof Long) edit.putLong(k, (Long)v); else if (v instanceof Set) edit.putStringSet(k, (Set<String>)v);
        }
        edit.commit();
    }
}
