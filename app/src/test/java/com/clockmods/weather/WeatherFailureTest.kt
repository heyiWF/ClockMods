package com.clockmods.weather

import java.security.cert.CertificateException
import java.security.cert.CertificateExpiredException
import java.security.cert.CertificateNotYetValidException
import javax.net.ssl.SSLHandshakeException
import org.junit.Assert.*
import org.junit.Test

class WeatherFailureTest {
    @Test fun nestedDateFailuresIdentifyClockCheck() {
        for (cause in listOf(CertificateExpiredException(), CertificateNotYetValidException())) {
            val error = SSLHandshakeException("Chain validation failed")
            error.initCause(CertificateException("validation", cause))
            assertEquals(WeatherFailure.CertificateProblem.TIME, WeatherFailure.certificateProblem(error))
        }
    }

    @Test fun untrustedCertificateDoesNotClaimClockFailure() {
        assertEquals(WeatherFailure.CertificateProblem.TRUST,
            WeatherFailure.certificateProblem(CertificateException("untrusted issuer")))
        assertNull(WeatherFailure.certificateProblem(java.io.IOException("offline")))
    }
}
