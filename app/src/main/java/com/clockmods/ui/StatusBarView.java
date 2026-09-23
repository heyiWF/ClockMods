package com.clockmods.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.TransportInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.telephony.SignalStrength;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import android.util.AttributeSet;
import android.view.View;

import com.clockmods.R;
import com.clockmods.background.BackgroundRepository;
import com.clockmods.background.ClockPreferences;

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
    private static final int[] BATTERY_GLYPHS = {
            StatusSymbolRenderer.BATTERY_0, StatusSymbolRenderer.BATTERY_1,
            StatusSymbolRenderer.BATTERY_2, StatusSymbolRenderer.BATTERY_3,
            StatusSymbolRenderer.BATTERY_4, StatusSymbolRenderer.BATTERY_5,
            StatusSymbolRenderer.BATTERY_6, StatusSymbolRenderer.BATTERY_FULL
    };
    private static final int[] WIFI_GLYPHS = {
            StatusSymbolRenderer.WIFI_0, StatusSymbolRenderer.WIFI_1,
            StatusSymbolRenderer.WIFI_2, StatusSymbolRenderer.WIFI_3,
            StatusSymbolRenderer.WIFI_4
    };
    private static final int[] CELLULAR_GLYPHS = {
            StatusSymbolRenderer.CELLULAR_0, StatusSymbolRenderer.CELLULAR_1,
            StatusSymbolRenderer.CELLULAR_2, StatusSymbolRenderer.CELLULAR_3,
            StatusSymbolRenderer.CELLULAR_4
    };

    private BackgroundRepository backgroundRepository;
    private StatusIconStyle statusIconStyle;

    private int batteryLevel = -1;
    private boolean batteryCharging;
    private NetworkState networkState = NetworkState.NONE;
    private int signalStrength;
    private int mobileSignalStrength;
    private boolean contentAlignedStart;
    private int tintOverride;
    private boolean contentShadow = true;

    private boolean receiverRegistered;
    private boolean networkCallbackRegistered;
    private TelephonyManager telephonyManager;
    private ConnectivityManager connectivityManager;

    /**
     * The platform retired {@code PhoneStateListener} at API 31 — the level this app already
     * requires — in favour of a callback bound to an executor. Registering on the main executor
     * means the view state this touches is still only written from the main thread.
     */
    private final TelephonyCallback telephonyCallback = new SignalStrengthCallback();

    private final class SignalStrengthCallback extends TelephonyCallback
            implements TelephonyCallback.SignalStrengthsListener {
        @Override
        public void onSignalStrengthsChanged(SignalStrength strength) {
            mobileSignalStrength = mobileSignalLevel(strength);
            if (networkState == NetworkState.MOBILE) {
                signalStrength = mobileSignalStrength;
                updateAccessibilityDescription();
                invalidate();
            }
        }
    }

    /**
     * Replaces the {@code CONNECTIVITY_ACTION} broadcast, which has been deprecated since API 28.
     * These arrive on a binder thread, so the refresh is posted rather than run in place.
     */
    private final ConnectivityManager.NetworkCallback networkCallback =
            new ConnectivityManager.NetworkCallback() {
        @Override public void onAvailable(Network network) { postRefreshNetworkState(); }

        @Override public void onLost(Network network) { postRefreshNetworkState(); }

        @Override public void onCapabilitiesChanged(Network network,
                NetworkCapabilities capabilities) {
            postRefreshNetworkState();
        }
    };

    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int previousLevel = batteryLevel;
            updateBatteryFromIntent(intent);
            refreshNetworkState();
            // The measured width includes the percentage text, so a new level has to re-measure or
            // a host-supplied pill background would keep the width of the previous reading.
            if (previousLevel != batteryLevel) requestLayout();
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
        requestLayout();
        invalidate();
    }

    public void setContentAlignedStart(boolean alignedStart) {
        contentAlignedStart = alignedStart;
        invalidate();
    }

    /**
     * Forces the icon and battery-text colour. Pass {@code 0} to go back to following the user's
     * clock colour, which is what hosts that sit on the user's own background want.
     */
    public void setTintOverride(int color) {
        tintOverride = color;
        invalidate();
    }

    /**
     * Turns the drop shadow behind the icons and the percentage off. A host that already gives the
     * row a capsule of its own has contrast without it, and a dark blur under dark glyphs on a light
     * capsule reads as smudging rather than as depth.
     */
    public void setContentShadowEnabled(boolean enabled) {
        if (contentShadow == enabled) return;
        contentShadow = enabled;
        invalidate();
    }

    public void start() {
        registerReceivers();
        registerNetworkCallback();
        registerTelephonyCallback();
        refreshNetworkState();
        // Registering picks up the sticky battery intent, which can be the first real level this
        // view sees; the percentage text is part of the measured width so re-measure with it.
        requestLayout();
        invalidate();
    }

    public void stop() {
        unregisterReceivers();
        unregisterNetworkCallback();
        unregisterTelephonyCallback();
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
        // Connectivity itself comes from the network callback now; this broadcast is kept only
        // because RSSI changes on an already-connected wifi network do not raise one.
        context.registerReceiver(connectivityReceiver,
                new IntentFilter(WifiManager.RSSI_CHANGED_ACTION));
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

    private void registerTelephonyCallback() {
        telephonyManager = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager == null) return;
        try {
            telephonyManager.registerTelephonyCallback(
                    getContext().getMainExecutor(), telephonyCallback);
        } catch (SecurityException ignored) {
            telephonyManager = null;
        }
    }

    private void unregisterTelephonyCallback() {
        if (telephonyManager == null) return;
        telephonyManager.unregisterTelephonyCallback(telephonyCallback);
        telephonyManager = null;
    }

    private void registerNetworkCallback() {
        if (networkCallbackRegistered) return;
        connectivityManager = (ConnectivityManager)
                getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) return;
        try {
            connectivityManager.registerDefaultNetworkCallback(networkCallback);
            networkCallbackRegistered = true;
        } catch (RuntimeException ignored) {
            // A restricted profile can refuse it; the synchronous read below still works.
        }
    }

    private void unregisterNetworkCallback() {
        if (!networkCallbackRegistered || connectivityManager == null) return;
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        } catch (IllegalArgumentException ignored) {
            // Already unregistered.
        }
        networkCallbackRegistered = false;
    }

    private void postRefreshNetworkState() {
        post(() -> {
            refreshNetworkState();
            invalidate();
        });
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
        if (connectivityManager == null) {
            connectivityManager = (ConnectivityManager)
                    context.getSystemService(Context.CONNECTIVITY_SERVICE);
        }
        // NetworkInfo and the TYPE_* constants were replaced by transports on capabilities; an
        // absent active network is the modern spelling of "not connected".
        NetworkCapabilities capabilities = connectivityManager == null ? null
                : connectivityManager.getNetworkCapabilities(
                        connectivityManager.getActiveNetwork());
        if (capabilities == null) {
            networkState = NetworkState.NONE;
        } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            networkState = NetworkState.WIFI;
            signalStrength = wifiSignalLevel(context, capabilities);
        } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            networkState = NetworkState.MOBILE;
            signalStrength = mobileSignalStrength;
        } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
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

    /**
     * {@code WifiManager.getConnectionInfo()} was retired at API 31 in favour of reading the
     * {@link WifiInfo} off the network's own capabilities — which is also the only form that stays
     * correct when more than one network is up.
     */
    private int wifiSignalLevel(Context context, NetworkCapabilities capabilities) {
        WifiManager wifiManager =
                (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        TransportInfo transportInfo = capabilities == null ? null : capabilities.getTransportInfo();
        if (wifiManager == null || !(transportInfo instanceof WifiInfo)) {
            return 0;
        }
        int rssi = ((WifiInfo) transportInfo).getRssi();
        if (rssi == INVALID_RSSI) {
            return 0;
        }
        return normalizeSignalLevel(
                wifiManager.calculateSignalLevel(rssi), wifiManager.getMaxSignalLevel());
    }

    static int normalizeSignalLevel(int level, int maxLevel) {
        if (maxLevel <= 0) return 0;
        int clamped = Math.max(0, Math.min(maxLevel, level));
        return Math.round(clamped * 4f / maxLevel);
    }

    static int mobileSignalLevel(SignalStrength strength) {
        if (strength == null) return 0;
        return Math.max(0, Math.min(4, strength.getLevel()));
    }

    private int resolveTint() {
        if (tintOverride != 0) {
            return tintOverride;
        }
        if (backgroundRepository != null) {
            return backgroundRepository.getTimeColor();
        }
        return 0xFFFFFFFF;
    }

    private float resolveStatusIconScale() {
        return backgroundRepository == null
                ? ClockPreferences.DEFAULT_STATUS_ICON_SCALE
                : backgroundRepository.getStatusIconScale();
    }

    static float calculateStatusIconHeight(float viewHeight, float density, float scale) {
        float baseHeight = viewHeight * 0.5f;
        if (baseHeight <= 0f) baseHeight = 18f * density;
        return baseHeight * ClockPreferences.normalizeStatusIconScale(scale);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredHeight = Math.round(28f * density);
        int measuredHeight = resolveSize(desiredHeight, heightMeasureSpec);
        // Measure with the same scale, text and glyph set onDraw uses. Hosts that give this view
        // a pill background rely on wrap_content hugging the drawn content, so measuring for the
        // largest possible icon scale or a placeholder "100%" would leave dead space in the pill.
        float scale = resolveStatusIconScale();
        float iconHeight = calculateStatusIconHeight(measuredHeight, density, scale);
        float gap = 8f * density * scale;
        textPaint.setTextSize(iconHeight * 0.9f);
        if (backgroundRepository != null) {
            textPaint.setTypeface(ClockTypefaceResolver.resolveTime(
                    getContext(), backgroundRepository.getFontFamily(), false));
        }
        float contentWidth = iconHeight * 1.15f;
        if (batteryLevel >= 0) {
            contentWidth += gap * 0.4f + iconHeight * 1.55f + gap * 0.6f
                    + textPaint.measureText(batteryLevel + "%");
        }
        int desiredWidth = Math.round(getPaddingLeft() + contentWidth + getPaddingRight());
        setMeasuredDimension(resolveSize(desiredWidth, widthMeasureSpec), measuredHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        statusIconStyle = StatusIconStyle.read(getContext());
        if (batteryLevel < 0 && !hasNetworkInfo()) {
            return;
        }
        int tint = resolveTint();
        float scale = resolveStatusIconScale();
        fillPaint.setColor(tint);
        textPaint.setColor(tint);
        if (contentShadow) {
            fillPaint.setShadowLayer(4f * density * scale, 0f,
                    1.5f * density * scale, 0x66000000);
            textPaint.setShadowLayer(4f * density * scale, 0f,
                    1.5f * density * scale, 0x66000000);
        } else {
            fillPaint.clearShadowLayer();
            textPaint.clearShadowLayer();
        }

        float iconHeight = calculateStatusIconHeight(getHeight(), density, scale);
        float centerY = getHeight() / 2f;
        float gap = 8f * density * scale;
        if (contentAlignedStart) {
            drawAlignedStart(canvas, iconHeight, centerY, gap);
            fillPaint.setAlpha(255);
            return;
        }
        float right = getWidth() - getPaddingRight();

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

    private void drawAlignedStart(Canvas canvas, float iconHeight, float centerY, float gap) {
        float cursor = getPaddingLeft();
        float networkSize = iconHeight * 1.15f;
        switch (networkState) {
            case WIFI: drawWifi(canvas, cursor, centerY, networkSize); break;
            case MOBILE: drawMobile(canvas, cursor, centerY, networkSize); break;
            case ETHERNET: drawIcon(canvas, MaterialIcon.ETHERNET, cursor, centerY, networkSize); break;
            default: drawIcon(canvas, MaterialIcon.GLOBE_CANCEL, cursor, centerY, networkSize); break;
        }
        cursor += networkSize + gap * 0.4f;
        if (batteryLevel < 0) return;
        float batterySize = iconHeight * 1.55f;
        drawBattery(canvas, cursor, centerY, batterySize);
        cursor += batterySize + gap * 0.6f;
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setTextSize(iconHeight * 0.9f);
        if (backgroundRepository != null) {
            textPaint.setTypeface(ClockTypefaceResolver.resolveTime(
                    getContext(), backgroundRepository.getFontFamily(), false));
        }
        canvas.drawText(batteryLevel + "%", cursor,
                centerY - (textPaint.ascent() + textPaint.descent()) / 2f, textPaint);
        textPaint.setTextAlign(Paint.Align.RIGHT);
    }

    /** True when the receivers have reported a connectivity state to display. */
    private boolean hasNetworkInfo() {
        return receiverRegistered;
    }

    private void drawIcon(Canvas canvas, MaterialIcon icon, float left, float centerY, float size) {
        drawStyledIcon(canvas, icon, icon == MaterialIcon.ETHERNET
                ? StatusSymbolRenderer.ETHERNET : StatusSymbolRenderer.GLOBE_CANCEL,
                left, centerY, size);
    }

    private void drawBattery(Canvas canvas, float left, float centerY, float size) {
        MaterialIcon icon;
        int codePoint;
        if (batteryCharging) {
            icon = MaterialIcon.BATTERY_BOLT;
            codePoint = StatusSymbolRenderer.BATTERY_BOLT;
        } else {
            // Map 0..100% onto the eight Battery Android glyphs
            // (Battery Android 0..6 then Battery Android Full).
            int level = Math.round(batteryLevel / 100f * (MaterialIcon.BATTERY_LEVELS.length - 1));
            level = Math.max(0, Math.min(MaterialIcon.BATTERY_LEVELS.length - 1, level));
            icon = MaterialIcon.BATTERY_LEVELS[level];
            codePoint = BATTERY_GLYPHS[level];
        }
        drawStyledIcon(canvas, icon, codePoint, left, centerY, size);
    }

    private void drawWifi(Canvas canvas, float left, float centerY, float size) {
        int bars = Math.max(0, Math.min(4, signalStrength));
        MaterialIcon icon = MaterialIcon.WIFI_BARS[bars];
        drawStyledIcon(canvas, icon, WIFI_GLYPHS[bars], left, centerY, size);
    }

    private void drawMobile(Canvas canvas, float left, float centerY, float size) {
        int bars = Math.max(0, Math.min(4, signalStrength));
        MaterialIcon icon = MaterialIcon.CELLULAR_BARS[bars];
        drawStyledIcon(canvas, icon, CELLULAR_GLYPHS[bars], left, centerY, size);
    }

    private void drawStyledIcon(Canvas canvas, MaterialIcon icon, int codePoint,
            float left, float centerY, float size) {
        fillPaint.setColor(resolveTint());
        fillPaint.setAlpha(255);
        if (statusIconStyle != null && !statusIconStyle.isDefault()
                && StatusSymbolRenderer.draw(canvas, getContext(), codePoint,
                        statusIconStyle, left, centerY - size / 2f, size, fillPaint)) {
            return;
        }
        icon.draw(canvas, left, centerY - size / 2f, size, fillPaint);
    }

    @Override
    protected void onDetachedFromWindow() {
        unregisterReceivers();
        super.onDetachedFromWindow();
    }
}
