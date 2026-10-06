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
            socket.connect(address, NTP_PORT)
            val buffer = ByteArray(NTP_PACKET_SIZE)
            val request = DatagramPacket(buffer, buffer.size, address, NTP_PORT)
            buffer[0] = ((NTP_VERSION shl 3) or NTP_MODE_CLIENT).toByte()
            val requestTime = System.currentTimeMillis()
            val requestTicks = SystemClock.elapsedRealtime()
            writeTimeStamp(buffer, TRANSMIT_TIME_OFFSET, requestTime)
            val requestTimestamp = buffer.copyOfRange(40, 48)
            socket.send(request)
            val response = DatagramPacket(buffer, buffer.size)
            socket.receive(response)
            if (!isValidResponse(buffer, response.length, requestTimestamp)) return false
            val responseTicks = SystemClock.elapsedRealtime()
            val responseTime = requestTime + (responseTicks - requestTicks)
            val originateTime = readTimeStamp(buffer, ORIGINATE_TIME_OFFSET)
            val receiveTime = readTimeStamp(buffer, RECEIVE_TIME_OFFSET)
            val transmitTime = readTimeStamp(buffer, TRANSMIT_TIME_OFFSET)
            if (transmitTime < receiveTime || transmitTime - receiveTime > responseTicks - requestTicks + 1L) return false
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
        @JvmStatic fun isValidResponse(packet: ByteArray?, length: Int, requestTimestamp: ByteArray?): Boolean {
            if (packet == null || length < 48 || packet.size < 48 || requestTimestamp == null || requestTimestamp.size != 8) return false
            val leap = (packet[0].toInt() ushr 6) and 3
            val version = (packet[0].toInt() ushr 3) and 7
            val mode = packet[0].toInt() and 7
            val stratum = packet[1].toInt() and 255
            if (leap == 3 || version !in 3..4 || mode != 4 || stratum !in 1..15) return false
            return (0..7).all { packet[24 + it] == requestTimestamp[it] } &&
                (32..39).any { packet[it] != 0.toByte() } && (40..47).any { packet[it] != 0.toByte() }
        }

        private const val NTP_PORT = 123
        private const val NTP_PACKET_SIZE = 48
        private const val NTP_MODE_CLIENT = 3
        private const val NTP_VERSION = 3
        private const val TIMEOUT_MS = 5000
        private const val TRANSMIT_TIME_OFFSET = 40
        private const val ORIGINATE_TIME_OFFSET = 24
        private const val RECEIVE_TIME_OFFSET = 32
        private const val OFFSET_1900_TO_1970 = ((365L * 70L) + 17L) * 24L * 60L * 60L
    }
}
