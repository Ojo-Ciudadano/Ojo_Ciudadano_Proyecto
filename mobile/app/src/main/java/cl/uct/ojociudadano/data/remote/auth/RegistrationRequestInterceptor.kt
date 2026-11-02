package cl.uct.ojociudadano.data.remote.auth

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.BufferedSink

internal fun Request.isRegistrationRequest() =
    method == "POST" && url.encodedPath == "/api/v1/auth/register"

/** Una respuesta perdida no debe provocar el reenvío automático de un registro. */
class RegistrationRequestInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val body = request.body
        if (!request.isRegistrationRequest() || body == null) return chain.proceed(request)
        val oneShotBody = object : RequestBody() {
            override fun contentType() = body.contentType()
            override fun contentLength() = body.contentLength()
            override fun isOneShot() = true
            override fun writeTo(sink: BufferedSink) = body.writeTo(sink)
        }
        return chain.proceed(request.newBuilder().method(request.method, oneShotBody).build())
    }
}
