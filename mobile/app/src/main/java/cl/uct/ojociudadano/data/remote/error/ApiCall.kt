package cl.uct.ojociudadano.data.remote.error

import cl.uct.ojociudadano.data.session.SessionRequiredException
import com.google.gson.JsonParser
import java.io.IOException
import java.util.concurrent.CancellationException
import retrofit2.HttpException

suspend fun <T> executeApiCall(
    block: suspend () -> T
): T {
    return try {
        block()
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: HttpException) {
        throw exception.toApiException()
    } catch (exception: SessionRequiredException) {
        throw exception
    } catch (exception: IOException) {
        throw ApiException.Network(exception)
    } catch (exception: Exception) {
        throw ApiException.Unexpected(exception)
    }
}

private fun HttpException.toApiException(): ApiException.Http {
    val parsedError = response()
        ?.errorBody()
        ?.string()
        ?.let(::parseErrorBody)

    return ApiException.Http(
        statusCode = code(),
        serverMessages = parsedError?.messages.orEmpty(),
        error = parsedError?.error,
        path = parsedError?.path
    )
}

private data class ParsedApiError(
    val messages: List<String>,
    val error: String?,
    val path: String?
)

private fun parseErrorBody(body: String): ParsedApiError? {
    return runCatching {
        val json = JsonParser.parseString(body).asJsonObject
        val message = json.get("message")

        val messages = when {
            message == null || message.isJsonNull -> {
                emptyList()
            }

            message.isJsonArray -> {
                message.asJsonArray
                    .filter { it.isJsonPrimitive }
                    .map { it.asString }
            }

            message.isJsonPrimitive -> {
                listOf(message.asString)
            }

            else -> {
                emptyList()
            }
        }

        ParsedApiError(
            messages = messages,
            error = json.get("error")
                ?.takeIf { it.isJsonPrimitive }
                ?.asString,
            path = json.get("path")
                ?.takeIf { it.isJsonPrimitive }
                ?.asString
        )
    }.getOrNull()
}
