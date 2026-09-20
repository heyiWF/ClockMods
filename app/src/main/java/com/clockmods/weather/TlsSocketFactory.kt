package com.clockmods.weather

import android.content.Context
import com.clockmods.R
import java.net.InetAddress
import java.net.Socket
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

class TlsSocketFactory private constructor(private val delegate: SSLSocketFactory) : SSLSocketFactory() {
    override fun getDefaultCipherSuites(): Array<String> = delegate.defaultCipherSuites
    override fun getSupportedCipherSuites(): Array<String> = delegate.supportedCipherSuites
    override fun createSocket(s: Socket, host: String, port: Int, autoClose: Boolean): Socket = enableTls(delegate.createSocket(s, host, port, autoClose))
    override fun createSocket(host: String, port: Int): Socket = enableTls(delegate.createSocket(host, port))
    override fun createSocket(host: String, port: Int, localHost: InetAddress, localPort: Int): Socket = enableTls(delegate.createSocket(host, port, localHost, localPort))
    override fun createSocket(host: InetAddress, port: Int): Socket = enableTls(delegate.createSocket(host, port))
    override fun createSocket(address: InetAddress, port: Int, localAddress: InetAddress, localPort: Int): Socket = enableTls(delegate.createSocket(address, port, localAddress, localPort))
    private fun enableTls(socket: Socket): Socket {
        if (socket is SSLSocket) {
            try {
                socket.enabledProtocols = ENABLED_PROTOCOLS
            } catch (_: IllegalArgumentException) {
                // Keep the platform defaults when an older provider rejects the protocol list.
            }
        }
        return socket
    }

    companion object {
        private val ENABLED_PROTOCOLS = arrayOf("TLSv1.2", "TLSv1.1", "TLSv1")
        @JvmStatic fun create(context: Context): TlsSocketFactory? = try {
            val sslContext = SSLContext.getInstance("TLS"); sslContext.init(null, arrayOf(buildTrustManager(context)), null); TlsSocketFactory(sslContext.socketFactory)
        } catch (_: Exception) { null }
        private fun buildTrustManager(context: Context): X509TrustManager {
            val certificateFactory = CertificateFactory.getInstance("X.509"); val store = KeyStore.getInstance(KeyStore.getDefaultType()); store.load(null, null)
            systemTrustAnchors().forEachIndexed { index, certificate -> store.setCertificateEntry("system-$index", certificate) }
            context.resources.openRawResource(R.raw.isrg_root_x1).use { store.setCertificateEntry("isrg-root-x1", certificateFactory.generateCertificate(it)) }
            val factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()); factory.init(store)
            return factory.trustManagers.filterIsInstance<X509TrustManager>().firstOrNull() ?: error("No X509TrustManager available")
        }
        private fun systemTrustAnchors(): List<X509Certificate> = try { val store = KeyStore.getInstance("AndroidCAStore"); store.load(null, null); val output = ArrayList<X509Certificate>(); val aliases = store.aliases(); while (aliases.hasMoreElements()) (store.getCertificate(aliases.nextElement()) as? X509Certificate)?.let(output::add); output } catch (_: Exception) { emptyList() }
    }
}
