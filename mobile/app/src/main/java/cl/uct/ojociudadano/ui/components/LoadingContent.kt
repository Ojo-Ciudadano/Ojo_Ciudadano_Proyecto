package cl.uct.ojociudadano.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Centra la carga en el espacio asignado por la pantalla, tarjeta o diálogo. */
@Composable
fun LoadingContent(
    modifier: Modifier = Modifier,
    message: String? = null,
    indicatorModifier: Modifier = Modifier
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(indicatorModifier.size(28.dp))
            message?.let { Text(it, textAlign = TextAlign.Center) }
        }
    }
}
