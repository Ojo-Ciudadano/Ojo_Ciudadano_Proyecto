package cl.uct.ojociudadano.ui.report

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.uct.ojociudadano.data.model.IncidentPhotoEvidence
import java.util.Locale

object ReportPhotoTestTags {
    const val SECTION = "report_photo_section"
    const val CAPTURE_BUTTON = "report_photo_capture_button"
    const val CAPTURING_INDICATOR = "report_photo_capturing_indicator"
    const val CAPTURED_CARD = "report_photo_captured_card"
    const val ERROR_MESSAGE = "report_photo_error_message"
}

/**
 * Sección del formulario que permite incorporar una fotografía de evidencia.
 *
 * Únicamente representa el resultado de la operación. La previsualización, la
 * eliminación y el envío de la evidencia pertenecen a tareas posteriores, por
 * lo que aquí solo se confirma que la fotografía quedó disponible para el
 * flujo de creación del incidente.
 *
 * [activePhotoOperation] indica si la cámara o la galería están en curso. La
 * sección solo necesita conocer que hay una operación activa para bloquear sus
 * acciones y mostrar el progreso correspondiente.
 */
@Composable
internal fun EvidencePhotoSection(
    evidencePhoto: IncidentPhotoEvidence?,
    activePhotoOperation: PhotoOperation?,
    errorMessage: String?,
    enabled: Boolean,
    onTakePhotoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(ReportPhotoTestTags.SECTION),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Evidencia fotográfica",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Adjunta una fotografía del incidente. La imagen se conserva solo " +
                "mientras completas el reporte.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (evidencePhoto != null) {
            CapturedPhotoCard(evidencePhoto = evidencePhoto)
        }

        OutlinedButton(
            onClick = onTakePhotoClick,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(ReportPhotoTestTags.CAPTURE_BUTTON),
            enabled = enabled && activePhotoOperation == null
        ) {
            if (activePhotoOperation != null) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(18.dp)
                        .testTag(ReportPhotoTestTags.CAPTURING_INDICATOR),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    when (activePhotoOperation) {
                        PhotoOperation.CAMERA -> "Abriendo cámara..."
                        PhotoOperation.GALLERY -> "Procesando fotografía..."
                    }
                )
            } else {
                Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    if (evidencePhoto == null) {
                        "Tomar fotografía"
                    } else {
                        "Tomar otra fotografía"
                    }
                )
            }
        }

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                modifier = Modifier.testTag(ReportPhotoTestTags.ERROR_MESSAGE),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun CapturedPhotoCard(evidencePhoto: IncidentPhotoEvidence) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(ReportPhotoTestTags.CAPTURED_CARD),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.PhotoCamera,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Fotografía capturada",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = formatEvidencePhotoSize(evidencePhoto.sizeBytes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Presenta el tamaño de la evidencia en una unidad legible para el usuario.
 */
internal fun formatEvidencePhotoSize(sizeBytes: Long): String {
    if (sizeBytes <= 0L) {
        return "Tamaño desconocido"
    }

    return when {
        sizeBytes < KILOBYTE -> "$sizeBytes B"
        sizeBytes < MEGABYTE -> String.format(
            Locale.getDefault(),
            "%.1f KB",
            sizeBytes / KILOBYTE.toDouble()
        )
        else -> String.format(
            Locale.getDefault(),
            "%.1f MB",
            sizeBytes / MEGABYTE.toDouble()
        )
    }
}

private const val KILOBYTE = 1024L
private const val MEGABYTE = KILOBYTE * 1024L
