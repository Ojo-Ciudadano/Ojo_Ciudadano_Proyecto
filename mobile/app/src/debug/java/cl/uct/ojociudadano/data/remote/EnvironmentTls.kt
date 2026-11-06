package cl.uct.ojociudadano.data.remote

import java.security.MessageDigest
import java.security.cert.X509Certificate
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient

private const val DEVELOPMENT_API_HOST = "api-ojociudadano.dev.censei.cl"

// Huellas SHA-256 de los dos certificados públicos observados en el ingreso de desarrollo.
// Si el servidor los reemplaza, la excepción falla hasta revisar los certificados nuevos.
private val DEVELOPMENT_CERTIFICATES = setOf(
    "bbc1e730ad1937be888500b3705abcb7560990ba9a590041ff5359c4f69480aa",
    "d68cbdf6646dc6e49c17d01d0282a6d214c6acd9d2845278ef89701a0200ec7e"
)

/** Solo existe en debug: conserva la validación normal y acota la excepción al certificado exacto. */
internal fun OkHttpClient.Builder.configureEnvironmentTls(baseUrl: String): OkHttpClient.Builder {
    if (baseUrl.toHttpUrl().host != DEVELOPMENT_API_HOST) return this

    val normalVerifier = build().hostnameVerifier
    return hostnameVerifier { hostname, session ->
        runCatching { normalVerifier.verify(hostname, session) }.getOrDefault(false) ||
            (hostname == DEVELOPMENT_API_HOST && runCatching {
                val leaf = session.peerCertificates.firstOrNull() as? X509Certificate
                    ?: return@runCatching false
                leaf.checkValidity()
                val fingerprint = MessageDigest.getInstance("SHA-256")
                    .digest(leaf.encoded)
                    .joinToString("") { "%02x".format(it.toInt() and 0xff) }
                fingerprint in DEVELOPMENT_CERTIFICATES
            }.getOrDefault(false))
    }
}
