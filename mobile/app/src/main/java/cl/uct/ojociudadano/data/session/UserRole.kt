package cl.uct.ojociudadano.data.session

/** Values supported by the backend role contract. UI labels must not be used for parsing. */
enum class UserRole(val backendValue: String) {
    CIUDADANO("CIUDADANO"),
    ADMIN("ADMIN");

    companion object {
        fun fromBackend(value: String): UserRole? = entries.firstOrNull {
            it.backendValue == value
        }
    }
}

/** Presentation state only. The backend remains responsible for authorization. */
sealed interface SessionRoles {
    data class Known(val values: Set<UserRole>) : SessionRoles

    /** Preserve the full response, including mixed known and unknown values. */
    data class Unknown(val backendValues: List<String>) : SessionRoles

    data object Missing : SessionRoles

    companion object {
        fun fromBackend(values: List<String>?): SessionRoles {
            if (values.isNullOrEmpty()) return Missing
            val parsed = values.map { UserRole.fromBackend(it) }
            if (parsed.any { it == null }) return Unknown(values.toList())
            return Known(parsed.filterNotNull().toSet())
        }
    }
}
