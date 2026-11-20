package com.clockmods.ultimate.navigation;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.*;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.*;
import android.widget.FrameLayout;
import android.widget.ImageView;
import androidx.activity.ComponentActivity;
import java.lang.ref.WeakReference;
import java.util.*;

/** Opt-in cross-activity preview using public touch/exclusion APIs, never synthetic system Back progress. */
public final class CompatBackGesture implements Application.ActivityLifecycleCallbacks {
    private static final String KEY = "compat_activity_swipe";
    private static CompatBackGesture instance;
    private final ArrayList<WeakReference<Activity>> history = new ArrayList<>();
    private final LinkedHashMap<Integer, Snapshot> snapshots = new LinkedHashMap<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final SharedPreferences preferences;
    private final SharedPreferences.OnSharedPreferenceChangeListener listener;
    private int captureGeneration;

    private CompatBackGesture(Application app) {
        preferences = app.getSharedPreferences("navigation_settings", 0);
        listener = (prefs, key) -> {
            if (!KEY.equals(key)) return;
            if (!enabled(app)) { captureGeneration++; snapshots.clear(); }
            for (WeakReference<Activity> ref : history) {
                Activity activity = ref.get();
                if (activity != null) activity.getWindow().getDecorView().post(() -> install(activity));
            }
        };
        preferences.registerOnSharedPreferenceChangeListener(listener);
        app.registerActivityLifecycleCallbacks(this);
        app.registerComponentCallbacks(new android.content.ComponentCallbacks2() {
            @Override public void onTrimMemory(int level) {
                if (level >= TRIM_MEMORY_UI_HIDDEN) { captureGeneration++; snapshots.clear(); }
            }
            @Override public void onLowMemory() { captureGeneration++; snapshots.clear(); }
            @Override public void onConfigurationChanged(android.content.res.Configuration config) { captureGeneration++; snapshots.clear(); }
        });
    }
    public static void initialize(Application app) { if (instance == null) instance = new CompatBackGesture(app); }
    public static boolean enabled(Context context) { return context.getSharedPreferences("navigation_settings", 0).getBoolean(KEY, false); }
    public static void setEnabled(Context context, boolean enabled) { context.getSharedPreferences("navigation_settings", 0).edit().putBoolean(KEY, enabled).apply(); }
    private static boolean supported(Activity activity) {
        return activity instanceof ComponentActivity && !activity.isFinishing() && !activity.isDestroyed()
                && !activity.isInMultiWindowMode()
                && !activity.getClass().getSimpleName().equals("AlarmRingingActivity")
                && (activity.getWindow().getAttributes().flags & WindowManager.LayoutParams.FLAG_SECURE) == 0;
    }
    private Activity previous(Activity current) {
        Activity previous = null;
        for (WeakReference<Activity> ref : history) {
            Activity activity = ref.get();
            if (activity == current) return previous;
            if (activity != null && !activity.isFinishing() && !activity.isDestroyed() && activity.getTaskId() == current.getTaskId()) previous = activity;
        }
        return null;
    }
    private Snapshot preview(Activity activity, int width, int height) {
        if (!enabled(activity) || !supported(activity)
                || ((ComponentActivity) activity).getOnBackPressedDispatcher().hasEnabledCallbacks()) return null;
        Activity previous = previous(activity);
        if (previous == null || !supported(previous)) return null;
        Snapshot snapshot = snapshots.get(System.identityHashCode(previous));
        if (snapshot == null || snapshot.width != width || snapshot.height != height) return null;
        return snapshot;
    }
    private void capture(Activity activity) {
        if (!enabled(activity) || !supported(activity)) return;
        View content = activity.findViewById(android.R.id.content);
        int width = content.getWidth(), height = content.getHeight();
        if (width <= 0 || height <= 0) return;
        int generation = captureGeneration;
        float scale = Math.min(1f, 720f / Math.max(width, height));
        Bitmap bitmap = Bitmap.createBitmap(Math.max(1, Math.round(width * scale)), Math.max(1, Math.round(height * scale)), Bitmap.Config.ARGB_8888);
        int[] location = new int[2]; content.getLocationInWindow(location);
        Rect rect = new Rect(location[0], location[1], location[0] + width, location[1] + height);
        int id = System.identityHashCode(activity);
        try {
            PixelCopy.request(activity.getWindow(), rect, bitmap, result -> {
                if (result != PixelCopy.SUCCESS || generation != captureGeneration || !enabled(activity)) return;
                snapshots.remove(id); snapshots.put(id, new Snapshot(bitmap, width, height));
                for (WeakReference<Activity> ref : history) { Activity current = ref.get(); if (current != null) current.getWindow().getDecorView().invalidate(); }
                while (snapshots.size() > 3) snapshots.remove(snapshots.keySet().iterator().next());
            }, handler);
        } catch (IllegalArgumentException unavailable) { /* No rendered window: keep ordinary Back available. */ }
    }
    private void install(Activity activity) {
        if (!supported(activity)) return;
        FrameLayout content = activity.findViewById(android.R.id.content);
        if (content == null || content.getChildCount() == 0) return;
        if (content.getChildAt(0) instanceof EdgeLayout) { ((EdgeLayout) content.getChildAt(0)).refresh(); return; }
        if (!enabled(activity)) return;
        EdgeLayout wrapper = new EdgeLayout(activity);
        while (content.getChildCount() > 0) {
            View child = content.getChildAt(0); ViewGroup.LayoutParams params = child.getLayoutParams();
            content.removeView(child); wrapper.foreground.addView(child, params);
        }
        content.addView(wrapper, new FrameLayout.LayoutParams(-1, -1));
        wrapper.requestApplyInsets();
    }
    @Override public void onActivityPostResumed(Activity activity) {
        int found = -1;
        for (int i = 0; i < history.size(); i++) if (history.get(i).get() == activity) found = i;
        if (found >= 0) {
            for (int i = history.size() - 1; i > found; i--) {
                Activity later = history.get(i).get();
                if (later == null || later.getTaskId() == activity.getTaskId()) history.remove(i);
            }
        } else history.add(new WeakReference<>(activity));
        history.removeIf(ref -> ref.get() == null);
        activity.getWindow().getDecorView().post(() -> install(activity));
    }
    @Override public void onActivityPrePaused(Activity activity) { capture(activity); }
    @Override public void onActivityDestroyed(Activity activity) {
        history.removeIf(ref -> ref.get() == null || ref.get() == activity);
        snapshots.remove(System.identityHashCode(activity));
    }
    @Override public void onActivityCreated(Activity activity, Bundle state) {}
    @Override public void onActivityStarted(Activity activity) {}
    @Override public void onActivityResumed(Activity activity) {}
    @Override public void onActivityPaused(Activity activity) {}
    @Override public void onActivityStopped(Activity activity) {}
    @Override public void onActivitySaveInstanceState(Activity activity, Bundle state) {}

    private static final class Snapshot {
        final Bitmap bitmap; final int width, height;
        Snapshot(Bitmap bitmap, int width, int height) { this.bitmap = bitmap; this.width = width; this.height = height; }
    }
    private final class EdgeLayout extends FrameLayout {
        private final Activity activity;
        final FrameLayout foreground;
        private final ImageView background;
        private final float density;
        private final int slop;
        private final Paint hint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final ViewTreeObserver.OnPreDrawListener preDraw = () -> { refresh(); return true; };
        private boolean available, candidate, dragging, settling;
        private float downX, downY, direction, progress;
        private int bandTop, bandBottom, edge;
        private ValueAnimator animation;
        private VelocityTracker velocity;

        EdgeLayout(Activity activity) {
            super(activity); this.activity = activity;
            density = getResources().getDisplayMetrics().density;
            slop = ViewConfiguration.get(activity).getScaledTouchSlop(); edge = Math.round(24 * density);
            background = new ImageView(activity); background.setScaleType(ImageView.ScaleType.FIT_XY); background.setVisibility(INVISIBLE);
            background.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
            addView(background, new LayoutParams(-1, -1));
            foreground = new FrameLayout(activity); addView(foreground, new LayoutParams(-1, -1));
            hint.setColor(0x887F9FC5);
        }
        @Override protected void onAttachedToWindow() { super.onAttachedToWindow(); getViewTreeObserver().addOnPreDrawListener(preDraw); }
        @Override protected void onDetachedFromWindow() {
            getViewTreeObserver().removeOnPreDrawListener(preDraw);
            if (animation != null) animation.cancel(); reset(); super.onDetachedFromWindow();
        }
        void refresh() {
            WindowInsets insets = getRootWindowInsets();
            boolean next = preview(activity, getWidth(), getHeight()) != null && (insets == null || !insets.isVisible(WindowInsets.Type.ime()));
            int half = Math.min(getHeight() / 2, Math.round(100 * density));
            int top = getHeight() / 2 - half, bottom = getHeight() / 2 + half;
            if (next == available && top == bandTop && bottom == bandBottom) return;
            available = next; bandTop = top; bandBottom = bottom;
            setSystemGestureExclusionRects(next ? Arrays.asList(new Rect(0, top, edge, bottom), new Rect(getWidth() - edge, top, getWidth(), bottom)) : Collections.emptyList());
            if (!next && !settling) reset(); invalidate();
        }
        @Override protected void dispatchDraw(Canvas canvas) {
            super.dispatchDraw(canvas);
            if (available && !dragging && !settling) {
                float y = getHeight() / 2f, r = 2 * density;
                canvas.drawRoundRect(2 * density, y - 24 * density, 5 * density, y + 24 * density, r, r, hint);
                canvas.drawRoundRect(getWidth() - 5 * density, y - 24 * density, getWidth() - 2 * density, y + 24 * density, r, r, hint);
            }
        }
        @Override public boolean onInterceptTouchEvent(MotionEvent event) {
            if (settling) return true;
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                refresh(); downX = event.getX(); downY = event.getY(); direction = downX < getWidth() / 2f ? 1 : -1;
                candidate = available && downY >= bandTop && downY <= bandBottom && (downX <= edge || downX >= getWidth() - edge);
            } else if (event.getActionMasked() == MotionEvent.ACTION_MOVE && candidate) {
                float dx = (event.getX() - downX) * direction, dy = Math.abs(event.getY() - downY);
                if (dy > slop && dy > dx) candidate = false;
                if (dx > slop && dx > dy * 1.2f) {
                    Snapshot snapshot = preview(activity, getWidth(), getHeight());
                    if (snapshot == null) { candidate = false; return false; }
                    dragging = true; background.setImageBitmap(snapshot.bitmap); background.setVisibility(VISIBLE);
                    velocity = VelocityTracker.obtain(); velocity.addMovement(event);
                    showProgress(Math.max(0, Math.min(1, dx / Math.max(1, getWidth() * .7f))));
                    getParent().requestDisallowInterceptTouchEvent(true); return true;
                }
            } else if (event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN || event.getActionMasked() == MotionEvent.ACTION_CANCEL || event.getActionMasked() == MotionEvent.ACTION_UP) candidate = false;
            return dragging;
        }
        @Override public boolean onTouchEvent(MotionEvent event) {
            if (settling) return true;
            if (!dragging) return super.onTouchEvent(event);
            if (velocity != null) velocity.addMovement(event);
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_MOVE:
                    showProgress(Math.max(0, Math.min(1, (event.getX() - downX) * direction / Math.max(1, getWidth() * .7f)))); break;
                case MotionEvent.ACTION_UP:
                    velocity.computeCurrentVelocity(1000);
                    boolean commit = progress >= .32f || (progress > .06f && velocity.getXVelocity() * direction > 1000 * density);
                    settle(commit); break;
                case MotionEvent.ACTION_CANCEL: case MotionEvent.ACTION_POINTER_DOWN: settle(false); break;
            }
            return true;
        }
        private void showProgress(float value) {
            progress = value; foreground.setTranslationX(direction * getWidth() * .85f * value);
            foreground.setScaleX(1 - .06f * value); foreground.setScaleY(1 - .06f * value);
        }
        private void settle(boolean commit) {
            dragging = false; candidate = false; settling = true;
            if (velocity != null) { velocity.recycle(); velocity = null; }
            animation = ValueAnimator.ofFloat(progress, commit ? 1f : 0f);
            animation.setDuration(ValueAnimator.areAnimatorsEnabled() ? 180 : 0);
            animation.addUpdateListener(value -> showProgress((Float) value.getAnimatedValue()));
            animation.addListener(new android.animation.AnimatorListenerAdapter() {
                private boolean cancelled;
                @Override public void onAnimationCancel(android.animation.Animator value) { cancelled = true; }
                @Override public void onAnimationEnd(android.animation.Animator value) {
                    settling = false;
                    if (commit && !cancelled && supported(activity) && !((ComponentActivity) activity).getOnBackPressedDispatcher().hasEnabledCallbacks()) {
                        ((ComponentActivity) activity).getOnBackPressedDispatcher().onBackPressed();
                        // This explicit compatibility gesture already supplied the transition.
                        activity.overridePendingTransition(0, 0);
                    } else reset();
                }
            });
            animation.start();
        }
        private void reset() {
            candidate = false; dragging = false; showProgress(0);
            background.setVisibility(INVISIBLE); background.setImageDrawable(null);
            if (velocity != null) { velocity.recycle(); velocity = null; }
        }
    }
}
