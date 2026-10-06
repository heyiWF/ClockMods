package com.clockmods.time;

import org.junit.Test;
import static org.junit.Assert.*;

public class SntpClientTest {
    private final byte[] request = {1, 2, 3, 4, 5, 6, 7, 8};
    private byte[] response() {
        byte[] packet = new byte[48];
        packet[0] = 0x24; packet[1] = 2;
        System.arraycopy(request, 0, packet, 24, 8);
        packet[32] = 1; packet[40] = 1;
        return packet;
    }
    @Test public void acceptsMatchingServerReply() {
        assertTrue(SntpClient.isValidResponse(response(), 48, request));
    }
    @Test public void rejectsTruncatedAndUnsolicitedReplies() {
        byte[] packet = response();
        assertFalse(SntpClient.isValidResponse(packet, 47, request));
        packet[31] ^= 1;
        assertFalse(SntpClient.isValidResponse(packet, 48, request));
    }
    @Test public void rejectsUnsynchronizedBroadcastAndInvalidVersion() {
        for (int header : new int[] {0xe4, 0x25, 0x04, 0x3c}) {
            byte[] packet = response(); packet[0] = (byte) header;
            assertFalse(SntpClient.isValidResponse(packet, 48, request));
        }
    }
    @Test public void rejectsKissOfDeathAndAbsentTimestamps() {
        byte[] packet = response(); packet[1] = 0;
        assertFalse(SntpClient.isValidResponse(packet, 48, request));
        packet = response(); packet[32] = 0;
        assertFalse(SntpClient.isValidResponse(packet, 48, request));
        packet = response(); packet[40] = 0;
        assertFalse(SntpClient.isValidResponse(packet, 48, request));
    }
}
