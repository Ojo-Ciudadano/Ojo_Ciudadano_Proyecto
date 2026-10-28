package cl.uct.ojociudadano.data.mapper

import cl.uct.ojociudadano.data.model.CreateIncidentInput
import cl.uct.ojociudadano.data.remote.dto.CreateIncidentRequestDto
import cl.uct.ojociudadano.data.remote.dto.LocationRequestDto

fun CreateIncidentInput.toRequestDto(reporterUserId: String): CreateIncidentRequestDto {
    return CreateIncidentRequestDto(
        titulo = title,
        descripcion = description,
        categoriaId = categoryId,
        usuarioReportaId = reporterUserId,
        ubicacion = LocationRequestDto(
            latitud = location.latitude,
            longitud = location.longitude,
            direccionReferencia = location.referenceAddress,
            comuna = location.commune,
            region = location.region,
            sector = location.sector
        )
    )
}
