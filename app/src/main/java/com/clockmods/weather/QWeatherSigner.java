package com.clockmods.weather;

import net.i2p.crypto.eddsa.EdDSAPrivateKey;
import net.i2p.crypto.eddsa.EdDSASecurityProvider;
import net.i2p.crypto.eddsa.EdDSAEngine;

import java.security.PrivateKey;
import java.security.Security;
import java.security.spec.PKCS8EncodedKeySpec;

import org.apache.commons.codec.binary.Base64;

public final class QWeatherSigner {
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
        byte[] privateKeyBytes = Base64.decodeBase64(privateKeyBase64);
        PrivateKey privateKey = new EdDSAPrivateKey(new PKCS8EncodedKeySpec(privateKeyBytes));
        EdDSAEngine engine = new EdDSAEngine();
        engine.initSign(privateKey);
        byte[] signature = engine.signOneShot(signingInput.getBytes("UTF-8"));
        return signingInput + "." + base64Url(signature);
        }

        private static String base64Url(byte[] value) {
        return Base64.encodeBase64URLSafeString(value);
    }
}