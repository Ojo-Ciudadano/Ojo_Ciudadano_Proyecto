package cl.uct.ojociudadano.ui.registration

import cl.uct.ojociudadano.data.model.RegistrationInput
import org.junit.Assert.*
import org.junit.Test

class RegistrationFormValidatorTest {
    private val validator = RegistrationFormValidator()
    private fun input(name: String = "Ana", email: String = "ana@example.invalid", password: String = "123456", phone: String = "") =
        RegistrationInput(name, email, password, phone)

    @Test fun emptyName() { assertNotNull(validator.validate(input(name = "  ")).name) }
    @Test fun emptyEmail() { assertNotNull(validator.validate(input(email = "")).email) }
    @Test fun invalidEmail() {
        listOf("ana", "a@", "a@b", "a b@example.invalid").forEach {
            assertNotNull(validator.validate(input(email = it)).email)
        }
    }
    @Test fun emptyPassword() { assertNotNull(validator.validate(input(password = "")).password) }
    @Test fun shortPassword() { assertNotNull(validator.validate(input(password = "12345")).password) }
    @Test fun phoneIsOptionalAndHasNoInventedRestrictions() {
        listOf("", "  ", "+56 9 1234 5678", "anexo 10").forEach {
            assertFalse(validator.validate(input(phone = it)).hasErrors)
        }
    }
    @Test fun validFormAndTrimmedEmail() {
        assertFalse(validator.validate(input(email = " ana@example.invalid ")).hasErrors)
    }
    @Test fun unicodePasswordLengthMatchesBackendCodePoints() {
        assertNotNull(validator.validate(input(password = "😀😀😀")).password)
    }
}
