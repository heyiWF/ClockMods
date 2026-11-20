package com.clockmods.platform;

import android.os.Bundle;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;

/**
 * API 14+ accessibility delegate, isolated in its own class.
 *
 * <p>Android 1.0 does not have {@code View.AccessibilityDelegate} or
 * {@code AccessibilityNodeInfo}. The verifier rejects a class as soon as one of its method
 * bodies references a missing type, so this code must never be loaded (or linked) on those
 * devices. {@link ApiCompat#installAccessibilityDelegate(View, Runnable)} loads it reflectively
 * only when {@code SDK_INT >= 14}.</p>
 */
public final class AccessibilityCompat {
    private AccessibilityCompat() {
    }

    public static void install(View view, final Runnable openSettings) {
        view.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override
            public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setClickable(true);
                if (ApiCompat.SDK_INT >= 16) {
                    info.addAction(AccessibilityNodeInfo.ACTION_CLICK);
                }
            }

            @Override
            public boolean performAccessibilityAction(View host, int action, Bundle arguments) {
                if (ApiCompat.SDK_INT >= 16 && action == AccessibilityNodeInfo.ACTION_CLICK) {
                    openSettings.run();
                    return true;
                }
                return super.performAccessibilityAction(host, action, arguments);
            }
        });
    }
}
