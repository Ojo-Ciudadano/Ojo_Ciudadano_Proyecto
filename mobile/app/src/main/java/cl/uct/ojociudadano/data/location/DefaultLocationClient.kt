package cl.uct.ojociudadano.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import cl.uct.ojociudadano.domain.model.LocationResult
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult as GmsLocationResult
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

interface LocationClient {
    suspend fun getCurrentLocation(): LocationResult
}

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

        // 3. Obtener ubicación de forma segura
        return suspendCancellableCoroutine { continuation ->
            client.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    continuation.resume(
                        LocationResult.Success(
                            latitude = location.latitude,
                            longitude = location.longitude
                        )
                    )
                } else {
                    // Si la última ubicación es nula, solicitamos una actualización única
                    val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000L)
                        .setWaitForAccurateLocation(false)
                        .setMaxUpdates(1)
                        .build()

                    val callback = object : LocationCallback() {
                        override fun onLocationResult(result: GmsLocationResult) {
                            client.removeLocationUpdates(this)
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

                    try {
                        client.requestLocationUpdates(request, callback, Looper.getMainLooper())
                    } catch (e: Exception) {
                        continuation.resume(LocationResult.Error(e.localizedMessage ?: "Error desconocido"))
                    }
                }
            }.addOnFailureListener { exception ->
                continuation.resume(LocationResult.Error(exception.localizedMessage ?: "Error al obtener ubicación"))
            }
        }
    }
}