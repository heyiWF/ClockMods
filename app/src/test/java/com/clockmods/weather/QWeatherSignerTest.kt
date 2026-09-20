package com.clockmods.weather

import java.nio.charset.StandardCharsets
import java.security.Security
import net.i2p.crypto.eddsa.EdDSAPrivateKey
import net.i2p.crypto.eddsa.EdDSAPublicKey
import net.i2p.crypto.eddsa.EdDSASecurityProvider
import net.i2p.crypto.eddsa.EdDSAEngine
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable
import net.i2p.crypto.eddsa.spec.EdDSAParameterSpec
import net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec
import org.apache.commons.codec.binary.Base64
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test

class QWeatherSignerTest {
    @Test
    fun createsVerifiableQWeatherJwt() {
        if (Security.getProvider(EdDSASecurityProvider.PROVIDER_NAME) == null) Security.addProvider(EdDSASecurityProvider())
        val seed = ByteArray(32) { it.toByte() }
        val spec: EdDSAParameterSpec = EdDSANamedCurveTable.getByName(EdDSANamedCurveTable.ED_25519)
        val privateKey = EdDSAPrivateKey(EdDSAPrivateKeySpec(seed, spec))
        val key = Base64.encodeBase64String(privateKey.encoded)
        val token = QWeatherSigner.token("CREDENTIAL", "DEVELOPER", "PROJECT", key, 1000L)
        val parts = token.split(".")
        Assert.assertEquals(3, parts.size)
        val header = json(parts[0])
        val payload = json(parts[1])
        Assert.assertEquals("EdDSA", header.getString("alg"))
        Assert.assertEquals("CREDENTIAL", header.getString("kid"))
        Assert.assertEquals("DEVELOPER", payload.getString("iss"))
        Assert.assertEquals("PROJECT", payload.getString("sub"))
        Assert.assertEquals(970L, payload.getLong("iat"))
        Assert.assertEquals(1870L, payload.getLong("exp"))
        Assert.assertFalse(token.contains("="))
        val verifier = EdDSAEngine()
        verifier.initVerify(EdDSAPublicKey(EdDSAPublicKeySpec(privateKey.abyte, spec)))
        verifier.update("${parts[0]}.${parts[1]}".toByteArray(StandardCharsets.UTF_8))
        Assert.assertTrue(verifier.verify(Base64.decodeBase64(parts[2])))
    }

    private fun json(encoded: String): JSONObject = JSONObject(String(Base64.decodeBase64(encoded), StandardCharsets.UTF_8))
}
