package com.clockmods.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.util.AttributeSet;
import android.view.View;

import com.clockmods.R;
import com.clockmods.background.ClockPreferences;

/**
 * Draws the network state and the battery level in the corner of the clock as two equally
 * sized text lines ("在线" / "离线" above "电量 78%" or "充电中 78%").
 */
public class StatusBarView extends View {
    /** Text size of one line at 100%, in dp (matches the previous 44dp two-line box). */
    private static final float TEXT_SIZE_DP = 14.5f;

    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final float density;
    private final ClockPreferences preferences;

    private int batteryLevel = -1;
    private boolean charging;
    private boolean networkConnected;

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
        preferences = new ClockPreferences(context.getApplicationContext());
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
        int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
        charging = plugged != 0 || status == BatteryManager.BATTERY_STATUS_CHARGING;
        updateAccessibilityDescription();
    }

    @SuppressWarnings("deprecation")
    private void refreshNetworkState() {
        ConnectivityManager cm = (ConnectivityManager)
                getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo active = cm == null ? null : cm.getActiveNetworkInfo();
        networkConnected = active != null && active.isConnected();
        updateAccessibilityDescription();
    }

    private void updateAccessibilityDescription() {
        requestLayout();
        setContentDescription(getContext().getString(R.string.status_network_and_battery,
                networkText(), batteryText()));
    }

    private String networkText() {
        return getContext().getString(networkConnected
                ? R.string.status_network_online : R.string.status_network_offline);
    }

    private String batteryText() {
        if (batteryLevel < 0) {
            return "--";
        }
        return getContext().getString(charging
                ? R.string.status_battery_charging : R.string.status_battery, batteryLevel);
    }

    private float resolveStatusTextScale() {
        return ClockPreferences.normalizeStatusIconScale(preferences.getStatusIconScale());
    }

    static float calculateStatusTextSize(float density, float scale) {
        return TEXT_SIZE_DP * density * scale;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        applyTextPaint();
        float textWidth = Math.max(textPaint.measureText(networkText()),
                textPaint.measureText(batteryText()));
        int width = (int) Math.ceil(textWidth) + getPaddingLeft() + getPaddingRight();
        int height = (int) Math.ceil(textPaint.getFontSpacing() * 2f)
                + getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(resolveSize(width, widthMeasureSpec),
                resolveSize(height, heightMeasureSpec));
    }

    private void applyTextPaint() {
        textPaint.setColor(ClockPreferences.TEXT_COLOR);
        textPaint.setTypeface(ClockTypefaceResolver.resolveTime(getContext(), false));
        textPaint.setTextSize(calculateStatusTextSize(density, resolveStatusTextScale()));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (batteryLevel < 0 && !receiverRegistered) {
            return;
        }
        float scale = resolveStatusTextScale();
        applyTextPaint();
        textPaint.setShadowLayer(4f * density * scale, 0f, 1.5f * density * scale, 0x66000000);
        textPaint.setTextAlign(Paint.Align.RIGHT);

        Paint.FontMetrics metrics = textPaint.getFontMetrics();
        float lineHeight = textPaint.getFontSpacing();
        float blockHeight = metrics.descent - metrics.ascent + lineHeight;
        float top = (getHeight() - blockHeight) / 2f;
        float firstBaseline = top - metrics.ascent;
        float x = getWidth() - getPaddingRight();

        canvas.drawText(networkText(), x, firstBaseline, textPaint);
        canvas.drawText(batteryText(), x, firstBaseline + lineHeight, textPaint);
    }

    @Override
    protected void onDetachedFromWindow() {
        unregisterReceivers();
        super.onDetachedFromWindow();
    }
}
