package cl.uct.ojociudadano.navigation

import cl.uct.ojociudadano.data.session.SessionState

/** Client UI permissions only; endpoint authorization remains the backend's responsibility. */
object RoleAccessPolicy {
    // Reuse #98's fail-closed session resolution, including invalid and ambiguous roles.
    // To share a future screen, assign both CITIZEN and ADMINISTRATOR here.
    private val audiences = mapOf(
        AppRoutes.LOGIN to setOf(InitialDestination.AUTHENTICATION),
        AppRoutes.REGISTER to setOf(InitialDestination.AUTHENTICATION),
        AppRoutes.ADMIN to setOf(InitialDestination.ADMINISTRATOR),
        AppRoutes.ADMIN_INCIDENTS to setOf(InitialDestination.ADMINISTRATOR),
        AppRoutes.ROLE_UNAVAILABLE to setOf(InitialDestination.UNAVAILABLE),
        AppRoutes.HOME to setOf(InitialDestination.CITIZEN),
        AppRoutes.REPORTS to setOf(InitialDestination.CITIZEN),
        AppRoutes.INCIDENT_DETAIL to setOf(InitialDestination.CITIZEN),
        AppRoutes.CREATE_REPORT to setOf(InitialDestination.CITIZEN),
        AppRoutes.NOTIFICATIONS to setOf(InitialDestination.CITIZEN),
        AppRoutes.MAP to setOf(InitialDestination.CITIZEN),
        AppRoutes.PROFILE to setOf(InitialDestination.CITIZEN)
    )

    /** Accept registered patterns and concrete incident routes; unclassified routes are denied. */
    fun allows(state: SessionState, route: String?): Boolean {
        val destination = when {
            route == AppRoutes.INCIDENT_DETAIL -> route
            route != null && incidentRoute.matches(route) -> AppRoutes.INCIDENT_DETAIL
            else -> route
        }
        return resolveInitialDestination(state) in audiences[destination].orEmpty()
    }

    fun safeRoute(state: SessionState): String? = resolveInitialDestination(state).route

    internal val registeredRoutes: Set<String> get() = audiences.keys

    private val incidentRoute = Regex("incident/[^/?#]+")
}
