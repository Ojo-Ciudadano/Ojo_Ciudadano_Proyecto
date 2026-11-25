package cl.uct.ojociudadano.data.location

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import cl.uct.ojociudadano.domain.model.ReverseGeocodedAddress
import cl.uct.ojociudadano.domain.model.ReverseGeocodingResult
import cl.uct.ojociudadano.domain.repository.ReverseGeocodingClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Implementación de geocodificación inversa basada en el [Geocoder] de Android.
 *
 * La consulta se ejecuta en [ioDispatcher] para no bloquear el hilo principal.
 * Cualquier fallo del proveedor se traduce a [ReverseGeocodingResult.Error] y
 * la falta de resultados a [ReverseGeocodingResult.NoResults], de modo que el
 * formulario pueda seguir completándose manualmente.
 */
class AndroidGeocoderReverseGeocodingClient(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val locale: Locale = Locale.forLanguageTag("es-CL")
) : ReverseGeocodingClient {

    private val applicationContext = context.applicationContext

    override suspend fun reverseGeocode(
        latitude: Double,
        longitude: Double
    ): ReverseGeocodingResult = withContext(ioDispatcher) {
        if (!Geocoder.isPresent()) {
            return@withContext ReverseGeocodingResult.Error(GEOCODER_UNAVAILABLE)
        }

        try {
            val addresses = withTimeoutOrNull(GEOCODING_TIMEOUT_MILLIS) {
                lookup(latitude, longitude)
            } ?: return@withContext ReverseGeocodingResult.Error(GEOCODING_TIMEOUT)

            val address = addresses.firstOrNull()
                ?: return@withContext ReverseGeocodingResult.NoResults

            val reverseGeocodedAddress = reverseGeocodedAddressFrom(address)

            if (reverseGeocodedAddress.hasAnyValue) {
                ReverseGeocodingResult.Success(reverseGeocodedAddress)
            } else {
                ReverseGeocodingResult.NoResults
            }
        } catch (exception: IOException) {
            ReverseGeocodingResult.Error(
                exception.message ?: GEOCODING_SERVICE_UNAVAILABLE
            )
        } catch (exception: Exception) {
            ReverseGeocodingResult.Error(
                exception.message ?: GEOCODING_SERVICE_UNAVAILABLE
            )
        }
    }

    private suspend fun lookup(
        latitude: Double,
        longitude: Double
    ): List<Address> {
        val geocoder = Geocoder(applicationContext, locale)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { continuation ->
                geocoder.getFromLocation(latitude, longitude, MAX_RESULTS) { addresses ->
                    if (continuation.isActive) {
                        continuation.resume(addresses?.toList().orEmpty())
                    }
                }
            }
        } else {
            @Suppress("DEPRECATION")
            geocoder.getFromLocation(latitude, longitude, MAX_RESULTS).orEmpty()
        }
    }

    private fun reverseGeocodedAddressFrom(address: Address): ReverseGeocodedAddress =
        reverseGeocodedAddressFrom(
            thoroughfare = address.thoroughfare,
            subThoroughfare = address.subThoroughfare,
            featureName = address.featureName,
            addressLine = address.getAddressLine(0),
            locality = address.locality,
            subAdminArea = address.subAdminArea,
            adminArea = address.adminArea
        )

    private companion object {
        const val MAX_RESULTS = 1
        const val GEOCODING_TIMEOUT_MILLIS = 8_000L
        const val GEOCODER_UNAVAILABLE =
            "La geocodificación no está disponible en este dispositivo."
        const val GEOCODING_TIMEOUT =
            "La geocodificación no respondió a tiempo."
        const val GEOCODING_SERVICE_UNAVAILABLE =
            "No fue posible obtener la dirección de esta ubicación."
    }
}

/**
 * Traduce los campos de una dirección de Android al modelo del dominio.
 *
 * Se separa del [Address] para poder verificar la prioridad de los campos y la
 * limpieza de valores vacíos sin depender del framework de Android.
 */
internal fun reverseGeocodedAddressFrom(
    thoroughfare: String?,
    subThoroughfare: String?,
    featureName: String?,
    addressLine: String?,
    locality: String?,
    subAdminArea: String?,
    adminArea: String?
): ReverseGeocodedAddress {
    val street = listOfNotNull(
        thoroughfare.cleaned(),
        subThoroughfare.cleaned()
    ).joinToString(" ")

    val referenceAddress = street.ifBlank {
        featureName.cleaned() ?: addressLine.cleaned().orEmpty()
    }

    return ReverseGeocodedAddress(
        referenceAddress = referenceAddress.cleaned(),
        commune = locality.cleaned() ?: subAdminArea.cleaned(),
        region = adminArea.cleaned()
    )
}

private fun String?.cleaned(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
