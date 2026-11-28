package cl.uct.ojociudadano.ui.admin

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.test.espresso.Espresso.pressBack
import cl.uct.ojociudadano.data.model.Paginated
import cl.uct.ojociudadano.data.model.UserProfile
import cl.uct.ojociudadano.data.remote.error.ApiException
import cl.uct.ojociudadano.data.repository.UserRepository
import cl.uct.ojociudadano.data.session.AuthenticatedSession
import cl.uct.ojociudadano.data.session.DefaultSessionManager
import cl.uct.ojociudadano.data.session.SessionStorage
import cl.uct.ojociudadano.data.session.UserRole
import cl.uct.ojociudadano.ui.theme.OjoCiudadanoTheme
import java.util.Base64
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AdminUsersScreenTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun loadingEmptyErrorAndRetry() {
        val state = mutableStateOf(AdminUsersState())
        var retries = 0
        var backs = 0
        compose.setContent {
            OjoCiudadanoTheme {
                AdminUsersContent(state.value, { backs++ }, {}, {}, {}, { retries++ }, {}, {}, {})
            }
        }
        compose.onNodeWithText("Cargando usuarios…").performScrollTo().assertIsDisplayed()
        compose.runOnIdle {
            state.value = AdminUsersState(result = Paginated(emptyList(), 1, 10, 0, 0), status = UsersLoadState.Ready)
        }
        compose.onNodeWithText("No hay usuarios para el filtro y la página seleccionados.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Siguiente").assertIsNotEnabled()
        compose.onNodeWithText("Anterior").assertIsNotEnabled()
        compose.runOnIdle { state.value = AdminUsersState(status = UsersLoadState.Error("Revisa tu conexión.")) }
        compose.onNodeWithText("Reintentar").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Volver").performClick()
        assertEquals(1, retries)
        assertEquals(1, backs)
    }

    @Test fun rowsFiltersAndServerPagination() {
        var selected: UserRoleFilter? = null
        var next = 0
        val user = UserProfile("u1", "Ana", "ana@example.invalid", null, null, listOf("ADMIN"))
        compose.setContent {
            OjoCiudadanoTheme {
                AdminUsersContent(
                    AdminUsersState(result = Paginated(listOf(user), 1, 10, 21, 3), status = UsersLoadState.Ready),
                    {}, { selected = it }, {}, { next++ }, {}, {}, {}, {}
                )
            }
        }
        compose.onNodeWithText("Todos").assertIsSelected()
        compose.onNodeWithText("Ciudadano").performScrollTo().performClick()
        assertEquals(UserRoleFilter.CITIZEN, selected)
        compose.onNode(hasText("Administrador") and hasClickAction()).performScrollTo().performClick()
        assertEquals(UserRoleFilter.ADMIN, selected)
        compose.onNodeWithText("Ana").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("ana@example.invalid").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Rol: Administrador").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Página 1 de 3 · 21 usuarios").assertIsDisplayed()
        compose.onNodeWithText("Anterior").assertIsNotEnabled()
        compose.onNodeWithText("Siguiente").performClick()
        assertEquals(1, next)
    }

    @Test fun roleChangeRequiresConfirmationAndShowsProgressUntilSuccess() {
        val user = UserProfile("u1", "Ana", "ana@example.invalid", null, null, listOf("CIUDADANO"))
        val confirmedRoles = CompletableDeferred<List<String>>()
        val requests = mutableListOf<Pair<String, UserRole>>()
        val repository = object : UserRepository {
            override suspend fun getUsers(page: Int, limit: Int, role: UserRole?) =
                Paginated(listOf(user), page, limit, 1, 1)

            override suspend fun updateUserRole(userId: String, role: UserRole): List<String> {
                requests += userId to role
                return confirmedRoles.await()
            }
        }
        showUsers(repository)

        fun openConfirmation() {
            compose.onNodeWithText("Cambiar rol").performScrollTo().performClick()
            compose.onNodeWithText("¿Quieres cambiar el rol de Ana a Administrador?").assertIsDisplayed()
        }

        openConfirmation()
        compose.onNodeWithText("Cancelar").performClick()
        openConfirmation()
        pressBack()
        compose.onNodeWithText("Confirmar").assertDoesNotExist()
        compose.onNodeWithText("Rol: Ciudadano").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, requests.size) }

        openConfirmation()
        compose.onNodeWithText("Confirmar").performClick()
        compose.onNodeWithText("Cambiando rol…").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Rol: Ciudadano").performScrollTo().assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(listOf(user.id to UserRole.ADMIN), requests)
            confirmedRoles.complete(listOf("ADMIN"))
        }
        compose.onNodeWithText("Rol: Administrador").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Cambiando rol…").assertDoesNotExist()
    }

    @Test fun currentAccountRemainsIdentifiableAcrossFiltersAndPages() {
        val ownAccount = UserProfile(ADMIN_ID, "Ana", "admin@example.invalid", null, null, listOf("ADMIN"))
        val otherAccount = ownAccount.copy(id = "u2", email = "otra@example.invalid")
        val repository = object : UserRepository {
            override suspend fun getUsers(page: Int, limit: Int, role: UserRole?): Paginated<UserProfile> {
                if (role == UserRole.CIUDADANO) return Paginated(emptyList(), page, limit, 0, 0)
                val account = if (page == 1) otherAccount else ownAccount
                return Paginated(listOf(account), page, limit, 11, 2)
            }

            override suspend fun updateUserRole(userId: String, role: UserRole): List<String> =
                error("The test cancels the only confirmation")
        }
        showUsers(repository)

        fun assertOwnAccount() {
            rowText("Tú").assertIsDisplayed()
            rowText("Rol: Administrador").assertIsDisplayed()
            rowText("Cambiar rol").assertIsDisplayed().assertIsNotEnabled()
        }

        rowText("Cambiar rol").assertIsEnabled().performClick()
        compose.onNodeWithText("¿Quieres cambiar el rol de Ana a Ciudadano?").assertIsDisplayed()
        compose.onNodeWithText("Confirmar").assertIsEnabled()
        compose.onNodeWithText("Cancelar").performClick()
        compose.onNodeWithText("Tú").assertDoesNotExist()
        compose.onNodeWithText("Siguiente").performClick()
        assertOwnAccount()

        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Ciudadano"))
        compose.onNodeWithText("Ciudadano").performClick()
        rowText("No hay usuarios para el filtro y la página seleccionados.").assertIsDisplayed()
        compose.onNodeWithText("Tú").assertDoesNotExist()

        compose.onNode(hasText("Administrador") and hasClickAction()).performScrollTo().performClick()
        rowText("Cambiar rol").assertIsEnabled()
        compose.onNodeWithText("Siguiente").performClick()
        assertOwnAccount()
    }

    @Test fun rejectedChangesShowErrorsKeepTheRoleAndLeaveTheListUsable() {
        val user = UserProfile("u1", "Ana", "ana@example.invalid", null, null, listOf("ADMIN"))
        val forbiddenMessage = "No puedes quitarte tu propio rol de administrador."
        val failures = listOf(
            ApiException.Http(400, emptyList(), null, null) to
                "El servidor rechazó el cambio de rol de esta cuenta.",
            ApiException.InsufficientPermissions(listOf(forbiddenMessage)) to forbiddenMessage
        )
        val errors = failures.map { it.first }.iterator()
        val repository = object : UserRepository {
            override suspend fun getUsers(page: Int, limit: Int, role: UserRole?) =
                Paginated(listOf(user), page, limit, 1, 1)

            override suspend fun updateUserRole(userId: String, role: UserRole): List<String> = throw errors.next()
        }
        showUsers(repository)

        for ((_, message) in failures) {
            rowText("Cambiar rol").assertIsEnabled().performClick()
            compose.onNodeWithText("Confirmar").performClick()
            compose.onNodeWithText(message).assertIsDisplayed()
            rowText("Rol: Administrador").assertIsDisplayed()
            rowText("Cambiar rol").assertIsEnabled()
        }
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Todos"))
        compose.onNodeWithText("Ciudadano").performClick().assertIsSelected()
    }

    private fun rowText(text: String) = compose.onNode(hasScrollAction())
        .performScrollToNode(hasText(text)).let { compose.onNodeWithText(text) }

    private fun showUsers(repository: UserRepository) {
        val payload = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("""{"exp":4102444800}""".toByteArray())
        val manager = DefaultSessionManager(object : SessionStorage {
            override fun currentSession() = AuthenticatedSession(
                "header.$payload.signature", "Bearer", ADMIN_ID, "admin@example.invalid", "Ana", listOf("ADMIN")
            )
            override fun currentToken() = currentSession().accessToken
            override fun save(session: AuthenticatedSession) = error("The test only restores a session")
            override fun clear() = error("The test only restores a session")
        })
        runBlocking { manager.restore() }
        val factory = AdminUsersViewModelFactory(repository, manager)
        compose.setContent {
            OjoCiudadanoTheme {
                val model: AdminUsersViewModel = viewModel(factory = factory)
                val state by model.state.collectAsStateWithLifecycle()
                AdminUsersContent(
                    state, {}, model::selectFilter, model::previousPage, model::nextPage, model::retry,
                    model::requestRoleChange, model::dismissRoleChange, model::confirmRoleChange
                )
            }
        }
    }

    private companion object {
        const val ADMIN_ID = "a0000000-0000-4000-8000-000000000001"
    }
}
