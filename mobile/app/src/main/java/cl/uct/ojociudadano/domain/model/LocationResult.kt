package cl.uct.ojociudadano.domain.model

sealed class LocationResult {
    data class Success(val latitude: Double, val longitude: Double) : LocationResult()
    data class Error(val message: String) : LocationResult()
    object NotAvailable : LocationResult()
    object MissingPermissions : LocationResult()
}