package cl.uct.ojociudadano.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.uct.ojociudadano.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitizenHeader(title: String, subtitle: String = "Ojo Ciudadano", onBack: (() -> Unit)? = null,
                  actions: @Composable RowScope.() -> Unit = {}) {
    TopAppBar(
        title = {
            Column {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(subtitle, style = MaterialTheme.typography.labelMedium)
            }
        },
        navigationIcon = {
            if (onBack != null) IconButton(onClick = onBack, modifier = Modifier.size(Spacing.touchTarget)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
            } else Icon(Icons.Default.Visibility, null, Modifier.padding(12.dp).size(24.dp))
        },
        actions = actions,
        windowInsets = WindowInsets(0, 0, 0, 0),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

@Composable
fun CitizenMessage(message: String, modifier: Modifier = Modifier, isError: Boolean = false) {
    Surface(modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
        color = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer) {
        Text(message, Modifier.padding(Spacing.medium),
            color = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
            style = MaterialTheme.typography.bodyMedium)
    }
}
