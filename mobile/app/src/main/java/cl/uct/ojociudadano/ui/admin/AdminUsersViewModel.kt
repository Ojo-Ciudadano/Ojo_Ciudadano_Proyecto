package cl.uct.ojociudadano.ui.admin

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

data class AdminUsersState(
    val filter: UserRoleFilter = UserRoleFilter.ALL,
    val page: Int = 1,
    val result: Paginated<UserProfile>? = null,
    val status: UsersLoadState = UsersLoadState.Loading
) {
    val canGoBack: Boolean get() = status == UsersLoadState.Ready && page > 1
    val canGoForward: Boolean get() = status == UsersLoadState.Ready && page < (result?.totalPages ?: 0)
}

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
        if (_state.value.status is UsersLoadState.Error) load(_state.value.page, _state.value.filter)
    }

    private fun load(page: Int, filter: UserRoleFilter) {
        val currentRequest = ++requestId
        job?.cancel()
        // Publish the selection immediately, including when clicks arrive before launch runs.
        _state.value = AdminUsersState(filter = filter, page = page)
        job = viewModelScope.launch {
            try {
                val result = repository.getUsers(page, PAGE_SIZE, filter.role)
                ensureActive()
                if (currentRequest == requestId) {
                    _state.value = AdminUsersState(filter, result.page, result, UsersLoadState.Ready)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                ensureActive()
                if (currentRequest == requestId) {
                    _state.value = AdminUsersState(filter, page, status = UsersLoadState.Error(exception.userListMessage()))
                }
            }
        }
    }

    private companion object { const val PAGE_SIZE = 10 }
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
