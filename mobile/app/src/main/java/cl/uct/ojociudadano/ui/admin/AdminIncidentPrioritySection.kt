package cl.uct.ojociudadano.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import cl.uct.ojociudadano.ui.components.CitizenMessage

/** Callbacks exposed by the priority section so the screen stays free of ViewModel details. */
data class AdminIncidentPriorityActions(
    val onOpen: () -> Unit = {},
    val onDismiss: () -> Unit = {},
    val onSelect: (Int) -> Unit = {},
    val onConfirm: () -> Unit = {}
)

@Composable
internal fun AdminIncidentPrioritySection(
    state: AdminIncidentDetailState,
    actions: AdminIncidentPriorityActions,
    modifier: Modifier = Modifier
) {
    DetailSection("Prioridad", modifier) {
        DetailField(
            "Prioridad actual",
            state.currentPriority?.let { "Nivel $it de ${ADMIN_PRIORITY_LEVELS.last}" } ?: "Sin prioridad registrada"
        )
        if (state.priorityUpdated) {
            CitizenMessage("Prioridad actualizada correctamente.")
        }
        OutlinedButton(
            onClick = actions.onOpen,
            enabled = state.canStartAction,
            modifier = Modifier.testTag("admin_priority_open")
        ) {
            Text("Modificar prioridad")
        }
    }
}

@Composable
internal fun PriorityPickerDialog(
    state: AdminIncidentDetailState,
    actions: AdminIncidentPriorityActions
) {
    if (!state.priorityDialogVisible) return
    AlertDialog(
        onDismissRequest = { if (!state.savingPriority) actions.onDismiss() },
        title = { Text("Modificar prioridad") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "La prioridad admitida va de ${ADMIN_PRIORITY_LEVELS.first} (mínima) a " +
                        "${ADMIN_PRIORITY_LEVELS.last} (crítica).",
                    style = MaterialTheme.typography.bodyMedium
                )
                Column(Modifier.selectableGroup()) {
                    ADMIN_PRIORITY_LEVELS.forEach { level ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = state.selectedPriority == level,
                                    enabled = !state.savingPriority,
                                    role = Role.RadioButton,
                                    onClick = { actions.onSelect(level) }
                                )
                                .padding(vertical = 4.dp)
                                .testTag("admin_priority_option_$level"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = state.selectedPriority == level,
                                onClick = null,
                                enabled = !state.savingPriority
                            )
                            Text(
                                "Nivel $level",
                                Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
                state.priorityError?.let { CitizenMessage(it, isError = true) }
                if (state.savingPriority) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                    ) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text("Guardando prioridad…")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = actions.onConfirm,
                enabled = state.canConfirmPriority,
                modifier = Modifier.testTag("admin_priority_confirm")
            ) {
                Text(if (state.priorityCanRetry && state.priorityError != null) "Reintentar" else "Confirmar")
            }
        },
        dismissButton = {
            TextButton(
                onClick = actions.onDismiss,
                enabled = !state.savingPriority,
                modifier = Modifier.testTag("admin_priority_cancel")
            ) {
                Text("Cancelar")
            }
        }
    )
}
