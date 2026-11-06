package cl.uct.ojociudadano.data.remote

import okhttp3.OkHttpClient

/** Producción conserva íntegramente la validación TLS de OkHttp y Android. */
@Suppress("UNUSED_PARAMETER")
internal fun OkHttpClient.Builder.configureEnvironmentTls(baseUrl: String): OkHttpClient.Builder = this
