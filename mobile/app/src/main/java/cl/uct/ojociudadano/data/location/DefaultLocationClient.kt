package cl.uct.ojociudadano.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import cl.uct.ojociudadano.domain.model.LocationResult
import cl.uct.ojociudadano.domain.repository.LocationClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult as GmsLocationResult
import com.google.android.gms.location.Priority
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

class DefaultLocationClient(
    private val context: Context,
    private val client: FusedLocationProviderClient
) : LocationClient {

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): LocationResult {
        // 1. Verificar permisos (asumiendo que la Issue 81 ya gestionó ACCESS_FINE_LOCATION / ACCESS_COARSE_LOCATION)
        val hasFinePermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCoarsePermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFinePermission && !hasCoarsePermission) {
            return LocationResult.MissingPermissions
        }

        // 2. Verificar si el GPS o proveedor de red está habilitado
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!isGpsEnabled && !isNetworkEnabled) {
            return LocationResult.NotAvailable
        }

        // 3. Obtener ubicación de forma segura y con un límite de espera para
        // que el formulario no quede bloqueado si el dispositivo nunca entrega
        // una lectura.
        return withTimeoutOrNull(LOCATION_TIMEOUT_MILLIS) {
            suspendCancellableCoroutine { continuation ->
                client.lastLocation.addOnSuccessListener { location ->
                    if (!continuation.isActive) return@addOnSuccessListener

                    if (location != null) {
                        continuation.resume(
                            LocationResult.Success(
                                latitude = location.latitude,
                                longitude = location.longitude
                            )
                        )
                    } else {
                        // Si la última ubicación es nula, solicitamos una actualización única
                        val request = LocationRequest.Builder(
                            Priority.PRIORITY_HIGH_ACCURACY,
                            LOCATION_UPDATE_INTERVAL_MILLIS
                        )
                            .setWaitForAccurateLocation(false)
                            .setMaxUpdates(1)
                            .build()

                        val callback = object : LocationCallback() {
                            override fun onLocationResult(result: GmsLocationResult) {
                                client.removeLocationUpdates(this)
                                if (!continuation.isActive) return

                                val loc = result.lastLocation
                                if (loc != null) {
                                    continuation.resume(
                                        LocationResult.Success(loc.latitude, loc.longitude)
                                    )
                                } else {
                                    continuation.resume(LocationResult.NotAvailable)
                                }
                            }
                        }

                        continuation.invokeOnCancellation {
                            client.removeLocationUpdates(callback)
                        }

                        try {
                            client.requestLocationUpdates(
                                request,
                                callback,
                                Looper.getMainLooper()
                            )
                        } catch (e: Exception) {
                            if (continuation.isActive) {
                                continuation.resume(
                                    LocationResult.Error(
                                        e.localizedMessage ?: "Error desconocido"
                                    )
                                )
                            }
                        }
                    }
                }.addOnFailureListener { exception ->
                    if (continuation.isActive) {
                        continuation.resume(
                            LocationResult.Error(
                                exception.localizedMessage ?: "Error al obtener ubicación"
                            )
                        )
                    }
                }
            }
        } ?: LocationResult.Error(LOCATION_TIMEOUT_MESSAGE)
    }

    private companion object {
        const val LOCATION_TIMEOUT_MILLIS = 15_000L
        const val LOCATION_UPDATE_INTERVAL_MILLIS = 10_000L
        const val LOCATION_TIMEOUT_MESSAGE =
            "No fue posible obtener una ubicación GPS a tiempo"
    }
}