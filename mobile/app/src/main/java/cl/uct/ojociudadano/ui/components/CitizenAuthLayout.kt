package cl.uct.ojociudadano.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Native Compose animation honors the system animator duration scale, including zero. */
@Composable
fun CitizenAuthLayout(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    Column(modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        CitizenHeader("Ojo Ciudadano", "Tu ciudad, tu reporte")
        AnimatedVisibility(visible, enter = fadeIn(tween(180))) {
            Card(Modifier.padding(16.dp).widthIn(max = 480.dp).fillMaxWidth().animateContentSize(tween(180)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
            }
        }
    }
}
