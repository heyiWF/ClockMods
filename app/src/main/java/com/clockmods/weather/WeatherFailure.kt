package com.clockmods.weather

import java.security.cert.CertificateException
import java.security.cert.CertificateExpiredException
import java.security.cert.CertificateNotYetValidException
import java.security.cert.CertPathValidatorException
import java.util.Collections
import java.util.IdentityHashMap

/** Classify nested TLS failures without changing certificate validation. */
internal object WeatherFailure {
    enum class CertificateProblem { TIME, TRUST }

    fun certificateProblem(error: Throwable): CertificateProblem? {
        val visited = Collections.newSetFromMap(IdentityHashMap<Throwable, Boolean>())
        var cause: Throwable? = error
        var certificate = false
        while (cause != null && visited.add(cause)) {
            if (cause is CertificateExpiredException || cause is CertificateNotYetValidException) {
                return CertificateProblem.TIME
            }
            if (cause is CertificateException || cause is CertPathValidatorException) certificate = true
            cause = cause.cause
        }
        return if (certificate) CertificateProblem.TRUST else null
    }
}
