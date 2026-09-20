package com.clockmods.weather

import net.i2p.crypto.eddsa.EdDSAPrivateKey
import net.i2p.crypto.eddsa.EdDSASecurityProvider
import net.i2p.crypto.eddsa.EdDSAEngine
import java.nio.charset.StandardCharsets
import java.security.PrivateKey
import java.security.Security
import java.security.spec.PKCS8EncodedKeySpec

object QWeatherSigner {
    private const val BASE64_URL = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"

    @JvmStatic
    @Throws(Exception::class)
    fun token(
        credentialId: String,
        developerId: String,
        projectId: String,
        privateKeyBase64: String,
        nowSeconds: Long,
    ): String {
        if (Security.getProvider(EdDSASecurityProvider.PROVIDER_NAME) == null) {
            Security.addProvider(EdDSASecurityProvider())
        }
        val header = "{\"alg\":\"EdDSA\",\"kid\":\"$credentialId\"}"
        val issuedAt = nowSeconds - 30L
        val payload = "{\"iss\":\"$developerId\",\"sub\":\"$projectId\",\"iat\":$issuedAt,\"exp\":${issuedAt + 900L}}"
        val signingInput = base64Url(header.toByteArray(StandardCharsets.UTF_8)) + "." +
            base64Url(payload.toByteArray(StandardCharsets.UTF_8))
        val privateKey: PrivateKey = EdDSAPrivateKey(PKCS8EncodedKeySpec(decodeBase64(privateKeyBase64)))
        val engine = EdDSAEngine()
        engine.initSign(privateKey)
        val signature = engine.signOneShot(signingInput.toByteArray(StandardCharsets.UTF_8))
        return "$signingInput.${base64Url(signature)}"
    }

    private fun base64Url(value: ByteArray): String {
        val encoded = StringBuilder((value.size * 4 + 2) / 3)
        var index = 0
        while (index < value.size) {
            val first = value[index].toInt() and 0xff
            val second = if (index + 1 < value.size) value[index + 1].toInt() and 0xff else 0
            val third = if (index + 2 < value.size) value[index + 2].toInt() and 0xff else 0
            encoded.append(BASE64_URL[first ushr 2])
            encoded.append(BASE64_URL[((first and 0x03) shl 4) or (second ushr 4)])
            if (index + 1 < value.size) encoded.append(BASE64_URL[((second and 0x0f) shl 2) or (third ushr 6)])
            if (index + 2 < value.size) encoded.append(BASE64_URL[third and 0x3f])
            index += 3
        }
        return encoded.toString()
    }

    private fun decodeBase64(value: String): ByteArray {
        val cleaned = buildString(value.length) {
            value.forEach { if (!it.isWhitespace() && it != '=') append(it) }
        }
        val decoded = ByteArray(cleaned.length * 6 / 8)
        var buffer = 0
        var bits = 0
        var outputIndex = 0
        cleaned.forEach { character ->
            val digit = base64Digit(character)
            require(digit >= 0) { "Invalid Base64 private key" }
            buffer = (buffer shl 6) or digit
            bits += 6
            if (bits >= 8) {
                bits -= 8
                if (outputIndex < decoded.size) decoded[outputIndex++] = (buffer shr bits).toByte()
                buffer = buffer and ((1 shl bits) - 1)
            }
        }
        return decoded
    }

    private fun base64Digit(character: Char): Int = when {
        character in 'A'..'Z' -> character - 'A'
        character in 'a'..'z' -> character - 'a' + 26
        character in '0'..'9' -> character - '0' + 52
        character == '+' || character == '-' -> 62
        character == '/' || character == '_' -> 63
        else -> -1
    }
}
