package com.clockmods.time;

import android.os.SystemClock;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Minimal SNTP (Simple Network Time Protocol, RFC 4330) client.
 *
 * <p>After a successful {@link #requestTime(String)} call, {@link #getNtpTime()}
 * returns the server time in milliseconds since the Unix epoch and
 * {@link #getNtpTimeReference()} returns the {@link SystemClock#elapsedRealtime()}
 * value at which that time was captured, allowing callers to project it forward
 * using the device's monotonic clock.
 *
 * <p>Based on the well-known AOSP {@code SntpClient} algorithm.
 */
class SntpClient {
    private static final int NTP_PORT = 123;
    private static final int NTP_PACKET_SIZE = 48;
    private static final int NTP_MODE_CLIENT = 3;
    private static final int NTP_VERSION = 3;
    private static final int TIMEOUT_MS = 5000;

    // Offset of the transmit timestamp within the NTP packet.
    private static final int TRANSMIT_TIME_OFFSET = 40;
    private static final int ORIGINATE_TIME_OFFSET = 24;
    private static final int RECEIVE_TIME_OFFSET = 32;

    // Seconds between Jan 1, 1900 (NTP epoch) and Jan 1, 1970 (Unix epoch).
    private static final long OFFSET_1900_TO_1970 = ((365L * 70L) + 17L) * 24L * 60L * 60L;

    private long ntpTime;
    private long ntpTimeReference;

    /**
     * Queries the given NTP host for the current time.
     *
     * @return {@code true} if the request succeeded and a valid response was
     *         received; {@code false} otherwise.
     */
    boolean requestTime(String host) {
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket();
            socket.setSoTimeout(TIMEOUT_MS);
            InetAddress address = InetAddress.getByName(host);
            byte[] buffer = new byte[NTP_PACKET_SIZE];
            DatagramPacket request = new DatagramPacket(buffer, buffer.length, address, NTP_PORT);

            // Set mode = 3 (client) and version = 3 in the first byte.
            buffer[0] = (byte) ((NTP_VERSION << 3) | NTP_MODE_CLIENT);

            long requestTime = System.currentTimeMillis();
            long requestTicks = SystemClock.elapsedRealtime();
            writeTimeStamp(buffer, TRANSMIT_TIME_OFFSET, requestTime);

            socket.send(request);

            DatagramPacket response = new DatagramPacket(buffer, buffer.length);
            socket.receive(response);
            long responseTicks = SystemClock.elapsedRealtime();
            long responseTime = requestTime + (responseTicks - requestTicks);

            long originateTime = readTimeStamp(buffer, ORIGINATE_TIME_OFFSET);
            long receiveTime = readTimeStamp(buffer, RECEIVE_TIME_OFFSET);
            long transmitTime = readTimeStamp(buffer, TRANSMIT_TIME_OFFSET);

            // Round-trip delay and clock offset per RFC 4330.
            long roundTripTime = (responseTicks - requestTicks) - (transmitTime - receiveTime);
            long clockOffset = ((receiveTime - originateTime) + (transmitTime - responseTime)) / 2;

            ntpTime = responseTime + clockOffset;
            ntpTimeReference = responseTicks;
            return true;
        } catch (Exception ignored) {
            return false;
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
    }

    /** @return the server time in milliseconds since the Unix epoch. */
    long getNtpTime() {
        return ntpTime;
    }

    /** @return {@link SystemClock#elapsedRealtime()} when {@link #getNtpTime()} was captured. */
    long getNtpTimeReference() {
        return ntpTimeReference;
    }

    /** Reads an unsigned 32-bit value from the buffer at the given offset. */
    private long read32(byte[] buffer, int offset) {
        int b0 = buffer[offset] & 0xFF;
        int b1 = buffer[offset + 1] & 0xFF;
        int b2 = buffer[offset + 2] & 0xFF;
        int b3 = buffer[offset + 3] & 0xFF;
        return ((long) b0 << 24) | ((long) b1 << 16) | ((long) b2 << 8) | (long) b3;
    }

    /** Reads an NTP timestamp at the given offset and converts it to Unix millis. */
    private long readTimeStamp(byte[] buffer, int offset) {
        long seconds = read32(buffer, offset);
        long fraction = read32(buffer, offset + 4);
        return ((seconds - OFFSET_1900_TO_1970) * 1000L) + ((fraction * 1000L) / 0x100000000L);
    }

    /** Writes an NTP timestamp (from Unix millis) into the buffer at the given offset. */
    private void writeTimeStamp(byte[] buffer, int offset, long time) {
        long seconds = time / 1000L;
        long milliseconds = time - seconds * 1000L;
        seconds += OFFSET_1900_TO_1970;

        buffer[offset] = (byte) (seconds >> 24);
        buffer[offset + 1] = (byte) (seconds >> 16);
        buffer[offset + 2] = (byte) (seconds >> 8);
        buffer[offset + 3] = (byte) seconds;

        long fraction = milliseconds * 0x100000000L / 1000L;
        buffer[offset + 4] = (byte) (fraction >> 24);
        buffer[offset + 5] = (byte) (fraction >> 16);
        buffer[offset + 6] = (byte) (fraction >> 8);
        // Low-order byte is set to a random value to avoid transmit-time collisions.
        buffer[offset + 7] = (byte) (Math.random() * 255.0);
    }
}
