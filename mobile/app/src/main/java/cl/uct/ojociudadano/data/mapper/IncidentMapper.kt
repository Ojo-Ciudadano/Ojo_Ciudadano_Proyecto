package cl.uct.ojociudadano.data.mapper

import cl.uct.ojociudadano.data.model.Incident
import cl.uct.ojociudadano.data.model.IncidentLocation
import cl.uct.ojociudadano.data.model.IncidentStatus
import cl.uct.ojociudadano.data.remote.dto.IncidentDto

fun IncidentDto.toDomain(): Incident {
    return Incident(
        id = id,
        title = titulo,
        description = descripcion,
        category = categoria.toDomain(),
        status = IncidentStatus(
            id = estado.id,
            name = estado.nombre,
            order = estado.orden
        ),
        location = IncidentLocation(
            id = ubicacion.id,
            latitude = ubicacion.latitud,
            longitude = ubicacion.longitud,
            referenceAddress = ubicacion.direccionReferencia,
            commune = ubicacion.comuna,
            region = ubicacion.region,
            sector = ubicacion.sector
        ),
        reporterUserId = usuarioReportaId,
        priorityLevel = nivelPrioridad,
        createdAt = fechaCreacion,
        updatedAt = fechaActualizacion
    )
}
