package com.clockmods.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.AttributeSet;
import android.view.View;

import com.clockmods.R;
import com.clockmods.background.BackgroundRepository;

/**
 * Draws the current network state and battery level (with percentage) in the
 * corner of the clock using Google Material Symbols glyphs (Battery Android,
 * Network Wifi and Signal Cellular series) rendered on the canvas so they look
 * identical on API 14+.
 */
public class StatusBarView extends View {
    private static final int INVALID_RSSI = -127;

    private enum NetworkState {
        NONE, MOBILE, WIFI, ETHERNET
    }

    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final float density;

    private BackgroundRepository backgroundRepository;

    private int batteryLevel = -1;
    private boolean batteryCharging;
    private NetworkState networkState = NetworkState.NONE;
    private int signalStrength;
    private int mobileSignalStrength;

    private boolean receiverRegistered;
    private TelephonyManager telephonyManager;

    private final PhoneStateListener phoneStateListener = new PhoneStateListener() {
        @Override
        public void onSignalStrengthsChanged(SignalStrength strength) {
            super.onSignalStrengthsChanged(strength);
            mobileSignalStrength = mobileSignalLevel(strength);
            if (networkState == NetworkState.MOBILE) {
                signalStrength = mobileSignalStrength;
                updateAccessibilityDescription();
                invalidate();
            }
        }
    };

    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateBatteryFromIntent(intent);
            refreshNetworkState();
            invalidate();
        }
    };

    private final BroadcastReceiver connectivityReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshNetworkState();
            invalidate();
        }
    };

    public StatusBarView(Context context) {
        this(context, null);
    }

    public StatusBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        fillPaint.setStyle(Paint.Style.FILL);
        textPaint.setTextAlign(Paint.Align.RIGHT);
    }

    public void setBackgroundRepository(BackgroundRepository repository) {
        backgroundRepository = repository;
        invalidate();
    }

    public void start() {
        registerReceivers();
        registerPhoneStateListener();
        refreshNetworkState();
        invalidate();
    }

    public void stop() {
        unregisterReceivers();
        unregisterPhoneStateListener();
    }

    private void registerReceivers() {
        if (receiverRegistered) {
            return;
        }
        Context context = getContext();
        Intent sticky = context.registerReceiver(batteryReceiver,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (sticky != null) {
            updateBatteryFromIntent(sticky);
        }
        IntentFilter networkFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        networkFilter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        context.registerReceiver(connectivityReceiver, networkFilter);
        receiverRegistered = true;
    }

    private void unregisterReceivers() {
        if (!receiverRegistered) {
            return;
        }
        Context context = getContext();
        try {
            context.unregisterReceiver(batteryReceiver);
            context.unregisterReceiver(connectivityReceiver);
        } catch (IllegalArgumentException ignored) {
            // Receivers were not registered.
        }
        receiverRegistered = false;
    }

    @SuppressWarnings("deprecation")
    private void registerPhoneStateListener() {
        telephonyManager = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager == null) return;
        try {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
        } catch (SecurityException ignored) {
            telephonyManager = null;
        }
    }

    @SuppressWarnings("deprecation")
    private void unregisterPhoneStateListener() {
        if (telephonyManager == null) return;
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        telephonyManager = null;
    }

    private void updateBatteryFromIntent(Intent intent) {
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        if (level >= 0 && scale > 0) {
            batteryLevel = Math.round(level * 100f / scale);
        }
        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        batteryCharging = status == BatteryManager.BATTERY_STATUS_CHARGING
                || status == BatteryManager.BATTERY_STATUS_FULL;
        updateAccessibilityDescription();
    }

    private void refreshNetworkState() {
        Context context = getContext();
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            networkState = NetworkState.NONE;
            updateAccessibilityDescription();
            return;
        }
        NetworkInfo active = cm.getActiveNetworkInfo();
        if (active == null || !active.isConnected()) {
            networkState = NetworkState.NONE;
            updateAccessibilityDescription();
            return;
        }
        int type = active.getType();
        if (type == ConnectivityManager.TYPE_WIFI) {
            networkState = NetworkState.WIFI;
            signalStrength = wifiSignalLevel(context);
        } else if (type == ConnectivityManager.TYPE_MOBILE) {
            networkState = NetworkState.MOBILE;
            signalStrength = mobileSignalStrength;
        } else if (type == ConnectivityManager.TYPE_ETHERNET) {
            networkState = NetworkState.ETHERNET;
        } else {
            networkState = NetworkState.NONE;
        }
        updateAccessibilityDescription();
    }

    private void updateAccessibilityDescription() {
        String network;
        if (networkState == NetworkState.WIFI) {
            network = getContext().getString(R.string.status_wifi_signal, signalStrength + 1, 5);
        } else if (networkState == NetworkState.MOBILE) {
            network = getContext().getString(R.string.status_mobile_signal, signalStrength + 1, 5);
        } else if (networkState == NetworkState.ETHERNET) {
            network = getContext().getString(R.string.status_ethernet);
        } else {
            network = getContext().getString(R.string.status_no_network);
        }
        String battery = batteryLevel >= 0
                ? getContext().getString(batteryCharging
                        ? R.string.status_battery_charging : R.string.status_battery, batteryLevel)
                : getContext().getString(R.string.status_battery_unknown);
        setContentDescription(getContext().getString(
                R.string.status_network_and_battery, network, battery));
    }

    private int wifiSignalLevel(Context context) {
        WifiManager wifiManager =
                (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null || wifiManager.getConnectionInfo() == null) {
            return 0;
        }
        int rssi = wifiManager.getConnectionInfo().getRssi();
        if (rssi == INVALID_RSSI) {
            return 0;
        }
        if (Build.VERSION.SDK_INT >= 30) {
            return normalizeSignalLevel(
                    wifiManager.calculateSignalLevel(rssi), wifiManager.getMaxSignalLevel());
        }
        return WifiManager.calculateSignalLevel(rssi, 5); // 0..4
    }

    static int normalizeSignalLevel(int level, int maxLevel) {
        if (maxLevel <= 0) return 0;
        int clamped = Math.max(0, Math.min(maxLevel, level));
        return Math.round(clamped * 4f / maxLevel);
    }

    @SuppressWarnings("deprecation")
    static int mobileSignalLevel(SignalStrength strength) {
        if (strength == null) return 0;
        if (Build.VERSION.SDK_INT >= 23) {
            return Math.max(0, Math.min(4, strength.getLevel()));
        }
        if (strength.isGsm()) {
            int asu = strength.getGsmSignalStrength();
            if (asu == 99 || asu <= 2) return 0;
            if (asu >= 20) return 4;
            if (asu >= 13) return 3;
            if (asu >= 8) return 2;
            return 1;
        }
        int dbm = strength.getCdmaDbm();
        if (dbm >= -75) return 4;
        if (dbm >= -85) return 3;
        if (dbm >= -95) return 2;
        if (dbm >= -100) return 1;
        return 0;
    }

    private int resolveTint() {
        if (backgroundRepository != null) {
            return backgroundRepository.getTimeColor();
        }
        return 0xFFFFFFFF;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (batteryLevel < 0 && !hasNetworkInfo()) {
            return;
        }
        int tint = resolveTint();
        fillPaint.setColor(tint);
        textPaint.setColor(tint);
        fillPaint.setShadowLayer(4f * density, 0f, 1.5f * density, 0x66000000);
        textPaint.setShadowLayer(4f * density, 0f, 1.5f * density, 0x66000000);

        float iconHeight = getHeight() * 0.5f;
        if (iconHeight <= 0f) {
            iconHeight = 18f * density;
        }
        float centerY = getHeight() / 2f;
        float right = getWidth() - getPaddingRight();
        float gap = 8f * density;

        // Battery percentage text.
        float cursor = right;
        if (batteryLevel >= 0) {
            textPaint.setTextSize(iconHeight * 0.9f);
            if (backgroundRepository != null) {
                textPaint.setTypeface(ClockTypefaceResolver.resolveTime(
                        getContext(), backgroundRepository.getFontFamily(), false));
            }
            String percent = batteryLevel + "%";
            canvas.drawText(percent, cursor, centerY - (textPaint.ascent() + textPaint.descent()) / 2f, textPaint);
            cursor -= textPaint.measureText(percent) + gap * 0.6f;

            // Battery Android glyph is drawn on a square box; the glyph itself
            // is wider than tall so scale the box up to keep the bar readable.
            float batterySize = iconHeight * 1.55f;
            cursor -= batterySize;
            drawBattery(canvas, cursor, centerY, batterySize);
            cursor -= gap * 0.4f;
        }

        // Network icon (always shown, including the "no network" globe).
        float networkSize = iconHeight * 1.15f;
        cursor -= networkSize;
        switch (networkState) {
            case WIFI:
                drawWifi(canvas, cursor, centerY, networkSize);
                break;
            case MOBILE:
                drawMobile(canvas, cursor, centerY, networkSize);
                break;
            case ETHERNET:
                drawIcon(canvas, MaterialIcon.ETHERNET, cursor, centerY, networkSize);
                break;
            default:
                drawIcon(canvas, MaterialIcon.GLOBE_CANCEL, cursor, centerY, networkSize);
                break;
        }
        fillPaint.setAlpha(255);
    }

    /** True when the receivers have reported a connectivity state to display. */
    private boolean hasNetworkInfo() {
        return receiverRegistered;
    }

    private void drawIcon(Canvas canvas, MaterialIcon icon, float left, float centerY, float size) {
        fillPaint.setColor(resolveTint());
        fillPaint.setAlpha(255);
        icon.draw(canvas, left, centerY - size / 2f, size, fillPaint);
    }

    private void drawBattery(Canvas canvas, float left, float centerY, float size) {
        MaterialIcon icon;
        if (batteryCharging) {
            icon = MaterialIcon.BATTERY_BOLT;
        } else {
            // Map 0..100% onto the eight Battery Android glyphs
            // (Battery Android 0..6 then Battery Android Full).
            int level = Math.round(batteryLevel / 100f * (MaterialIcon.BATTERY_LEVELS.length - 1));
            level = Math.max(0, Math.min(MaterialIcon.BATTERY_LEVELS.length - 1, level));
            icon = MaterialIcon.BATTERY_LEVELS[level];
        }
        fillPaint.setColor(resolveTint());
        fillPaint.setAlpha(255);
        icon.draw(canvas, left, centerY - size / 2f, size, fillPaint);
    }

    private void drawWifi(Canvas canvas, float left, float centerY, float size) {
        int bars = Math.max(0, Math.min(4, signalStrength));
        MaterialIcon icon = MaterialIcon.WIFI_BARS[bars];
        fillPaint.setColor(resolveTint());
        fillPaint.setAlpha(255);
        icon.draw(canvas, left, centerY - size / 2f, size, fillPaint);
    }

    private void drawMobile(Canvas canvas, float left, float centerY, float size) {
        int bars = Math.max(0, Math.min(4, signalStrength));
        MaterialIcon icon = MaterialIcon.CELLULAR_BARS[bars];
        fillPaint.setColor(resolveTint());
        fillPaint.setAlpha(255);
        icon.draw(canvas, left, centerY - size / 2f, size, fillPaint);
    }

    @Override
    protected void onDetachedFromWindow() {
        unregisterReceivers();
        super.onDetachedFromWindow();
    }
}
