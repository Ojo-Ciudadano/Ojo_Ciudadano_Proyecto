package cl.uct.ojociudadano.ui.reports.evidence

import coil3.compose.AsyncImagePainter

/** Estado visual de una fotografía de evidencia. */
internal sealed interface EvidencePhotoUiState {

    data object Loading : EvidencePhotoUiState

    data object Loaded : EvidencePhotoUiState

    data class Error(val message: String) : EvidencePhotoUiState
}

internal fun AsyncImagePainter.State.toEvidencePhotoUiState(): EvidencePhotoUiState {
    return when (this) {
        is AsyncImagePainter.State.Success -> EvidencePhotoUiState.Loaded

        is AsyncImagePainter.State.Error -> EvidencePhotoUiState.Error(
            evidencePhotoErrorMessage(result.throwable)
        )

        AsyncImagePainter.State.Empty, is AsyncImagePainter.State.Loading ->
            EvidencePhotoUiState.Loading
    }
}
