package cl.uct.ojociudadano.data.remote.dto

data class UpdateUserRoleRequestDto(val rol: String)

data class UpdateUserRoleResponseDto(val id: String, val roles: List<String>?)
