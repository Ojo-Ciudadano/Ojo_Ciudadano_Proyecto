package cl.uct.ojociudadano.data.remote.dto

data class LocationRequestDto(
    val latitud: Double,
    val longitud: Double,
    val direccionReferencia: String,
    val comuna: String,
    val region: String,
    val sector: String
)
