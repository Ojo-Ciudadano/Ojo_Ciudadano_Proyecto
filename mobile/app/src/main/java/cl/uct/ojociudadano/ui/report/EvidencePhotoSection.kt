package cl.uct.ojociudadano.ui.report

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.uct.ojociudadano.data.model.IncidentPhotoEvidence
import java.util.Locale

object ReportPhotoTestTags {
    const val SECTION = "report_photo_section"
    const val CAPTURE_BUTTON = "report_photo_capture_button"
    const val GALLERY_BUTTON = "report_photo_gallery_button"
    const val CAPTURING_INDICATOR = "report_photo_capturing_indicator"
    const val GALLERY_INDICATOR = "report_photo_gallery_indicator"
    const val ERROR_MESSAGE = "report_photo_error_message"
    const val PHOTO_LIST = "report_photo_list"
    const val PREVIEW_DIALOG = "report_photo_preview_dialog"
    const val CLOSE_PREVIEW_BUTTON = "report_photo_close_preview"

    private const val PHOTO_THUMBNAIL_PREFIX = "report_photo_thumbnail_"
    private const val REMOVE_PHOTO_BUTTON_PREFIX = "report_photo_remove_"
    private const val PHOTO_PREVIEW_ERROR_PREFIX = "report_photo_preview_error_"
    private const val PHOTO_LOADED_PREFIX = "report_photo_loaded_"

    fun thumbnail(fileName: String): String = "$PHOTO_THUMBNAIL_PREFIX$fileName"

    fun removeButton(fileName: String): String = "$REMOVE_PHOTO_BUTTON_PREFIX$fileName"

    fun previewError(fileName: String): String = "$PHOTO_PREVIEW_ERROR_PREFIX$fileName"

    fun loaded(fileName: String): String = "$PHOTO_LOADED_PREFIX$fileName"
}

/**
 * Sección del formulario que permite incorporar y revisar fotografías de
 * evidencia.
 *
 * La sección solo conoce las evidencias locales y notifica al formulario
 * cuándo se solicita capturar, elegir o eliminar una fotografía. La carga de
 * las imágenes se resuelve desde su URI local, sin depender de que hayan sido
 * subidas al backend.
 *
 * [activePhotoOperation] indica si la cámara o la galería están en curso. La
 * sección bloquea sus acciones mientras el sistema toma el control de la
 * pantalla, pero mantiene visibles las fotografías ya agregadas.
 */
@Composable
internal fun EvidencePhotoSection(
    evidencePhotos: List<IncidentPhotoEvidence>,
    activePhotoOperation: PhotoOperation?,
    errorMessage: String?,
    enabled: Boolean,
    onTakePhotoClick: () -> Unit,
    onPickPhotoClick: () -> Unit,
    onRemovePhoto: (IncidentPhotoEvidence) -> Unit,
    modifier: Modifier = Modifier
) {
    var previewFileName by rememberSaveable { mutableStateOf<String?>(null) }
    val previewPhoto = previewFileName?.let { fileName ->
        evidencePhotos.firstOrNull { it.fileName == fileName }
    }
    val isPhotoOperationActive = activePhotoOperation != null
    val hasPhotos = evidencePhotos.isNotEmpty()

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
            text = "Adjunta una o más fotografías del incidente. Las imágenes se " +
                "conservan solo mientras completas el reporte.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (hasPhotos) {
            Text(
                text = photoCountLabel(evidencePhotos.size),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(ReportPhotoTestTags.PHOTO_LIST),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(
                    items = evidencePhotos,
                    key = { _, photo -> photo.fileName }
                ) { index, photo ->
                    EvidencePhotoThumbnail(
                        photo = photo,
                        position = index + 1,
                        total = evidencePhotos.size,
                        enabled = enabled,
                        onRemove = { onRemovePhoto(photo) },
                        onOpenPreview = { previewFileName = photo.fileName }
                    )
                }
            }
            Text(
                text = "Toca una fotografía para verla en tamaño completo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        PhotoActionButton(
            label = if (hasPhotos) {
                "Tomar otra fotografía"
            } else {
                "Tomar fotografía"
            },
            icon = Icons.Filled.PhotoCamera,
            testTag = ReportPhotoTestTags.CAPTURE_BUTTON,
            isProcessing = activePhotoOperation == PhotoOperation.CAMERA,
            processingText = "Abriendo cámara...",
            processingTestTag = ReportPhotoTestTags.CAPTURING_INDICATOR,
            enabled = enabled && !isPhotoOperationActive,
            onClick = onTakePhotoClick
        )

        PhotoActionButton(
            label = if (hasPhotos) {
                "Elegir otra fotografía"
            } else {
                "Elegir de la galería"
            },
            icon = Icons.Filled.PhotoLibrary,
            testTag = ReportPhotoTestTags.GALLERY_BUTTON,
            isProcessing = activePhotoOperation == PhotoOperation.GALLERY,
            processingText = "Procesando fotografía...",
            processingTestTag = ReportPhotoTestTags.GALLERY_INDICATOR,
            enabled = enabled && !isPhotoOperationActive,
            onClick = onPickPhotoClick
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                modifier = Modifier.testTag(ReportPhotoTestTags.ERROR_MESSAGE),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }

    if (previewPhoto != null) {
        EvidencePhotoPreviewDialog(
            photo = previewPhoto,
            onDismiss = { previewFileName = null }
        )
    }
}

@Composable
private fun PhotoActionButton(
    label: String,
    icon: ImageVector,
    testTag: String,
    isProcessing: Boolean,
    processingText: String,
    processingTestTag: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        enabled = enabled
    ) {
        if (isProcessing) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(18.dp)
                    .testTag(processingTestTag),
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(processingText)
        } else {
            Icon(icon, contentDescription = null)
            Spacer(modifier = Modifier.size(8.dp))
            Text(label)
        }
    }
}

private fun photoCountLabel(count: Int): String = when (count) {
    1 -> "1 fotografía adjunta"
    else -> "$count fotografías adjuntas"
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
