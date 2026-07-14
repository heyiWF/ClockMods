package com.clockmods.time;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Provides the current time in milliseconds, optionally sourced from an NTP
 * server ({@code ntp.aliyun.com}).
 *
 * <p>When network time is enabled the provider periodically queries the NTP
 * server on a background thread and stores the offset between the server time
 * and the device's monotonic clock ({@link SystemClock#elapsedRealtime()}).
 * {@link #currentTimeMillis()} then projects the last known server time forward
 * using that monotonic clock, so it never blocks the UI thread and does not
 * drift when the wall clock is changed.
 *
 * <p>If network time is disabled, or a fresh sample has never been obtained,
 * the device local time ({@link System#currentTimeMillis()}) is returned.
 */
public class NetworkTimeProvider {
    private static final String NTP_HOST = "ntp.aliyun.com";

    /** Default re-sync interval while enabled (used until configured). */
    private static final long DEFAULT_SYNC_INTERVAL_MS = 60L * 60L * 1000L;
    /** A cached sample older than this is considered stale and unusable. */
    private static final long SAMPLE_MAX_AGE_MS = 24L * 60L * 60L * 1000L;
    /** Retry sooner after a failed sync attempt. */
    private static final long RETRY_INTERVAL_MS = 60L * 1000L;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final SntpClient sntpClient = new SntpClient();
    private final AtomicBoolean syncInFlight = new AtomicBoolean(false);

    private volatile boolean enabled;
    /** Configured re-sync interval, in milliseconds. */
    private volatile long syncIntervalMs = DEFAULT_SYNC_INTERVAL_MS;
    /** Whether {@link #ntpTimeReference} / {@link #ntpTime} hold a valid sample. */
    private volatile boolean hasSample;
    /** Server time (ms since epoch) captured at the last successful sync. */
    private volatile long ntpTime;
    /** Value of {@link SystemClock#elapsedRealtime()} when {@link #ntpTime} was captured. */
    private volatile long ntpTimeReference;
    /** {@link SystemClock#elapsedRealtime()} of the last sync attempt (success or failure). */
    private volatile long lastAttemptReference = Long.MIN_VALUE;
    private volatile boolean lastAttemptSucceeded;

    /** Enables or disables the use of network time. Triggers a sync when enabling. */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (enabled) {
            maybeSync();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets how often network time is re-synchronized while enabled.
     *
     * @param minutes the interval in minutes; values below one are clamped.
     */
    public void setSyncIntervalMinutes(int minutes) {
        long clamped = Math.max(1, minutes);
        long newInterval = clamped * 60L * 1000L;
        if (newInterval != syncIntervalMs) {
            syncIntervalMs = newInterval;
            // A shorter interval may make a sync due immediately.
            if (enabled) {
                maybeSync();
            }
        }
    }

    /**
     * @return the current time in milliseconds. Uses network time when it is
     *         enabled and a fresh sample is available; otherwise the device
     *         local time.
     */
    public long currentTimeMillis() {
        if (enabled) {
            maybeSync();
            if (hasSample) {
                long elapsed = SystemClock.elapsedRealtime() - ntpTimeReference;
                if (elapsed >= 0 && elapsed <= SAMPLE_MAX_AGE_MS) {
                    return ntpTime + elapsed;
                }
            }
        }
        return System.currentTimeMillis();
    }

    /** Starts a background sync if enabled, not already running, and due. */
    private void maybeSync() {
        if (!enabled || syncInFlight.get() || !isSyncDue()) {
            return;
        }
        if (!syncInFlight.compareAndSet(false, true)) {
            return;
        }
        executor.execute(new Runnable() {
            @Override
            public void run() {
                boolean success = false;
                long serverTime = 0L;
                long reference = 0L;
                try {
                    if (sntpClient.requestTime(NTP_HOST)) {
                        serverTime = sntpClient.getNtpTime();
                        reference = sntpClient.getNtpTimeReference();
                        success = true;
                    }
                } catch (RuntimeException ignored) {
                    success = false;
                }
                final boolean ok = success;
                final long capturedTime = serverTime;
                final long capturedReference = reference;
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (ok) {
                            ntpTime = capturedTime;
                            ntpTimeReference = capturedReference;
                            hasSample = true;
                        }
                        lastAttemptReference = SystemClock.elapsedRealtime();
                        lastAttemptSucceeded = ok;
                        syncInFlight.set(false);
                    }
                });
            }
        });
    }

    private boolean isSyncDue() {
        if (lastAttemptReference == Long.MIN_VALUE) {
            return true;
        }
        long sinceAttempt = SystemClock.elapsedRealtime() - lastAttemptReference;
        long interval = lastAttemptSucceeded ? syncIntervalMs : RETRY_INTERVAL_MS;
        return sinceAttempt >= interval;
    }

    /** Releases the background executor. Call from the owner's teardown. */
    public void shutdown() {
        executor.shutdownNow();
        mainHandler.removeCallbacksAndMessages(null);
    }
}
