package cl.uct.ojociudadano.data.mapper

import cl.uct.ojociudadano.data.model.UserProfile
import cl.uct.ojociudadano.data.remote.dto.UsuarioDto

/**
 * El contrato de `/usuarios/me` es idéntico al de `UsuarioDto`, así que no se
 * crea un DTO paralelo: se reutiliza el existente y se separa el modelo de
 * dominio.
 */
fun UsuarioDto.toProfile(): UserProfile = UserProfile(
    id = id,
    name = nombre,
    email = email,
    phone = telefono,
    registeredAt = fechaRegistro,
    roles = roles
)
