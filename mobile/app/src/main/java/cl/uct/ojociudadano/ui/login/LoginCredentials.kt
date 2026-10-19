package cl.uct.ojociudadano.ui.login

/**
 * Datos entregados por la interfaz cuando las validaciones locales del login son correctas.
 *
 * No representa una sesión autenticada ni realiza persistencia; quien use la pantalla decide
 * cómo procesar estas credenciales.
 */
data class LoginCredentials(
    val email: String,
    val password: String
)
