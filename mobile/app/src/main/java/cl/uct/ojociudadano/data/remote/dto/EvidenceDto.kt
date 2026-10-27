package cl.uct.ojociudadano.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class EvidenceDto(
    val id: String,
    val incidenteId: String,
    val tipoMime: String,
    val tamanoArchivo: Long,
    val fechaCreacion: String,
    val url: String
)
