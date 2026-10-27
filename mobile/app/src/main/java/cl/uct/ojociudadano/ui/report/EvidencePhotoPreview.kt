package cl.uct.ojociudadano.ui.report

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import cl.uct.ojociudadano.data.model.IncidentPhotoEvidence

private val ThumbnailSize = 112.dp

/**
 * Miniatura de una fotografía agregada al formulario.
 *
 * La imagen se carga desde su URI local. Si el archivo ya no está disponible o
 * no puede decodificarse, se muestra un marcador visual en lugar de propagar
 * el error y la miniatura sigue siendo eliminable.
 */
@Composable
internal fun EvidencePhotoThumbnail(
    photo: IncidentPhotoEvidence,
    position: Int,
    total: Int,
    enabled: Boolean,
    onRemove: () -> Unit,
    onOpenPreview: () -> Unit,
    modifier: Modifier = Modifier
) {
    var loaded by remember(photo.localUri) { mutableStateOf(false) }
    var loadFailed by remember(photo.localUri) { mutableStateOf(false) }

    Card(
        modifier = modifier
            .size(ThumbnailSize)
            .testTag(ReportPhotoTestTags.thumbnail(photo.fileName)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = Uri.parse(photo.localUri),
                contentDescription = "Fotografía $position de $total",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (loaded) {
                            Modifier.testTag(ReportPhotoTestTags.loaded(photo.fileName))
                        } else {
                            Modifier
                        }
                    ),
                onSuccess = {
                    loaded = true
                    loadFailed = false
                },
                onError = {
                    loaded = false
                    loadFailed = true
                }
            )

            if (loadFailed) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .testTag(ReportPhotoTestTags.previewError(photo.fileName)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.BrokenImage,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "No disponible",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(
                        onClickLabel = "Ver fotografía $position de $total",
                        onClick = onOpenPreview
                    )
            )

            IconButton(
                onClick = onRemove,
                enabled = enabled,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                    .testTag(ReportPhotoTestTags.removeButton(photo.fileName))
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Eliminar fotografía $position",
                    tint = Color.White
                )
            }
        }
    }
}

/**
 * Vista previa a pantalla completa de una fotografía.
 *
 * Se apoya en el mismo URI local que la miniatura. Un fallo de carga se
 * representa dentro del diálogo para que la pantalla de creación no se cierre.
 */
@Composable
internal fun EvidencePhotoPreviewDialog(
    photo: IncidentPhotoEvidence,
    onDismiss: () -> Unit
) {
    var loadFailed by remember(photo.localUri) { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag(ReportPhotoTestTags.PREVIEW_DIALOG),
            color = Color.Black
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = Uri.parse(photo.localUri),
                    contentDescription = "Vista previa de la fotografía",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    onSuccess = { loadFailed = false },
                    onError = { loadFailed = true }
                )

                if (loadFailed) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.BrokenImage,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Text(
                            text = "No fue posible mostrar la fotografía",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .testTag(ReportPhotoTestTags.CLOSE_PREVIEW_BUTTON)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Cerrar vista previa",
                        tint = Color.White
                    )
                }

                Text(
                    text = formatEvidencePhotoSize(photo.sizeBytes),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
