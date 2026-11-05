package cl.uct.ojociudadano.data.remote

import okhttp3.Interceptor
import okhttp3.RequestBody
import okhttp3.Response
import okio.BufferedSink

/** A confirmed or uncertain status write must never be replayed by the HTTP client. */
internal class StatusPatchInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val body = request.body
        if (request.method != "PATCH" || body == null ||
            !request.url.encodedPath.matches(Regex("/api/v1/incidentes/[^/]+/estado"))
        ) return chain.proceed(request)

        val singleUseBody = object : RequestBody() {
            override fun contentType() = body.contentType()
            override fun contentLength() = body.contentLength()
            override fun writeTo(sink: BufferedSink) = body.writeTo(sink)
            override fun isOneShot() = true
        }
        return chain.proceed(request.newBuilder().method(request.method, singleUseBody).build())
    }
}
