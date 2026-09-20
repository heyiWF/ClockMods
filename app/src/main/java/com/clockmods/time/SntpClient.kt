package com.clockmods.time

import android.os.SystemClock
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/** Minimal SNTP client based on the AOSP implementation. */
open class SntpClient {
    private var ntpTime: Long = 0
    private var ntpTimeReference: Long = 0

    fun requestTime(host: String): Boolean {
        var socket: DatagramSocket? = null
        return try {
            socket = DatagramSocket()
            socket.soTimeout = TIMEOUT_MS
            val address = InetAddress.getByName(host)
            val buffer = ByteArray(NTP_PACKET_SIZE)
            val request = DatagramPacket(buffer, buffer.size, address, NTP_PORT)
            buffer[0] = ((NTP_VERSION shl 3) or NTP_MODE_CLIENT).toByte()
            val requestTime = System.currentTimeMillis()
            val requestTicks = SystemClock.elapsedRealtime()
            writeTimeStamp(buffer, TRANSMIT_TIME_OFFSET, requestTime)
            socket.send(request)
            val response = DatagramPacket(buffer, buffer.size)
            socket.receive(response)
            val responseTicks = SystemClock.elapsedRealtime()
            val responseTime = requestTime + (responseTicks - requestTicks)
            val leap = (buffer[0].toInt() ushr 6) and 0x3
            val mode = buffer[0].toInt() and 0x7
            val stratum = buffer[1].toInt() and 0xff
            if (leap == NTP_LEAP_NOSYNC ||
                (mode != NTP_MODE_SERVER && mode != NTP_MODE_BROADCAST) ||
                stratum !in 1..NTP_STRATUM_MAX
            ) return false
            val originateTime = readTimeStamp(buffer, ORIGINATE_TIME_OFFSET)
            val receiveTime = readTimeStamp(buffer, RECEIVE_TIME_OFFSET)
            val transmitTime = readTimeStamp(buffer, TRANSMIT_TIME_OFFSET)
            // Keep the RFC round-trip calculation for parity with the original algorithm.
            @Suppress("UNUSED_VARIABLE")
            val roundTripTime = (responseTicks - requestTicks) - (transmitTime - receiveTime)
            val clockOffset = ((receiveTime - originateTime) + (transmitTime - responseTime)) / 2
            ntpTime = responseTime + clockOffset
            ntpTimeReference = responseTicks
            true
        } catch (_: Exception) {
            false
        } finally {
            socket?.close()
        }
    }

    fun getNtpTime(): Long = ntpTime
    fun getNtpTimeReference(): Long = ntpTimeReference

    private fun read32(buffer: ByteArray, offset: Int): Long =
        ((buffer[offset].toLong() and 0xff) shl 24) or
            ((buffer[offset + 1].toLong() and 0xff) shl 16) or
            ((buffer[offset + 2].toLong() and 0xff) shl 8) or
            (buffer[offset + 3].toLong() and 0xff)

    private fun readTimeStamp(buffer: ByteArray, offset: Int): Long {
        val seconds = read32(buffer, offset)
        val fraction = read32(buffer, offset + 4)
        return ((seconds - OFFSET_1900_TO_1970) * 1000L) +
            ((fraction * 1000L) / 0x100000000L)
    }

    private fun writeTimeStamp(buffer: ByteArray, offset: Int, time: Long) {
        var seconds = time / 1000L
        val milliseconds = time - seconds * 1000L
        seconds += OFFSET_1900_TO_1970
        buffer[offset] = (seconds shr 24).toByte()
        buffer[offset + 1] = (seconds shr 16).toByte()
        buffer[offset + 2] = (seconds shr 8).toByte()
        buffer[offset + 3] = seconds.toByte()
        val fraction = milliseconds * 0x100000000L / 1000L
        buffer[offset + 4] = (fraction shr 24).toByte()
        buffer[offset + 5] = (fraction shr 16).toByte()
        buffer[offset + 6] = (fraction shr 8).toByte()
        buffer[offset + 7] = (Math.random() * 255.0).toInt().toByte()
    }

    companion object {
        private const val NTP_PORT = 123
        private const val NTP_PACKET_SIZE = 48
        private const val NTP_MODE_CLIENT = 3
        private const val NTP_MODE_SERVER = 4
        private const val NTP_MODE_BROADCAST = 5
        private const val NTP_VERSION = 3
        private const val NTP_LEAP_NOSYNC = 3
        private const val NTP_STRATUM_MAX = 15
        private const val TIMEOUT_MS = 5000
        private const val TRANSMIT_TIME_OFFSET = 40
        private const val ORIGINATE_TIME_OFFSET = 24
        private const val RECEIVE_TIME_OFFSET = 32
        private const val OFFSET_1900_TO_1970 = ((365L * 70L) + 17L) * 24L * 60L * 60L
    }
}
