package com.clockmods.platform;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import java.io.Closeable;
import java.io.IOException;

/**
 * Centralised API-level compatibility helpers for the {@code compat} flavor (minSdk 1).
 *
 * <p>Android 1.0's Dalvik verifier rejects an entire class when any method body references a
 * member that does not exist on the device. {@code Build.VERSION.SDK_INT} itself is API 4, so
 * every API-level check on this flavor must read {@link #SDK_INT} (resolved reflectively) and
 * every post-API-1 member must be reached through reflection.</p>
 *
 * <p>High API classes (e.g. {@code WindowInsetsController}, {@code AccessibilityNodeInfo}) must
 * NOT appear in this class's signatures or fields; they are isolated at their call sites.</p>
 */
public final class ApiCompat {
    /**
     * Runtime API level. {@code Build.VERSION.SDK_INT} was added in API 4, so Android 1.0/1.1
     * have no such field; those devices resolve to 1.
     */
    public static final int SDK_INT = resolveSdkInt();

    private ApiCompat() {
    }

    private static int resolveSdkInt() {
        try {
            Object value = Build.VERSION.class.getField("SDK_INT").get(null);
            if (value instanceof Integer) {
                return ((Integer) value).intValue();
            }
        } catch (Throwable ignored) {
            // Android 1.0/1.1: SDK_INT does not exist.
        }
        return 1;
    }

    /** Null-safe replacement for {@code String.isEmpty()} (API 9). */
    public static boolean isEmpty(String value) {
        return value == null || value.length() == 0;
    }

    /** Replacement for {@code java.util.Objects.equals} (API 19). */
    public static boolean equals(Object first, Object second) {
        return first == second || (first != null && first.equals(second));
    }

    /**
     * Applies a disabled-looking state. {@code View.setAlpha} requires API 11; on older
     * devices the view is disabled and its text is greyed out instead.
     */
    public static void setAlphaCompat(View view, float alpha) {
        if (view == null) {
            return;
        }
        if (SDK_INT >= 11) {
            try {
                View.class.getMethod("setAlpha", float.class).invoke(view, alpha);
                return;
            } catch (Throwable ignored) {
                // Fall through to the API 1 appearance below.
            }
        }
        if (alpha >= 1f) {
            view.setEnabled(true);
            return;
        }
        view.setEnabled(false);
        if (view instanceof TextView) {
            TextView text = (TextView) view;
            int original = text.getCurrentTextColor();
            int red = Color.red(original);
            int green = Color.green(original);
            int blue = Color.blue(original);
            // Blend 45% toward the white background so the label looks muted.
            text.setTextColor(Color.rgb(
                    red + (255 - red) * 45 / 100,
                    green + (255 - green) * 45 / 100,
                    blue + (255 - blue) * 45 / 100));
        }
    }

    /** {@code Gravity.START} (API 17) with an API 1 LEFT fallback. */
    public static int gravityStart() {
        return SDK_INT >= 17 ? Gravity.START : Gravity.LEFT;
    }

    /** {@code Gravity.END} (API 17) with an API 1 RIGHT fallback. */
    public static int gravityEnd() {
        return SDK_INT >= 17 ? Gravity.END : Gravity.RIGHT;
    }

    /** {@code View.setContentDescription} (API 14); old devices use click listeners instead. */
    public static void setContentDescriptionCompat(View view, CharSequence description) {
        if (view == null || SDK_INT < 14) {
            return;
        }
        try {
            View.class.getMethod("setContentDescription", CharSequence.class)
                    .invoke(view, description);
        } catch (Throwable ignored) {
            // Accessibility description is decorative; keep the API 1 behaviour.
        }
    }

    /**
     * {@code View.sendAccessibilityEvent(int)} is API 4. Android 1.0 has no accessibility layer,
     * and a direct call makes the verifier reject the whole declaring class, so it stays
     * reflective here.
     */
    public static void sendAccessibilityEventCompat(View view, int eventType) {
        if (view == null || SDK_INT < 4) {
            return;
        }
        try {
            View.class.getMethod("sendAccessibilityEvent", int.class).invoke(view, eventType);
        } catch (Throwable ignored) {
            // No accessibility service is listening.
        }
    }

    /**
     * Runs {@code action} once after the view has been laid out. Always uses the API 1
     * {@link ViewTreeObserver.OnGlobalLayoutListener} path so no API 11 listener type is
     * linked on Android 1.0.
     */
    public static void onLayoutReady(final View view, final Runnable action) {
        if (view == null || action == null) {
            return;
        }
        final ViewTreeObserver observer = view.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                try {
                    view.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                } catch (Throwable ignored) {
                    // Observer may already be detached.
                }
                action.run();
            }
        });
    }

    /** Whether {@code View.setSystemUiVisibility} (API 14) may be called. */
    public static boolean hasSystemUiVisibility() {
        return SDK_INT >= 14;
    }

    /** Applies system UI flags without linking API 14 on Android 1.0. */
    public static void setSystemUiVisibility(View view, int visibility) {
        if (view == null || SDK_INT < 14) {
            return;
        }
        try {
            View.class.getMethod("setSystemUiVisibility", int.class).invoke(view, visibility);
        } catch (Throwable ignored) {
            // Not available on this runtime.
        }
    }

    /** {@code View.setBackground(Drawable)} is API 16; API 1 has {@code setBackgroundDrawable}. */
    @SuppressWarnings("deprecation")
    public static void setBackgroundCompat(View view, Drawable drawable) {
        if (view == null) {
            return;
        }
        if (SDK_INT >= 16) {
            try {
                View.class.getMethod("setBackground", Drawable.class).invoke(view, drawable);
                return;
            } catch (Throwable ignored) {
                // Fall through to the API 1 setter.
            }
        }
        view.setBackgroundDrawable(drawable);
    }

    /** {@code View.animate()} is API 12; falls back to an immediate alpha change. */
    public static void animateAlpha(View view, float alpha, long duration) {
        if (view == null) {
            return;
        }
        if (SDK_INT >= 12) {
            try {
                Object animator = View.class.getMethod("animate").invoke(view);
                Class<?> type = animator.getClass();
                type.getMethod("alpha", float.class).invoke(animator, alpha);
                type.getMethod("setDuration", long.class).invoke(animator, duration);
                type.getMethod("start").invoke(animator);
                return;
            } catch (Throwable ignored) {
                // Fall through to the static fallback.
            }
        }
        setAlphaCompat(view, alpha);
    }

    /** {@code SignalStrength.getLevel()} is API 23; returns -1 when unavailable. */
    public static int signalStrengthLevel(Object strength) {
        if (strength == null) {
            return -1;
        }
        try {
            Object value = strength.getClass().getMethod("getLevel").invoke(strength);
            if (value instanceof Integer) {
                return ((Integer) value).intValue();
            }
        } catch (Throwable ignored) {
            // API 1-22 use the ASU based calculation instead.
        }
        return -1;
    }

    /** {@code SignalStrength.isGsm()} is API 3; pre-3 devices are GSM only. */
    public static boolean signalStrengthIsGsm(Object strength) {
        if (strength == null) {
            return true;
        }
        try {
            Object value = strength.getClass().getMethod("isGsm").invoke(strength);
            if (value instanceof Boolean) {
                return ((Boolean) value).booleanValue();
            }
        } catch (Throwable ignored) {
            // Android 1.0-1.5: GSM only.
        }
        return true;
    }

    /** {@code SignalStrength.getGsmSignalStrength()} (ASU); 99 means unknown. */
    public static int signalStrengthGsmAsu(Object strength) {
        if (strength == null) {
            return 99;
        }
        try {
            Object value = strength.getClass().getMethod("getGsmSignalStrength").invoke(strength);
            if (value instanceof Integer) {
                return ((Integer) value).intValue();
            }
        } catch (Throwable ignored) {
            // Treated as unknown.
        }
        return 99;
    }

    /** {@code SignalStrength.getCdmaDbm()} (API 7); -120 dBm when unavailable. */
    public static int signalStrengthCdmaDbm(Object strength) {
        if (strength == null) {
            return -120;
        }
        try {
            Object value = strength.getClass().getMethod("getCdmaDbm").invoke(strength);
            if (value instanceof Integer) {
                return ((Integer) value).intValue();
            }
        } catch (Throwable ignored) {
            // Not a CDMA device.
        }
        return -120;
    }

    /** Callback used to hand a signal level back from an isolated API 7+ listener. */
    public interface IntCallback {
        void onLevel(int level);
    }

    /**
     * Starts the mobile signal listener. {@code SignalStrength} and
     * {@code PhoneStateListener.onSignalStrengthsChanged} are API 7, so the implementation lives
     * in its own class and is only loaded on those devices.
     */
    public static Object startMobileSignalMonitor(Context context, IntCallback callback) {
        if (context == null || SDK_INT < 7) {
            return null;
        }
        try {
            return Class.forName("com.clockmods.ui.MobileSignalMonitor")
                    .getMethod("start", Context.class, IntCallback.class)
                    .invoke(null, context, callback);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** Stops a monitor created by {@link #startMobileSignalMonitor(Context, IntCallback)}. */
    public static void stopMobileSignalMonitor(Object handle) {
        if (handle == null) {
            return;
        }
        try {
            Class.forName("com.clockmods.ui.MobileSignalMonitor")
                    .getMethod("stop", Object.class)
                    .invoke(null, handle);
        } catch (Throwable ignored) {
            // Nothing to unregister.
        }
    }

    /**
     * Creates a {@code GestureDetector}. The {@code (Context, OnGestureListener)} constructor is
     * API 3; Android 1.0 only has the two-argument form. Double-tap detection does not exist at
     * all before API 3, so callers must offer a single-tap entry point there.
     */
    public static GestureDetector newGestureDetector(Context context,
            GestureDetector.OnGestureListener listener) {
        if (SDK_INT >= 3) {
            try {
                return GestureDetector.class
                        .getConstructor(Context.class, GestureDetector.OnGestureListener.class)
                        .newInstance(context, listener);
            } catch (Throwable ignored) {
                // Fall through to the API 1 constructors.
            }
        }
        try {
            return GestureDetector.class
                    .getConstructor(GestureDetector.OnGestureListener.class, Handler.class)
                    .newInstance(listener, null);
        } catch (Throwable ignored) {
            return new GestureDetector(listener);
        }
    }

    /** {@code Activity.isDestroyed()} is API 17; older runtimes are never destroyed here. */    public static boolean isDestroyed(Activity activity) {
        if (activity == null || SDK_INT < 17) {
            return false;
        }
        try {
            Object value = Activity.class.getMethod("isDestroyed").invoke(activity);
            return value instanceof Boolean && ((Boolean) value).booleanValue();
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** {@code Activity.recreate()} is API 11; falls back to a manual relaunch. */
    public static void recreate(Activity activity) {
        if (activity == null) {
            return;
        }
        if (SDK_INT >= 11) {
            try {
                Activity.class.getMethod("recreate").invoke(activity);
                return;
            } catch (Throwable ignored) {
                // Fall through to the API 1 restart path.
            }
        }
        try {
            android.content.Intent intent = activity.getIntent();
            activity.finish();
            if (intent != null) {
                activity.startActivity(intent);
            }
        } catch (Throwable ignored) {
            // Nothing else can be done on this runtime.
        }
    }

    /**
     * Installs the API 14 accessibility delegate from an isolated class, so Android 1.0 never
     * links {@code View.AccessibilityDelegate} or {@code AccessibilityNodeInfo}.
     */
    public static void installAccessibilityDelegate(View view, Runnable openSettings) {
        if (view == null || SDK_INT < 14) {
            return;
        }
        try {
            Class.forName("com.clockmods.platform.AccessibilityCompat")
                    .getMethod("install", View.class, Runnable.class)
                    .invoke(null, view, openSettings);
        } catch (Throwable ignored) {
            // Accessibility enhancement only; the click listener remains.
        }
    }

    /** Closes a stream without propagating secondary failures. */
    public static void closeQuietly(Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException ignored) {
            // Nothing useful can be done while closing a failed stream.
        }
    }
}
