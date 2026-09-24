package cl.uct.ojociudadano.data.mapper

import cl.uct.ojociudadano.data.model.Category
import cl.uct.ojociudadano.data.remote.dto.CategoryDto

fun CategoryDto.toDomain(): Category {
    return Category(
        id = id,
        name = nombre,
        description = descripcion,
        basePriority = prioridadBase,
        isCriticalEmergency = esEmergenciaCritica,
        officialContactChannel = canalOficialContacto
    )
}
