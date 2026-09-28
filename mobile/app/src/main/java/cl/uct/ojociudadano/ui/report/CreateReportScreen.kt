package cl.uct.ojociudadano.ui.report

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cl.uct.ojociudadano.ui.theme.OjoCiudadanoTheme

/**
 * Formulario visual desacoplado de la navegación y de la capa de datos.
 *
 * El estado y los eventos se reciben desde quien gestione el formulario.
 */
@Composable
fun CreateReportScreen(
    state: CreateReportFormState,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onCategoryClick: () -> Unit,
    onCreateReportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val descriptionFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Crear reporte",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        ReportTextField(
            value = state.title,
            onValueChange = onTitleChange,
            label = "Título",
            placeholder = "Ej: Poste de alumbrado dañado",
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { descriptionFocusRequester.requestFocus() }
            ),
            errorMessage = state.titleError
        )

        ReportTextField(
            value = state.description,
            onValueChange = onDescriptionChange,
            label = "Descripción",
            placeholder = "Describe el problema que deseas reportar",
            singleLine = false,
            minLines = 5,
            modifier = Modifier.focusRequester(descriptionFocusRequester),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                }
            ),
            errorMessage = state.descriptionError
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Categoría",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            OutlinedButton(
                onClick = onCategoryClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = state.categoryName.ifBlank { "Seleccionar categoría" },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            state.categoryError?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Ubicación",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = "Ubicación pendiente",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                keyboardController?.hide()
                focusManager.clearFocus()
                onCreateReportClick()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Crear reporte")
        }
    }
}

@Composable
private fun ReportTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    singleLine: Boolean,
    minLines: Int = 1,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions,
    errorMessage: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        isError = errorMessage != null,
        supportingText = errorMessage?.let { error -> { Text(error) } }
    )
}

/**
 * Adaptador temporal para conservar la llamada de navegación existente hasta que
 * la pantalla tenga un propietario de estado dedicado.
 */
@Composable
fun CreateReportScreen(modifier: Modifier = Modifier, onBack: () -> Unit = {}) {
    var state by remember { mutableStateOf(CreateReportFormState()) }
    val viewModel = remember { CreateReportViewModel() }

    CreateReportScreen(
        state = state,
        onTitleChange = { state = state.copy(title = it) },
        onDescriptionChange = { state = state.copy(description = it) },
        onCategoryClick = {},
        onCreateReportClick = { state = viewModel.validateForm(state) },
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
private fun CreateReportScreenPreview() {
    OjoCiudadanoTheme {
        CreateReportScreen(
            state = CreateReportFormState(
                title = "Poste de alumbrado dañado",
                description = "La luminaria lleva varios días sin funcionar",
                categoryName = "Alumbrado público"
            ),
            onTitleChange = {},
            onDescriptionChange = {},
            onCategoryClick = {},
            onCreateReportClick = {}
        )
    }
}
