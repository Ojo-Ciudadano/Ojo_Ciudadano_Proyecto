package cl.uct.ojociudadano.data.remote.dto

data class IncidentLocationDto(
    val id: String,
    val latitud: Double,
    val longitud: Double,
    val direccionReferencia: String,
    val comuna: String,
    val region: String,
    val sector: String
)
