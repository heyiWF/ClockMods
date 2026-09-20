package com.clockmods.time

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/** Provides local or monotonic-projected NTP time without blocking the UI thread. */
open class NetworkTimeProvider {
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val sntpClient = SntpClient()
    private val syncInFlight = AtomicBoolean(false)
    @Volatile private var enabled = false
    @Volatile private var syncIntervalMs = DEFAULT_SYNC_INTERVAL_MS
    @Volatile private var hasSample = false
    @Volatile private var ntpTime = 0L
    @Volatile private var ntpTimeReference = 0L
    @Volatile private var lastAttemptReference = Long.MIN_VALUE
    @Volatile private var lastAttemptSucceeded = false
    @Volatile private var preferredHostIndex = 0

    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        if (enabled) maybeSync()
    }

    fun isEnabled(): Boolean = enabled

    fun setSyncIntervalMinutes(minutes: Int) {
        val newInterval = maxOf(1, minutes).toLong() * 60L * 1000L
        if (newInterval != syncIntervalMs) {
            syncIntervalMs = newInterval
            if (enabled) maybeSync()
        }
    }

    fun currentTimeMillis(): Long {
        if (enabled) {
            maybeSync()
            if (hasSample) {
                val elapsed = SystemClock.elapsedRealtime() - ntpTimeReference
                if (elapsed >= 0 && elapsed <= SAMPLE_MAX_AGE_MS) return ntpTime + elapsed
            }
        }
        return System.currentTimeMillis()
    }

    private fun maybeSync() {
        if (!enabled || syncInFlight.get() || !isSyncDue()) return
        if (!syncInFlight.compareAndSet(false, true)) return
        executor.execute {
            var success = false
            var serverTime = 0L
            var reference = 0L
            for (offset in NTP_HOSTS.indices) {
                if (success) break
                val index = (preferredHostIndex + offset) % NTP_HOSTS.size
                try {
                    if (sntpClient.requestTime(NTP_HOSTS[index])) {
                        serverTime = sntpClient.getNtpTime()
                        reference = sntpClient.getNtpTimeReference()
                        preferredHostIndex = index
                        success = true
                    }
                } catch (_: RuntimeException) {
                    // Try the next server.
                }
            }
            val capturedTime = serverTime
            val capturedReference = reference
            mainHandler.post {
                if (success) {
                    ntpTime = capturedTime
                    ntpTimeReference = capturedReference
                    hasSample = true
                }
                lastAttemptReference = SystemClock.elapsedRealtime()
                lastAttemptSucceeded = success
                syncInFlight.set(false)
            }
        }
    }

    private fun isSyncDue(): Boolean {
        if (lastAttemptReference == Long.MIN_VALUE) return true
        val sinceAttempt = SystemClock.elapsedRealtime() - lastAttemptReference
        val interval = if (lastAttemptSucceeded) syncIntervalMs else RETRY_INTERVAL_MS
        return sinceAttempt >= interval
    }

    fun shutdown() {
        executor.shutdownNow()
        mainHandler.removeCallbacksAndMessages(null)
    }

    companion object {
        private val NTP_HOSTS = arrayOf(
            "ntp.aliyun.com", "ntp.tencent.com", "ntp.ntsc.ac.cn", "cn.pool.ntp.org", "time.apple.com",
        )
        private const val DEFAULT_SYNC_INTERVAL_MS = 60L * 60L * 1000L
        private const val SAMPLE_MAX_AGE_MS = 24L * 60L * 60L * 1000L
        private const val RETRY_INTERVAL_MS = 60L * 1000L
    }
}
