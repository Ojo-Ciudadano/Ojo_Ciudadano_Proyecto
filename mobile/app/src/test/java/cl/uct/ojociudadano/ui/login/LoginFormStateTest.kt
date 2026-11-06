package cl.uct.ojociudadano.ui.login

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginFormStateTest {

    @Test
    fun `password is hidden by default`() {
        assertFalse(LoginFormState().isPasswordVisible)
    }

    @Test
    fun `updating email clears only its previous error`() {
        val state = LoginFormState(
            emailError = "El correo electrónico es obligatorio",
            passwordError = "La contraseña es obligatoria"
        )

        val updatedState = state.updateEmail("ciudadano@ojociudadano.cl")

        assertEquals("ciudadano@ojociudadano.cl", updatedState.email)
        assertNull(updatedState.emailError)
        assertEquals(state.passwordError, updatedState.passwordError)
    }

    @Test
    fun `updating password clears only its previous error`() {
        val state = LoginFormState(
            emailError = "El correo electrónico es obligatorio",
            passwordError = "La contraseña es obligatoria"
        )

        val updatedState = state.updatePassword("secreta")

        assertEquals("secreta", updatedState.password)
        assertNull(updatedState.passwordError)
        assertEquals(state.emailError, updatedState.emailError)
    }

    @Test
    fun `toggling password visibility changes only visibility`() {
        val state = LoginFormState(email = "ciudadano@ojociudadano.cl", password = "secreta")

        val visibleState = state.togglePasswordVisibility()

        assertTrue(visibleState.isPasswordVisible)
        assertEquals(state.email, visibleState.email)
        assertEquals(state.password, visibleState.password)
    }
}
