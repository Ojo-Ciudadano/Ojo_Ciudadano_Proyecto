package cl.uct.ojociudadano.ui.registration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import cl.uct.ojociudadano.data.model.RegistrationInput

@Composable
fun RegistrationRoute(viewModel: RegistrationViewModel, onBack: () -> Unit, onSuccess: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(state) { if (state == RegistrationUiState.Success) onSuccess() }
    RegistrationScreen(state, viewModel::register, onBack)
}

@Composable
fun RegistrationScreen(
    state: RegistrationUiState,
    onRegister: (RegistrationInput) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // No rememberSaveable: la contraseña permanece solo en memoria, fuera de Bundle/disco.
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    val loading = state == RegistrationUiState.Loading || state == RegistrationUiState.Success
    val fields = when (state) {
        is RegistrationUiState.Invalid -> state.fields
        is RegistrationUiState.Error -> state.fields
        else -> RegistrationFieldErrors()
    }
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    fun submit() {
        if (loading) return
        keyboard?.hide()
        focus.clearFocus()
        onRegister(RegistrationInput(name, email, password, phone))
    }
    Column(
        modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Crear cuenta", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Regístrate para participar como ciudadano.", style = MaterialTheme.typography.bodyMedium)
        if (state is RegistrationUiState.Error) {
            Text(state.message, color = MaterialTheme.colorScheme.error)
        }
        OutlinedTextField(
            name, { name = it }, Modifier.fillMaxWidth().testTag("register_name"),
            label = { Text("Nombre completo") }, enabled = !loading, singleLine = true,
            isError = fields.name != null, supportingText = fields.name?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next)
        )
        OutlinedTextField(
            email, { email = it }, Modifier.fillMaxWidth().testTag("register_email"),
            label = { Text("Correo electrónico") }, enabled = !loading, singleLine = true,
            isError = fields.email != null, supportingText = fields.email?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
        )
        OutlinedTextField(
            password, { password = it }, Modifier.fillMaxWidth().testTag("register_password"),
            label = { Text("Contraseña") }, enabled = !loading, singleLine = true,
            isError = fields.password != null, supportingText = fields.password?.let { { Text(it) } },
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
            trailingIcon = {
                IconButton(onClick = { visible = !visible }, enabled = !loading) {
                    Icon(if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        if (visible) "Ocultar contraseña" else "Mostrar contraseña")
                }
            }
        )
        OutlinedTextField(
            phone, { phone = it }, Modifier.fillMaxWidth().testTag("register_phone"),
            label = { Text("Teléfono (opcional)") }, enabled = !loading, singleLine = true,
            isError = fields.phone != null, supportingText = fields.phone?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() })
        )
        if (loading) {
            CircularProgressIndicator(Modifier.testTag("register_loading"))
            Text("Creando cuenta…")
        }
        Button(onClick = ::submit, enabled = !loading, modifier = Modifier.fillMaxWidth().testTag("register_submit")) {
            Text("Registrarse")
        }
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Volver al inicio de sesión") }
    }
}
