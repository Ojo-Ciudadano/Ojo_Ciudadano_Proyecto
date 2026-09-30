package cl.uct.ojociudadano.ui.report

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cl.uct.ojociudadano.data.model.Category
import cl.uct.ojociudadano.OjoCiudadanoApplication
import cl.uct.ojociudadano.ui.theme.OjoCiudadanoTheme
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Formulario visual desacoplado de la navegación y de la capa de datos.
 *
 * El estado y los eventos se reciben desde quien gestione el formulario.
 */
@Composable
fun CreateReportScreen(
    state: CreateReportFormState,
    categories: List<Category>,
    isLoadingCategories: Boolean,
    categoryLoadError: String?,
    submissionState: CreateReportSubmissionState,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onCategorySelected: (Category) -> Unit,
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

        CategorySelector(
            selectedCategory = state.selectedCategory,
            categories = categories,
            isLoading = isLoadingCategories,
            loadErrorMessage = categoryLoadError,
            errorMessage = state.categoryError,
            onCategorySelected = onCategorySelected
        )

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
            modifier = Modifier.fillMaxWidth(),
            enabled = submissionState !is CreateReportSubmissionState.Loading
        ) {
            Text(
                if (submissionState is CreateReportSubmissionState.Loading) {
                    "Creando reporte..."
                } else {
                    "Crear reporte"
                }
            )
        }

        if (submissionState is CreateReportSubmissionState.Success) {
            Text("Reporte creado correctamente")
        }

        if (submissionState is CreateReportSubmissionState.Error) {
            Text(
                text = submissionState.message,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategorySelector(
    selectedCategory: Category?,
    categories: List<Category>,
    isLoading: Boolean,
    loadErrorMessage: String?,
    errorMessage: String?,
    onCategorySelected: (Category) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val supportingMessage = listOfNotNull(errorMessage, loadErrorMessage)
        .joinToString("\n")
        .ifBlank { null }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            if (!isLoading) {
                expanded = !expanded
            }
        },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedCategory?.name.orEmpty(),
            onValueChange = {},
            modifier = Modifier
                .menuAnchor(
                    type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                    enabled = !isLoading
                )
                .fillMaxWidth(),
            readOnly = true,
            enabled = !isLoading,
            label = { Text("Categoría") },
            placeholder = {
                Text(if (isLoading) "Cargando categorías..." else "Seleccionar categoría")
            },
            trailingIcon = {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            },
            isError = supportingMessage != null,
            supportingText = supportingMessage?.let { message -> { Text(message) } }
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            if (categories.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("No hay categorías disponibles") },
                    onClick = {},
                    enabled = false
                )
            } else {
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category.name) },
                        onClick = {
                            onCategorySelected(category)
                            expanded = false
                        }
                    )
                }
            }
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
    val application = LocalContext.current.applicationContext as OjoCiudadanoApplication
    val viewModel: CreateReportViewModel = viewModel(
        factory = remember(application) {
            CreateReportViewModelFactory(
                categoryRepository = application.container.categoryRepository,
                incidentRepository = application.container.incidentRepository,
                sessionStorage = application.container.sessionStorage
            )
        }
    )

    CreateReportScreen(
        state = viewModel.formState,
        categories = viewModel.categoryUiState.categories,
        isLoadingCategories = viewModel.categoryUiState.isLoading,
        categoryLoadError = viewModel.categoryUiState.errorMessage,
        submissionState = viewModel.submissionState,
        onTitleChange = viewModel::onTitleChange,
        onDescriptionChange = viewModel::onDescriptionChange,
        onCategorySelected = viewModel::onCategorySelected,
        onCreateReportClick = viewModel::onCreateReportClick,
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
                selectedCategory = previewCategory
            ),
            categories = listOf(previewCategory),
            isLoadingCategories = false,
            categoryLoadError = null,
            submissionState = CreateReportSubmissionState.Idle,
            onTitleChange = {},
            onDescriptionChange = {},
            onCategorySelected = {},
            onCreateReportClick = {}
        )
    }
}

private val previewCategory = Category(
    id = "preview-category",
    name = "Categoría de ejemplo",
    description = "",
    basePriority = 0,
    isCriticalEmergency = false,
    officialContactChannel = null
)
