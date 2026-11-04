package cl.uct.ojociudadano.data.mapper

import cl.uct.ojociudadano.data.model.CandidateUser
import cl.uct.ojociudadano.data.model.IncidentAssignment
import cl.uct.ojociudadano.data.model.IncidentStateHistory
import cl.uct.ojociudadano.data.model.IncidentStatus
import cl.uct.ojociudadano.data.model.Paginated
import cl.uct.ojociudadano.data.remote.dto.AsignacionDto
import cl.uct.ojociudadano.data.remote.dto.HistorialEstadoDto
import cl.uct.ojociudadano.data.remote.dto.PaginatedResponseDto
import cl.uct.ojociudadano.data.remote.dto.UsuarioDto

fun HistorialEstadoDto.toDomain(): IncidentStateHistory {
    return IncidentStateHistory(
        id = id,
        incidenteId = incidenteId,
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
        incidenteId = incidenteId,
        assignedUserId = usuarioAsignadoId,
        assignedByUserId = asignadoPorId,
        assignmentDate = fechaAsignacion,
        isActive = activa
    )
}

fun UsuarioDto.toCandidate(): CandidateUser {
    return CandidateUser(
        id = id,
        name = nombre,
        email = email,
        roles = roles
    )
}

fun PaginatedResponseDto<UsuarioDto>.toCandidatePage(): Paginated<CandidateUser> {
    return Paginated(
        items = data.map { it.toCandidate() },
        page = meta.page,
        limit = meta.limit,
        total = meta.total,
        totalPages = meta.totalPages
    )
}
