package cl.uct.ojociudadano.ui.map

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * Botón flotante que solicita centrar el mapa en la ubicación actual.
 *
 * Mientras la lectura GPS está en curso muestra un indicador y no dispara
 * solicitudes adicionales. La comprobación de permisos queda en la pantalla,
 * que reutiliza el flujo de ubicación existente.
 */
@Composable
internal fun MapCenterLocationFab(
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = { if (!isLoading) onClick() },
        modifier = modifier.testTag(MapTestTags.CENTER_LOCATION_FAB),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp
            )
        } else {
            Icon(
                imageVector = Icons.Filled.MyLocation,
                contentDescription = "Centrar en mi ubicación"
            )
        }
    }
}
