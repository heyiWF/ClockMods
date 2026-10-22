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
 * Draws the network state and the battery percentage in the corner of the clock as
 * two equally sized text lines ("网络正常" / "网络断开" above the percentage).
 */
public class StatusBarView extends View {
    /** Text size of one line at 100%, in dp (matches the previous 44dp two-line box). */
    private static final float TEXT_SIZE_DP = 14.5f;

    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final float density;
    private final ClockPreferences preferences;

    private int batteryLevel = -1;
    private boolean networkConnected;
    private boolean contentAlignedStart;

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

    public void setContentAlignedStart(boolean alignedStart) {
        contentAlignedStart = alignedStart;
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
        setContentDescription(getContext().getString(R.string.status_network_and_battery,
                networkText(), batteryText()));
    }

    private String networkText() {
        return getContext().getString(networkConnected
                ? R.string.status_network_online : R.string.status_network_offline);
    }

    private String batteryText() {
        return batteryLevel >= 0 ? batteryLevel + "%" : "--";
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
        textPaint.setTextAlign(contentAlignedStart ? Paint.Align.LEFT : Paint.Align.RIGHT);

        Paint.FontMetrics metrics = textPaint.getFontMetrics();
        float lineHeight = textPaint.getFontSpacing();
        float blockHeight = metrics.descent - metrics.ascent + lineHeight;
        float top = (getHeight() - blockHeight) / 2f;
        float firstBaseline = top - metrics.ascent;
        float x = contentAlignedStart ? getPaddingLeft() : getWidth() - getPaddingRight();

        canvas.drawText(networkText(), x, firstBaseline, textPaint);
        canvas.drawText(batteryText(), x, firstBaseline + lineHeight, textPaint);
    }

    @Override
    protected void onDetachedFromWindow() {
        unregisterReceivers();
        super.onDetachedFromWindow();
    }
}
