package cl.uct.ojociudadano.ui.admin

import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cl.uct.ojociudadano.data.model.Paginated
import cl.uct.ojociudadano.data.model.UserProfile
import cl.uct.ojociudadano.data.remote.error.ApiException
import cl.uct.ojociudadano.data.repository.UserRepository
import cl.uct.ojociudadano.data.session.SessionRequiredException
import cl.uct.ojociudadano.data.session.UserRole
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

enum class UserRoleFilter(val label: String, val role: UserRole?) {
    ALL("Todos", null), CITIZEN("Ciudadano", UserRole.CIUDADANO), ADMIN("Administrador", UserRole.ADMIN)
}

sealed interface UsersLoadState {
    data object Loading : UsersLoadState
    data object Ready : UsersLoadState
    data class Error(val message: String) : UsersLoadState
}

data class RoleChangeConfirmation(val userId: String, val userName: String, val newRole: UserRole)

data class AdminUsersState(
    val filter: UserRoleFilter = UserRoleFilter.ALL,
    val page: Int = 1,
    val result: Paginated<UserProfile>? = null,
    val status: UsersLoadState = UsersLoadState.Loading,
    val roleChange: RoleChangeConfirmation? = null,
    val changingRoleIds: Set<String> = emptySet(),
    val roleChangeError: String? = null
) {
    val canGoBack: Boolean get() = status == UsersLoadState.Ready && page > 1
    val canGoForward: Boolean get() = status == UsersLoadState.Ready && page < (result?.totalPages ?: 0)
}

internal fun UserProfile.alternativeRole(): UserRole? =
    when (roles.distinct().singleOrNull()?.let(UserRole::fromBackend)) {
        UserRole.ADMIN -> UserRole.CIUDADANO
        UserRole.CIUDADANO -> UserRole.ADMIN
        null -> null
    }

@MainThread
class AdminUsersViewModel(private val repository: UserRepository) : ViewModel() {
    private val _state = MutableStateFlow(AdminUsersState())
    val state = _state.asStateFlow()
    private var job: Job? = null
    private var requestId = 0L

    init { load(1, UserRoleFilter.ALL) }

    fun selectFilter(filter: UserRoleFilter) {
        if (filter != _state.value.filter) load(1, filter)
    }

    fun previousPage() {
        if (_state.value.canGoBack) load(_state.value.page - 1, _state.value.filter)
    }

    fun nextPage() {
        if (_state.value.canGoForward) load(_state.value.page + 1, _state.value.filter)
    }

    fun retry() {
        val current = _state.value
        if (current.status is UsersLoadState.Error) {
            load(current.page, current.filter, retainedResult = current.result)
        }
    }

    fun requestRoleChange(userId: String) {
        val current = _state.value
        if (current.roleChange != null || userId in current.changingRoleIds) return
        val user = current.result?.items?.find { it.id == userId } ?: return
        val newRole = user.alternativeRole() ?: return
        _state.value = current.copy(roleChange = RoleChangeConfirmation(user.id, user.name, newRole))
    }

    fun dismissRoleChange() {
        _state.value = _state.value.copy(roleChange = null)
    }

    fun confirmRoleChange() {
        val current = _state.value
        val change = current.roleChange ?: return
        if (change.userId in current.changingRoleIds) return
        _state.value = current.copy(
            roleChange = null,
            changingRoleIds = current.changingRoleIds + change.userId,
            roleChangeError = null
        )
        viewModelScope.launch {
            try {
                val roles = repository.updateUserRole(change.userId, change.newRole)
                ensureActive()
                applyConfirmedRoles(change.userId, roles)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                ensureActive()
                _state.value = _state.value.copy(roleChangeError = exception.roleChangeMessage())
            } finally {
                val latest = _state.value
                _state.value = latest.copy(changingRoleIds = latest.changingRoleIds - change.userId)
            }
        }
    }

    private fun applyConfirmedRoles(userId: String, roles: List<String>) {
        val current = _state.value
        val filterRole = current.filter.role?.backendValue
        val updatedResult = current.result?.let { result ->
            result.copy(items = result.items.mapNotNull { user ->
                when {
                    user.id != userId -> user
                    filterRole != null && filterRole !in roles -> null
                    else -> user.copy(roles = roles)
                }
            })
        }
        // An active filter can change page membership. An in-flight read can predate the PATCH.
        if (filterRole != null || current.status != UsersLoadState.Ready) {
            load(current.page, current.filter, retainedResult = updatedResult)
        } else {
            _state.value = current.copy(result = updatedResult)
        }
    }

    private fun load(page: Int, filter: UserRoleFilter, retainedResult: Paginated<UserProfile>? = null) {
        val currentRequest = ++requestId
        job?.cancel()
        val current = _state.value
        _state.value = current.copy(
            filter = filter,
            page = page,
            result = retainedResult,
            status = UsersLoadState.Loading,
            roleChange = current.roleChange.takeIf { page == current.page && filter == current.filter }
        )
        job = viewModelScope.launch {
            var requestedPage = page
            try {
                var result = repository.getUsers(requestedPage, PAGE_SIZE, filter.role)
                while (result.items.isEmpty() && requestedPage > result.totalPages.coerceAtLeast(1)) {
                    ensureActive()
                    requestedPage = result.totalPages.coerceAtLeast(1)
                    result = repository.getUsers(requestedPage, PAGE_SIZE, filter.role)
                }
                ensureActive()
                if (currentRequest == requestId) {
                    _state.value = _state.value.copy(page = result.page, result = result, status = UsersLoadState.Ready)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                ensureActive()
                if (currentRequest == requestId) {
                    _state.value = _state.value.copy(
                        page = requestedPage,
                        status = UsersLoadState.Error(exception.userListMessage())
                    )
                }
            }
        }
    }

    private companion object { const val PAGE_SIZE = 10 }
}

private fun Throwable.roleChangeMessage(): String = when (this) {
    is SessionRequiredException -> message.orEmpty()
    is ApiException.Network, is java.io.IOException ->
        "No fue posible conectar con el servidor. Revisa tu conexión e intenta nuevamente."
    is ApiException.Http -> when (statusCode) {
        401 -> "Tu sesión expiró. Inicia sesión nuevamente."
        in 500..599 -> "El servicio de usuarios no está disponible. Intenta más tarde."
        else -> serverMessages.filter { it.isNotBlank() }.joinToString("\n").ifBlank {
            if (statusCode == 403) "No tienes autorización para cambiar el rol de esta cuenta."
            else "No se pudo cambiar el rol (HTTP $statusCode)."
        }
    }
    else -> "No se pudo cambiar el rol. Intenta nuevamente."
}

private fun Throwable.userListMessage(): String = when (this) {
    is SessionRequiredException -> message.orEmpty()
    is ApiException.Http -> when (statusCode) {
        401 -> "Tu sesión expiró. Inicia sesión nuevamente."
        403 -> "No tienes autorización para consultar usuarios."
        in 500..599 -> "El servicio de usuarios no está disponible. Intenta más tarde."
        else -> "No se pudo consultar usuarios (HTTP $statusCode)."
    }
    is ApiException.Network, is java.io.IOException -> "No fue posible conectar con el servidor. Revisa tu conexión e intenta nuevamente."
    else -> "No se pudo interpretar la respuesta de usuarios. Intenta nuevamente."
}

class AdminUsersViewModelFactory(private val repository: UserRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(AdminUsersViewModel::class.java))
        return AdminUsersViewModel(repository) as T
    }
}
