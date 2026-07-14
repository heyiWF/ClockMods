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
import android.util.AttributeSet;
import android.view.View;

import com.clockmods.background.BackgroundRepository;

/**
 * Draws the current network state and battery level (with percentage) in the
 * corner of the clock using Google Material Symbols glyphs (Battery Android,
 * Network Wifi and Signal Cellular series) rendered on the canvas so they look
 * identical on API 14+.
 */
public class StatusBarView extends View {
    private enum NetworkState {
        NONE, MOBILE, WIFI
    }

    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final float density;

    private BackgroundRepository backgroundRepository;

    private int batteryLevel = -1;
    private boolean batteryCharging;
    private NetworkState networkState = NetworkState.NONE;
    private int signalStrength = 4; // 0..4 bars

    private boolean receiverRegistered;

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
        refreshNetworkState();
        invalidate();
    }

    public void stop() {
        unregisterReceivers();
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
        context.registerReceiver(connectivityReceiver,
                new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
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

    private void updateBatteryFromIntent(Intent intent) {
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        if (level >= 0 && scale > 0) {
            batteryLevel = Math.round(level * 100f / scale);
        }
        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        batteryCharging = status == BatteryManager.BATTERY_STATUS_CHARGING
                || status == BatteryManager.BATTERY_STATUS_FULL;
    }

    private void refreshNetworkState() {
        Context context = getContext();
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            networkState = NetworkState.NONE;
            return;
        }
        NetworkInfo active = cm.getActiveNetworkInfo();
        if (active == null || !active.isConnected()) {
            networkState = NetworkState.NONE;
            return;
        }
        int type = active.getType();
        if (type == ConnectivityManager.TYPE_WIFI || type == ConnectivityManager.TYPE_ETHERNET) {
            networkState = NetworkState.WIFI;
            signalStrength = wifiSignalLevel(context);
        } else {
            networkState = NetworkState.MOBILE;
            signalStrength = 4;
        }
    }

    private int wifiSignalLevel(Context context) {
        WifiManager wifiManager =
                (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null || wifiManager.getConnectionInfo() == null) {
            return 4;
        }
        int rssi = wifiManager.getConnectionInfo().getRssi();
        return WifiManager.calculateSignalLevel(rssi, 5); // 0..4
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
        if (batteryLevel < 0 && networkState == NetworkState.NONE) {
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

        // Network icon.
        if (networkState != NetworkState.NONE) {
            float networkSize = iconHeight * 1.15f;
            cursor -= networkSize;
            if (networkState == NetworkState.WIFI) {
                drawWifi(canvas, cursor, centerY, networkSize);
            } else {
                drawMobile(canvas, cursor, centerY, networkSize);
            }
            fillPaint.setAlpha(255);
        }
    }

    private void drawBattery(Canvas canvas, float left, float centerY, float size) {
        MaterialIcon icon;
        if (batteryCharging) {
            icon = MaterialIcon.BATTERY_BOLT;
        } else if (batteryLevel <= 5) {
            icon = MaterialIcon.BATTERY_ALERT;
        } else if (batteryLevel >= 98) {
            icon = MaterialIcon.BATTERY_FULL;
        } else {
            // Map 0..100% onto the seven Battery Android fill levels (0..6).
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
