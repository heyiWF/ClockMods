package com.clockmods.weather;

import net.i2p.crypto.eddsa.EdDSAPrivateKey;
import net.i2p.crypto.eddsa.EdDSASecurityProvider;
import net.i2p.crypto.eddsa.EdDSAEngine;

import java.security.PrivateKey;
import java.security.Security;
import java.security.spec.PKCS8EncodedKeySpec;

public final class QWeatherSigner {
    private static final char[] BASE64_URL =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_".toCharArray();

    private QWeatherSigner() { }

        public static String token(String credentialId, String projectId, String privateKeyBase64,
            long nowSeconds) throws Exception {
        if (Security.getProvider(EdDSASecurityProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new EdDSASecurityProvider());
        }
        String header = "{\"alg\":\"EdDSA\",\"kid\":\"" + credentialId + "\"}";
        long issuedAt = nowSeconds - 30L;
        String payload = "{\"sub\":\"" + projectId + "\",\"iat\":" + issuedAt
            + ",\"exp\":" + (issuedAt + 900L) + "}";
        String signingInput = base64Url(header.getBytes("UTF-8")) + "."
            + base64Url(payload.getBytes("UTF-8"));
        byte[] privateKeyBytes = decodeBase64(privateKeyBase64);
        PrivateKey privateKey = new EdDSAPrivateKey(new PKCS8EncodedKeySpec(privateKeyBytes));
        EdDSAEngine engine = new EdDSAEngine();
        engine.initSign(privateKey);
        byte[] signature = engine.signOneShot(signingInput.getBytes("UTF-8"));
        return signingInput + "." + base64Url(signature);
        }

    private static String base64Url(byte[] value) {
        StringBuilder encoded = new StringBuilder((value.length * 4 + 2) / 3);
        for (int index = 0; index < value.length; index += 3) {
            int first = value[index] & 0xff;
            int second = index + 1 < value.length ? value[index + 1] & 0xff : 0;
            int third = index + 2 < value.length ? value[index + 2] & 0xff : 0;
            encoded.append(BASE64_URL[first >>> 2]);
            encoded.append(BASE64_URL[((first & 0x03) << 4) | (second >>> 4)]);
            if (index + 1 < value.length) {
                encoded.append(BASE64_URL[((second & 0x0f) << 2) | (third >>> 6)]);
            }
            if (index + 2 < value.length) {
                encoded.append(BASE64_URL[third & 0x3f]);
            }
        }
        return encoded.toString();
    }

    private static byte[] decodeBase64(String value) {
        StringBuilder cleaned = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (!Character.isWhitespace(character) && character != '=') cleaned.append(character);
        }
        int outputLength = cleaned.length() * 6 / 8;
        byte[] decoded = new byte[outputLength];
        int buffer = 0;
        int bits = 0;
        int outputIndex = 0;
        for (int index = 0; index < cleaned.length(); index++) {
            int digit = base64Digit(cleaned.charAt(index));
            if (digit < 0) throw new IllegalArgumentException("Invalid Base64 private key");
            buffer = (buffer << 6) | digit;
            bits += 6;
            if (bits >= 8) {
                bits -= 8;
                if (outputIndex < decoded.length) decoded[outputIndex++] = (byte) (buffer >> bits);
                buffer &= (1 << bits) - 1;
            }
        }
        return decoded;
    }

    private static int base64Digit(char character) {
        if (character >= 'A' && character <= 'Z') return character - 'A';
        if (character >= 'a' && character <= 'z') return character - 'a' + 26;
        if (character >= '0' && character <= '9') return character - '0' + 52;
        if (character == '+' || character == '-') return 62;
        if (character == '/' || character == '_') return 63;
        return -1;
    }
}