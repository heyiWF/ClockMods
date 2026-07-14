package com.clockmods.weather;

import net.i2p.crypto.eddsa.EdDSAPrivateKey;
import net.i2p.crypto.eddsa.EdDSAPublicKey;
import net.i2p.crypto.eddsa.EdDSASecurityProvider;
import net.i2p.crypto.eddsa.EdDSAEngine;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;
import net.i2p.crypto.eddsa.spec.EdDSAParameterSpec;
import net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec;
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.security.Security;

import org.apache.commons.codec.binary.Base64;

public class QWeatherSignerTest {
    @Test
    public void createsVerifiableQWeatherJwt() throws Exception {
        if (Security.getProvider(EdDSASecurityProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new EdDSASecurityProvider());
        }
        byte[] seed = new byte[32];
        for (int index = 0; index < seed.length; index++) seed[index] = (byte) index;
        EdDSAParameterSpec spec = EdDSANamedCurveTable.getByName(EdDSANamedCurveTable.ED_25519);
        EdDSAPrivateKey privateKey = new EdDSAPrivateKey(new EdDSAPrivateKeySpec(seed, spec));
        String key = Base64.encodeBase64String(privateKey.getEncoded());
        String token = QWeatherSigner.token("CREDENTIAL", "PROJECT", key, 1000L);
        String[] parts = token.split("\\.");
        Assert.assertEquals(3, parts.length);
        JSONObject header = json(parts[0]);
        JSONObject payload = json(parts[1]);
        Assert.assertEquals("EdDSA", header.getString("alg"));
        Assert.assertEquals("CREDENTIAL", header.getString("kid"));
        Assert.assertEquals("PROJECT", payload.getString("sub"));
        Assert.assertEquals(970L, payload.getLong("iat"));
        Assert.assertEquals(1870L, payload.getLong("exp"));
        Assert.assertFalse(token.contains("="));

        EdDSAEngine verifier = new EdDSAEngine();
        verifier.initVerify(new EdDSAPublicKey(new EdDSAPublicKeySpec(privateKey.getAbyte(), spec)));
        verifier.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.UTF_8));
        Assert.assertTrue(verifier.verify(Base64.decodeBase64(parts[2])));
    }

    private static JSONObject json(String encoded) throws Exception {
        return new JSONObject(new String(Base64.decodeBase64(encoded), StandardCharsets.UTF_8));
    }
}