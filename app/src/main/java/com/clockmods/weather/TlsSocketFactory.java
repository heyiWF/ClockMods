package com.clockmods.weather;

import android.content.Context;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import com.clockmods.R;

/**
 * Wraps a platform {@link SSLSocketFactory} to make HTTPS work on old Android releases.
 *
 * <p>Two problems are addressed:</p>
 * <ol>
 *     <li>Android 4.x (API 16-19) ships with TLS 1.1/1.2 support but leaves the protocols
 *     disabled by default, so servers that require TLS 1.2 fail with a handshake error.
 *     Every socket produced here has the modern protocols explicitly enabled.</li>
 *     <li>Old system CA stores predate the ISRG Root X1 root certificate used by
 *     Let's Encrypt, which signs the QWeather certificate chain. That leads to a
 *     {@code CertPathValidatorException: Trust anchor for certification path not found}.
 *     The bundled ISRG Root X1 certificate is added to the trust anchors so the chain
 *     validates while still honouring the platform's own trusted roots.</li>
 * </ol>
 */
final class TlsSocketFactory extends SSLSocketFactory {
    private static final String[] ENABLED_PROTOCOLS = {"TLSv1.2", "TLSv1.1", "TLSv1"};

    private final SSLSocketFactory delegate;

    private TlsSocketFactory(SSLSocketFactory delegate) {
        this.delegate = delegate;
    }

    /**
     * Builds a socket factory that trusts the system CAs plus the bundled ISRG Root X1
     * certificate and forces modern TLS protocols. Returns {@code null} if the secure
     * context cannot be built, in which case the caller should keep the platform default.
     */
    static TlsSocketFactory create(Context context) {
        try {
            X509TrustManager trustManager = buildTrustManager(context);
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{trustManager}, null);
            return new TlsSocketFactory(sslContext.getSocketFactory());
        } catch (Exception unableToBuild) {
            return null;
        }
    }

    private static X509TrustManager buildTrustManager(Context context) throws Exception {
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");

        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);

        int index = 0;
        for (X509Certificate certificate : systemTrustAnchors()) {
            trustStore.setCertificateEntry("system-" + index++, certificate);
        }

        InputStream bundled = context.getResources().openRawResource(R.raw.isrg_root_x1);
        try {
            Certificate isrgRootX1 = certificateFactory.generateCertificate(bundled);
            trustStore.setCertificateEntry("isrg-root-x1", isrgRootX1);
        } finally {
            bundled.close();
        }

        TrustManagerFactory factory =
                TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        factory.init(trustStore);
        for (TrustManager manager : factory.getTrustManagers()) {
            if (manager instanceof X509TrustManager) {
                return (X509TrustManager) manager;
            }
        }
        throw new IllegalStateException("No X509TrustManager available");
    }

    private static List<X509Certificate> systemTrustAnchors() {
        List<X509Certificate> anchors = new ArrayList<>();
        try {
            KeyStore systemKeyStore = KeyStore.getInstance("AndroidCAStore");
            systemKeyStore.load(null, null);
            Enumeration<String> aliases = systemKeyStore.aliases();
            while (aliases.hasMoreElements()) {
                Certificate certificate = systemKeyStore.getCertificate(aliases.nextElement());
                if (certificate instanceof X509Certificate) {
                    anchors.add((X509Certificate) certificate);
                }
            }
        } catch (Exception ignored) {
            // Fall back to just the bundled root if the system store is unavailable.
        }
        return anchors;
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return delegate.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return delegate.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
        return enableTls(delegate.createSocket(s, host, port, autoClose));
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        return enableTls(delegate.createSocket(host, port));
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
        return enableTls(delegate.createSocket(host, port, localHost, localPort));
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return enableTls(delegate.createSocket(host, port));
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort)
            throws IOException {
        return enableTls(delegate.createSocket(address, port, localAddress, localPort));
    }

    private static Socket enableTls(Socket socket) {
        if (socket instanceof SSLSocket) {
            try {
                ((SSLSocket) socket).setEnabledProtocols(ENABLED_PROTOCOLS);
            } catch (IllegalArgumentException ignored) {
                // Fall back to whatever the platform enables if it rejects the list.
            }
        }
        return socket;
    }
}
