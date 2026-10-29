package cl.uct.ojociudadano.ui.report

import cl.uct.ojociudadano.ui.components.CitizenHeader
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.ui.graphics.vector.ImageVector
import cl.uct.ojociudadano.data.model.Category
import cl.uct.ojociudadano.data.model.IncidentPhotoEvidence
import cl.uct.ojociudadano.OjoCiudadanoApplication
import cl.uct.ojociudadano.ui.report.photo.rememberGalleryPhotoPickerLauncher
import cl.uct.ojociudadano.ui.report.photo.rememberPhotoCaptureLauncher
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
    onTakePhotoClick: () -> Unit,
    onPickPhotoClick: () -> Unit,
    onRemovePhoto: (IncidentPhotoEvidence) -> Unit,
    onPhotoPreviewError: (IncidentPhotoEvidence) -> Unit,
    onCreateReportClick: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
) {
    val descriptionFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .animateContentSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CitizenHeader("Crear reporte", onBack = onBack)

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
            errorMessage = state.titleError,
            leadingIcon = Icons.Filled.Edit
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
            errorMessage = state.descriptionError,
            leadingIcon = Icons.Filled.Description
        )

        CategorySelector(
            selectedCategory = state.selectedCategory,
            categories = categories,
            isLoading = isLoadingCategories,
            loadErrorMessage = categoryLoadError,
            errorMessage = state.categoryError,
            onCategorySelected = onCategorySelected
        )

        EvidencePhotoSection(
            evidencePhotos = state.evidencePhotos,
            activePhotoOperation = state.activePhotoOperation,
            errorMessage = state.photoError,
            enabled = submissionState !is CreateReportSubmissionState.Loading,
            onTakePhotoClick = onTakePhotoClick,
            onPickPhotoClick = onPickPhotoClick,
            onRemovePhoto = onRemovePhoto,
            onPhotoPreviewError = onPhotoPreviewError
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
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Ubicación pendiente", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
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
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
            Spacer(modifier = Modifier.size(8.dp))
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
            leadingIcon = { Icon(Icons.Filled.Category, contentDescription = null) },
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
    errorMessage: String? = null,
    leadingIcon: ImageVector? = null
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
        leadingIcon = leadingIcon?.let { icon -> { Icon(icon, contentDescription = null) } },
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
    val fileStore = application.container.evidencePhotoStore
    val viewModel: CreateReportViewModel = viewModel(
        factory = remember(application) {
            CreateReportViewModelFactory(
                categoryRepository = application.container.categoryRepository,
                incidentRepository = application.container.incidentRepository,
                photoFileStore = fileStore
            )
        }
    )

    val photoCaptureLauncher = rememberPhotoCaptureLauncher(
        fileStore = fileStore,
        onStart = viewModel::onPhotoCaptureStarted,
        onCaptured = viewModel::onPhotoCaptured,
        onCancelled = viewModel::onPhotoCaptureCancelled,
        onError = viewModel::onPhotoCaptureError
    )

    val galleryPhotoPickerLauncher = rememberGalleryPhotoPickerLauncher(
        onStart = viewModel::onGallerySelectionStarted,
        onSelected = viewModel::onGalleryPhotoSelected,
        onCancelled = viewModel::onGallerySelectionCancelled,
        onError = viewModel::onGallerySelectionError
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
        onTakePhotoClick = photoCaptureLauncher::launch,
        onPickPhotoClick = galleryPhotoPickerLauncher::launch,
        onRemovePhoto = viewModel::onRemovePhoto,
        onPhotoPreviewError = viewModel::onPhotoPreviewError,
        onCreateReportClick = viewModel::createReport,
        modifier = modifier,
        onBack = onBack
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
                selectedCategory = previewCategory,
                evidencePhotos = listOf(previewEvidencePhoto)
            ),
            categories = listOf(previewCategory),
            isLoadingCategories = false,
            categoryLoadError = null,
            submissionState = CreateReportSubmissionState.Idle,
            onTitleChange = {},
            onDescriptionChange = {},
            onCategorySelected = {},
            onTakePhotoClick = {},
            onPickPhotoClick = {},
            onRemovePhoto = {},
            onPhotoPreviewError = {},
            onCreateReportClick = {}
        )
    }
}

private val previewEvidencePhoto = IncidentPhotoEvidence(
    localUri = "content://cl.uct.ojociudadano.fileprovider/evidence_photos/evidence_preview.jpg",
    fileName = "evidence_preview.jpg",
    sizeBytes = 1_258_291L,
    capturedAt = 0L
)

private val previewCategory = Category(
    id = "preview-category",
    name = "Categoría de ejemplo",
    description = "",
    basePriority = 0,
    isCriticalEmergency = false,
    officialContactChannel = null
)
