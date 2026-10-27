package cl.uct.ojociudadano.domain.model

data class Evidence(
    val id: String,
    val incidentId: String,
    val mimeType: String,
    val size: Long,
    val createdAt: String,
    val fileUrl: String
)

fun cl.uct.ojociudadano.data.remote.dto.EvidenceDto.toDomain() = Evidence(
    id = id,
    incidentId = incidenteId,
    mimeType = tipoMime,
    size = tamanoArchivo,
    createdAt = fechaCreacion,
    fileUrl = url
)
