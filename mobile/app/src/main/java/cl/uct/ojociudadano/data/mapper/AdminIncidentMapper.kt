package cl.uct.ojociudadano.data.mapper

import cl.uct.ojociudadano.data.model.IncidentAssignment
import cl.uct.ojociudadano.data.model.IncidentStateHistory
import cl.uct.ojociudadano.data.model.IncidentStatus
import cl.uct.ojociudadano.data.remote.dto.AsignacionDto
import cl.uct.ojociudadano.data.remote.dto.HistorialEstadoDto

fun HistorialEstadoDto.toDomain(): IncidentStateHistory {
    return IncidentStateHistory(
        id = id,
        status = IncidentStatus(
            id = estado.id,
            name = estado.nombre,
            order = estado.orden
        ),
        userId = usuarioId,
        observation = observacion,
        date = fecha
    )
}

fun AsignacionDto.toDomain(): IncidentAssignment {
    return IncidentAssignment(
        id = id,
        assignedUserId = usuarioAsignadoId,
        assignedByUserId = asignadoPorId,
        assignmentDate = fechaAsignacion,
        isActive = activa
    )
}
