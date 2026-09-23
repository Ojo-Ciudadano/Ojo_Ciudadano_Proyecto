package cl.uct.ojociudadano.ui.report

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
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
    onCreateReportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
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
            singleLine = true
        )

        ReportTextField(
            value = state.description,
            onValueChange = onDescriptionChange,
            label = "Descripción",
            singleLine = false,
            minLines = 4
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Categoría",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            OutlinedTextField(
                value = state.categoryName.ifBlank { "Seleccionar categoría" },
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                label = { Text("Categoría") }
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Ubicación",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            OutlinedTextField(
                value = "Información de ubicación",
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                label = { Text("Ubicación") }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onCreateReportClick,
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
    singleLine: Boolean,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            keyboardType = KeyboardType.Text
        )
    )
}

/**
 * Adaptador temporal para conservar la llamada de navegación existente hasta que
 * la pantalla tenga un propietario de estado dedicado.
 */
@Composable
fun CreateReportScreen(modifier: Modifier = Modifier, onBack: () -> Unit = {}) {
    var state by remember { mutableStateOf(CreateReportFormState()) }

    CreateReportScreen(
        state = state,
        onTitleChange = { state = state.copy(title = it) },
        onDescriptionChange = { state = state.copy(description = it) },
        onCreateReportClick = {},
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
private fun CreateReportScreenPreview() {
    OjoCiudadanoTheme {
        CreateReportScreen(
            state = CreateReportFormState(
                title = "Bache en la calle",
                description = "Hay un bache grande frente a la plaza.",
                categoryName = "Infraestructura"
            ),
            onTitleChange = {},
            onDescriptionChange = {},
            onCreateReportClick = {}
        )
    }
}
