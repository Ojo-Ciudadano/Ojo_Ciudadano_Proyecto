package cl.uct.ojociudadano.data.mapper

import cl.uct.ojociudadano.data.remote.dto.EvidenceDto
import cl.uct.ojociudadano.domain.model.Evidence

fun EvidenceDto.toDomain() = Evidence(
    id = id,
    incidentId = incidenteId,
    mimeType = tipoMime,
    size = tamanoArchivo,
    createdAt = fechaCreacion,
    fileUrl = url
)
