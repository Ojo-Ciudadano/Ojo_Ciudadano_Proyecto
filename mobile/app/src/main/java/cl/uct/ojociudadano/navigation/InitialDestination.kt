package cl.uct.ojociudadano.navigation

import cl.uct.ojociudadano.data.session.SessionRoles
import cl.uct.ojociudadano.data.session.SessionState
import cl.uct.ojociudadano.data.session.UserRole

/** One decision for interactive login and persisted session restoration. */
enum class InitialDestination(val route: String?) {
    LOADING(null),
    AUTHENTICATION(AppRoutes.LOGIN),
    CITIZEN(AppRoutes.HOME),
    ADMINISTRATOR(AppRoutes.ADMIN),
    UNAVAILABLE(AppRoutes.ROLE_UNAVAILABLE)
}

fun resolveInitialDestination(state: SessionState): InitialDestination = when (state) {
    SessionState.Restoring -> InitialDestination.LOADING
    SessionState.Unauthenticated -> InitialDestination.AUTHENTICATION
    is SessionState.Authenticated -> {
        val roles = state.session.roleState
        when {
            !state.session.isLocallyValid() -> InitialDestination.UNAVAILABLE
            roles !is SessionRoles.Known -> InitialDestination.UNAVAILABLE
            // The project defines no priority for multiple roles. Do not select one implicitly.
            roles.values.size != 1 -> InitialDestination.UNAVAILABLE
            roles.values.single() == UserRole.CIUDADANO -> InitialDestination.CITIZEN
            roles.values.single() == UserRole.ADMIN -> InitialDestination.ADMINISTRATOR
            else -> InitialDestination.UNAVAILABLE
        }
    }
}
