package cl.uct.ojociudadano.data.remote

import java.io.File
import java.lang.reflect.Proxy
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import javax.net.ssl.SSLPeerUnverifiedException
import javax.net.ssl.SSLSession
import okhttp3.OkHttpClient
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class EnvironmentTlsTest {
    private val host = "api-ojociudadano.dev.censei.cl"

    @Test
    fun `debug allows only the observed certificates for the exact API hostname`() {
        val verifier = client().hostnameVerifier
        for (name in listOf("debug_ingress_a", "debug_ingress_b")) {
            val session = session(certificate("src/debug/res/raw/$name.pem"))
            assertTrue(verifier.verify(host, session))
            assertFalse(verifier.verify("otro.example.invalid", session))
            assertFalse(verifier.verify("sub.$host", session))
        }
    }

    @Test
    fun `debug rejects a different certificate even for the API hostname`() {
        val unrelated = certificate("src/main/res/raw/uct_ingress_cert.pem")
        assertFalse(client().hostnameVerifier.verify(host, session(unrelated)))
    }

    @Test
    fun `debug rejects missing or unavailable peer certificates`() {
        assertFalse(client().hostnameVerifier.verify(host, session()))
        val unavailable = Proxy.newProxyInstance(
            SSLSession::class.java.classLoader, arrayOf(SSLSession::class.java)
        ) { _, _, _ -> throw SSLPeerUnverifiedException("sin certificado") } as SSLSession
        assertFalse(client().hostnameVerifier.verify(host, unavailable))
    }

    @Test
    fun `custom environments keep the original hostname verifier`() {
        val client = OkHttpClient.Builder()
            .configureEnvironmentTls("https://otro.example.invalid/")
            .build()
        assertSame(OkHttpClient().hostnameVerifier, client.hostnameVerifier)
    }

    @Test
    fun `normal hostname checks still work`() {
        // Los certificados de desarrollo tienen SAN ingress.local; se conserva la comprobación normal.
        val certificate = certificate("src/debug/res/raw/debug_ingress_a.pem")
        assertTrue(client().hostnameVerifier.verify("ingress.local", session(certificate)))
    }

    private fun client() = OkHttpClient.Builder().configureEnvironmentTls("https://$host/").build()

    private fun certificate(path: String): Certificate = File(path).inputStream().use {
        CertificateFactory.getInstance("X.509").generateCertificate(it)
    }

    private fun session(vararg certificates: Certificate): SSLSession = Proxy.newProxyInstance(
        SSLSession::class.java.classLoader, arrayOf(SSLSession::class.java)
    ) { _, method, _ ->
        when (method.name) {
            "getPeerCertificates" -> certificates
            else -> throw UnsupportedOperationException(method.name)
        }
    } as SSLSession
}
