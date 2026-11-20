package com.clockmods.ui;

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;

import com.clockmods.platform.ApiCompat;

/**
 * Mobile signal listener, isolated for API 7+.
 *
 * <p>{@code android.telephony.SignalStrength} does not exist on Android 1.0 and
 * {@code PhoneStateListener.onSignalStrengthsChanged} was added in API 7, so none of these types
 * may appear in {@link StatusBarView}; it loads this class reflectively through
 * {@link ApiCompat#startMobileSignalMonitor(Context, ApiCompat.IntCallback)}.</p>
 */
public final class MobileSignalMonitor {
    private MobileSignalMonitor() {
    }

    public static Object start(Context context, final ApiCompat.IntCallback callback) {
        TelephonyManager telephonyManager =
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager == null) {
            return null;
        }
        PhoneStateListener listener = new PhoneStateListener() {
            @Override
            public void onSignalStrengthsChanged(SignalStrength strength) {
                super.onSignalStrengthsChanged(strength);
                callback.onLevel(StatusBarView.mobileSignalLevel(strength));
            }
        };
        try {
            telephonyManager.listen(listener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
            return new Object[] {telephonyManager, listener};
        } catch (SecurityException ignored) {
            return null;
        }
    }

    public static void stop(Object handle) {
        if (!(handle instanceof Object[])) {
            return;
        }
        Object[] parts = (Object[]) handle;
        try {
            ((TelephonyManager) parts[0]).listen((PhoneStateListener) parts[1],
                    PhoneStateListener.LISTEN_NONE);
        } catch (Throwable ignored) {
            // Already unregistered or permission revoked.
        }
    }
}
