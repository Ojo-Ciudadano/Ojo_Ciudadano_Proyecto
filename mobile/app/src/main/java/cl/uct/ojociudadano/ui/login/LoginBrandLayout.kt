package cl.uct.ojociudadano.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.uct.ojociudadano.ui.theme.LocalDarkTheme
import cl.uct.ojociudadano.ui.theme.Spacing

/** Encabezado de marca y formulario desplazable, independientes de la autenticación. */
@Composable
internal fun LoginBrandLayout(
    modifier: Modifier = Modifier,
    logo: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val darkTheme = LocalDarkTheme.current
    val colors = MaterialTheme.colorScheme
    BoxWithConstraints(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).imePadding()) {
        val compact = maxHeight < 560.dp
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (darkTheme) colors.primaryContainer else colors.primary,
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Column(
                    Modifier.padding(horizontal = 32.dp, vertical = if (compact) 16.dp else 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    logo()
                    Text(
                        "Tu ciudad, tu reporte",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            Card(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)
                    .widthIn(max = 480.dp).fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    Modifier.padding(if (compact) 20.dp else 24.dp),
                    verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                    content = content
                )
            }
        }
    }
}
