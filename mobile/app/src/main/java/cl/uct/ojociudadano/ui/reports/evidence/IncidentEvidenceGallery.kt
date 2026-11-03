package cl.uct.ojociudadano.ui.reports.evidence

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cl.uct.ojociudadano.domain.model.Evidence
import coil3.compose.AsyncImage

private val CompactPhotoSize = 112.dp
private val DetailPhotoHeight = 220.dp
private val PhotoCornerRadius = 12.dp

/**
 * Galería reutilizable de evidencias fotográficas de un incidente.
 *
 * Solo recibe evidencias ya cargadas por la capa de datos. Coil se encarga de
 * descargarlas mediante [cl.uct.ojociudadano.data.evidence.EvidenceImageFetcher];
 * este componente únicamente representa carga, éxito o error por fotografía.
 *
 * @param compact cuando es `true` muestra miniaturas para el listado; en caso
 * contrario usa un formato más amplio para el detalle.
 * @param showEmptyMessage muestra un mensaje cuando no hay fotografías.
 */
@Composable
fun IncidentEvidenceGallery(
    evidences: List<Evidence>,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    showEmptyMessage: Boolean = false
) {
    val photos = remember(evidences) {
        evidences.filter { it.mimeType.startsWith("image/", ignoreCase = true) }
    }

    if (photos.isEmpty()) {
        if (showEmptyMessage) {
            Text(
                text = "Este incidente no registra evidencias fotográficas.",
                modifier = modifier.testTag(IncidentEvidenceTestTags.EMPTY),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyRow(
        modifier = modifier.testTag(IncidentEvidenceTestTags.GALLERY),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(photos, key = { _, evidence -> evidence.id }) { index, evidence ->
            IncidentEvidencePhoto(
                evidence = evidence,
                position = index + 1,
                total = photos.size,
                compact = compact,
                modifier = if (compact) {
                    Modifier.size(CompactPhotoSize)
                } else {
                    Modifier.fillParentMaxWidth().height(DetailPhotoHeight)
                }
            )
        }
    }
}

@Composable
private fun IncidentEvidencePhoto(
    evidence: Evidence,
    position: Int,
    total: Int,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    var retryToken by remember(evidence.id) { mutableIntStateOf(0) }
    var uiState by remember(evidence.id) {
        mutableStateOf<EvidencePhotoUiState>(EvidencePhotoUiState.Loading)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(PhotoCornerRadius))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .testTag(IncidentEvidenceTestTags.photo(evidence.id))
    ) {
        key(retryToken) {
            AsyncImage(
                model = evidence,
                contentDescription = "Evidencia fotográfica $position de $total",
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (uiState == EvidencePhotoUiState.Loaded) {
                            Modifier.testTag(IncidentEvidenceTestTags.loaded(evidence.id))
                        } else {
                            Modifier
                        }
                    ),
                contentScale = if (compact) ContentScale.Crop else ContentScale.Fit,
                onState = { state -> uiState = state.toEvidencePhotoUiState() }
            )
        }

        when (val state = uiState) {
            EvidencePhotoUiState.Loading -> EvidencePhotoLoading(
                evidenceId = evidence.id,
                compact = compact
            )

            EvidencePhotoUiState.Loaded -> Unit

            is EvidencePhotoUiState.Error -> EvidencePhotoError(
                evidenceId = evidence.id,
                message = state.message,
                compact = compact,
                onRetry = {
                    uiState = EvidencePhotoUiState.Loading
                    retryToken += 1
                }
            )
        }
    }
}

@Composable
private fun EvidencePhotoLoading(
    evidenceId: String,
    compact: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag(IncidentEvidenceTestTags.loading(evidenceId)),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(if (compact) 24.dp else 32.dp),
            strokeWidth = 2.dp
        )
    }
}

@Composable
private fun EvidencePhotoError(
    evidenceId: String,
    message: String,
    compact: Boolean,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(if (compact) 4.dp else 16.dp)
            .testTag(IncidentEvidenceTestTags.error(evidenceId)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.BrokenImage,
            contentDescription = null,
            modifier = Modifier.size(if (compact) 20.dp else 32.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = if (compact) "No disponible" else message,
            modifier = if (compact) Modifier else Modifier.padding(top = 8.dp),
            style = if (compact) {
                MaterialTheme.typography.labelSmall
            } else {
                MaterialTheme.typography.bodyMedium
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        if (compact) {
            TextButton(
                onClick = onRetry,
                modifier = Modifier.testTag(IncidentEvidenceTestTags.retry(evidenceId)),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("Reintentar", style = MaterialTheme.typography.labelSmall)
            }
        } else {
            OutlinedButton(
                onClick = onRetry,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .testTag(IncidentEvidenceTestTags.retry(evidenceId))
            ) {
                Text("Reintentar")
            }
        }
    }
}
