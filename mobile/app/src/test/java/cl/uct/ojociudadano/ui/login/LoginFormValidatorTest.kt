package cl.uct.ojociudadano.ui.login

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginFormValidatorTest {

    private val validator = LoginFormValidator()

    @Test
    fun `email with surrounding whitespace reaches login without changing password`() {
        val state = LoginFormState(email = " ciudadano@example.invalid ", password = " secreta ")
        val validated = validator.validate(state)

        assertFalse(validated.hasValidationErrors())
        assertEquals(state.email, validated.email)
        assertEquals(state.password, validated.password)
    }

    @Test
    fun `empty email and password have required field errors`() {
        val validatedState = validator.validate(LoginFormState())

        assertEquals("El correo electrónico es obligatorio", validatedState.emailError)
        assertEquals("La contraseña es obligatoria", validatedState.passwordError)
        assertTrue(validatedState.hasValidationErrors())
    }

    @Test
    fun `invalid email format has a clear error`() {
        val validatedState = validator.validate(
            LoginFormState(email = "correo-invalido", password = "secreta")
        )

        assertEquals("Ingresa un correo electrónico válido", validatedState.emailError)
        assertNull(validatedState.passwordError)
    }

    @Test
    fun `valid credentials have no local validation errors`() {
        val validatedState = validator.validate(
            LoginFormState(email = "ciudadano@ojociudadano.cl", password = "secreta")
        )

        assertFalse(validatedState.hasValidationErrors())
    }
}
