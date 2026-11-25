package cl.uct.ojociudadano.ui.admin

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import cl.uct.ojociudadano.data.model.Paginated
import cl.uct.ojociudadano.data.model.UserProfile
import cl.uct.ojociudadano.ui.theme.OjoCiudadanoTheme
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
                AdminUsersContent(state.value, { backs++ }, {}, {}, {}, { retries++ })
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
                    {}, { selected = it }, {}, { next++ }, {}
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
}
