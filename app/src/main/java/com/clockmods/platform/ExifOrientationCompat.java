package com.clockmods.platform;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Pure-Java EXIF orientation reader for the compat flavor (minSdk 1).
 *
 * <p>{@code androidx.exifinterface} declares {@code minSdk=14} in its manifest, which would
 * block the manifest merger for compat. This implementation parses only the JPEG APP1/EXIF
 * Orientation tag (0x0112) and falls back to 0 degrees when the data is absent or malformed,
 * matching the behaviour of the AndroidX implementation used by the other flavors.</p>
 */
public final class ExifOrientationCompat {
    private static final int TAG_ORIENTATION = 0x0112;
    private static final byte[] EXIF_HEADER = {'E', 'x', 'i', 'f', 0, 0};

    private ExifOrientationCompat() {
    }

    /** @return rotation in degrees (0/90/180/270), or 0 when unknown. */
    public static int readOrientationDegrees(String path) {
        if (path == null) {
            return 0;
        }
        InputStream stream = null;
        try {
            stream = new BufferedInputStream(new FileInputStream(path));
            java.io.DataInputStream in = new java.io.DataInputStream(stream);
            if (in.readUnsignedShort() != 0xFFD8) {
                return 0;
            }
            while (true) {
                int prefix = in.read();
                if (prefix != 0xFF) {
                    return 0;
                }
                int marker;
                do {
                    marker = in.read();
                } while (marker == 0xFF);
                if (marker < 0 || marker == 0xD9 || marker == 0xDA) {
                    return 0;
                }
                int segmentLength = in.readUnsignedShort();
                if (segmentLength < 2) {
                    return 0;
                }
                int consumed = 0;
                if (marker == 0xE1) {
                    byte[] identifier = new byte[EXIF_HEADER.length];
                    in.readFully(identifier);
                    consumed += identifier.length;
                    if (matchesExifHeader(identifier)) {
                        int orientation = readExifOrientation(in, segmentLength - consumed);
                        if (orientation > 0) {
                            return orientation;
                        }
                    }
                }
                int remaining = segmentLength - consumed;
                while (remaining > 0) {
                    int skipped = in.skipBytes(remaining);
                    if (skipped <= 0) {
                        return 0;
                    }
                    remaining -= skipped;
                }
            }
        } catch (IOException | RuntimeException ignored) {
            return 0;
        } finally {
            closeQuietly(stream);
        }
    }

    /**
     * Reads the TIFF header and IFD0 entry table that immediately follow the "Exif\0\0"
     * identifier. {@code available} is the number of bytes left in the APP1 segment.
     */
    private static int readExifOrientation(java.io.DataInputStream in, int available)
            throws IOException {
        if (available < 8) {
            return 0;
        }
        byte[] tiff = new byte[8];
        in.readFully(tiff);
        int consumed = 8;
        boolean littleEndian = tiff[0] == 'I' && tiff[1] == 'I';
        if (!littleEndian && !(tiff[0] == 'M' && tiff[1] == 'M')) {
            return 0;
        }
        if (readUnsigned16(tiff, 2, littleEndian) != 42) {
            return 0;
        }
        long ifdOffset = readUnsigned32(tiff, 4, littleEndian);
        if (ifdOffset < 8 || ifdOffset > available - 2) {
            return 0;
        }
        int skip = (int) (ifdOffset - 8);
        while (skip > 0) {
            int skipped = in.skipBytes(skip);
            if (skipped <= 0) {
                return 0;
            }
            skip -= skipped;
        }
        consumed += (int) ifdOffset;
        if (available - consumed < 2) {
            return 0;
        }
        int entryCount = in.readUnsignedShort();
        consumed += 2;
        for (int index = 0; index < entryCount; index++) {
            if (available - consumed < 12) {
                return 0;
            }
            byte[] entry = new byte[12];
            in.readFully(entry);
            consumed += 12;
            int tag = readUnsigned16(entry, 0, littleEndian);
            int type = readUnsigned16(entry, 2, littleEndian);
            long count = readUnsigned32(entry, 4, littleEndian);
            if (tag == TAG_ORIENTATION && type == 3 && count >= 1) {
                int raw = readUnsigned16(entry, 8, littleEndian);
                return toDegrees(raw);
            }
        }
        return 0;
    }

    private static boolean matchesExifHeader(byte[] identifier) {
        for (int index = 0; index < EXIF_HEADER.length; index++) {
            if (identifier[index] != EXIF_HEADER[index]) {
                return false;
            }
        }
        return true;
    }

    private static int readUnsigned16(byte[] data, int offset, boolean littleEndian) {
        if (littleEndian) {
            return (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8);
        }
        return ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF);
    }

    private static long readUnsigned32(byte[] data, int offset, boolean littleEndian) {
        long value = 0;
        if (littleEndian) {
            for (int index = 3; index >= 0; index--) {
                value = (value << 8) | (data[offset + index] & 0xFFL);
            }
        } else {
            for (int index = 0; index < 4; index++) {
                value = (value << 8) | (data[offset + index] & 0xFFL);
            }
        }
        return value;
    }

    private static int toDegrees(int orientation) {
        if (orientation == 6) return 90;
        if (orientation == 3) return 180;
        if (orientation == 8) return 270;
        return 0;
    }

    private static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ignored) {
                // Nothing useful can be done while closing a failed stream.
            }
        }
    }
}
