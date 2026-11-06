package cl.uct.ojociudadano.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import cl.uct.ojociudadano.data.model.CandidateUser
import cl.uct.ojociudadano.data.model.IncidentAssignment
import cl.uct.ojociudadano.ui.components.CitizenMessage
import cl.uct.ojociudadano.ui.reports.incidentDate

/** Callbacks owned by the screen so the section stays stateless. */
data class AdminIncidentAssignmentActions(
    val onOpen: () -> Unit = {},
    val onDismiss: () -> Unit = {},
    val onSelect: (String) -> Unit = {},
    val onConfirm: () -> Unit = {},
    val onRetryCandidates: () -> Unit = {}
)

@Composable
internal fun AdminIncidentAssignmentSection(
    assignment: IncidentAssignment?,
    state: AdminIncidentDetailState,
    actions: AdminIncidentAssignmentActions,
    modifier: Modifier = Modifier
) {
    val responsibleName = state.responsibleName
    DetailSection("Responsable", modifier) {
        if (assignment == null) {
            Text("Sin responsable asignado.", style = MaterialTheme.typography.bodyMedium)
        } else {
            DetailField("Usuario asignado", responsibleName ?: assignment.assignedUserId)
            DetailField("Asignado el", incidentDate(assignment.assignmentDate))
        }
        if (state.assignmentCompleted) {
            CitizenMessage("Responsable actualizado correctamente.")
        }
        OutlinedButton(
            onClick = actions.onOpen,
            enabled = state.canStartAction,
            modifier = Modifier.fillMaxWidth().testTag("admin_assignment_open")
        ) {
            Text(if (assignment == null) "Asignar responsable" else "Reasignar responsable")
        }
    }
    if (state.pickerVisible) {
        AssignmentPickerDialog(assignment, state, actions)
    }
}

@Composable
private fun AssignmentPickerDialog(
    assignment: IncidentAssignment?,
    state: AdminIncidentDetailState,
    actions: AdminIncidentAssignmentActions
) {
    AlertDialog(
        onDismissRequest = actions.onDismiss,
        title = { Text(if (assignment == null) "Asignar responsable" else "Reasignar responsable") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (val candidates = state.candidates) {
                    AssignmentCandidates.Loading -> Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                    ) {
                        CircularProgressIndicator(Modifier.size(20.dp))
                        Text("Cargando administradores…")
                    }
                    is AssignmentCandidates.Failed -> {
                        CitizenMessage(candidates.message, isError = true)
                        if (candidates.canRetry) {
                            TextButton(onClick = actions.onRetryCandidates) { Text("Reintentar") }
                        }
                    }
                    is AssignmentCandidates.Ready -> {
                        if (candidates.users.isEmpty()) {
                            Text("No hay administradores disponibles.")
                        } else {
                            Column(
                                Modifier
                                    .heightIn(max = 320.dp)
                                    .verticalScroll(rememberScrollState())
                                    .selectableGroup(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                candidates.users.forEach { candidate ->
                                    CandidateRow(
                                        candidate = candidate,
                                        selected = candidate.id == state.selectedCandidateId,
                                        enabled = !state.assigning,
                                        onClick = { actions.onSelect(candidate.id) }
                                    )
                                }
                            }
                            if (candidates.total > candidates.users.size) {
                                Text(
                                    "Se muestran los primeros ${candidates.users.size} administradores.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                if (state.assigning) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                    ) {
                        CircularProgressIndicator(Modifier.size(20.dp))
                        Text("Asignando…")
                    }
                }
                state.assignmentError?.let { CitizenMessage(it.message, isError = true) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = actions.onConfirm,
                enabled = state.selectedCandidateId != null && !state.assigning,
                modifier = Modifier.testTag("admin_assignment_confirm")
            ) {
                Text(if (state.assigning) "Asignando…" else "Asignar")
            }
        },
        dismissButton = {
            TextButton(
                onClick = actions.onDismiss,
                enabled = !state.assigning,
                modifier = Modifier.testTag("admin_assignment_cancel")
            ) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun CandidateRow(
    candidate: CandidateUser,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RadioButton(selected = selected, onClick = null, enabled = enabled)
        Column(Modifier.weight(1f)) {
            Text(
                candidate.name.ifBlank { "Administrador" },
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                candidate.email,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
