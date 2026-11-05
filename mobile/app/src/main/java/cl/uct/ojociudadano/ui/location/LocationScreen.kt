package cl.uct.ojociudadano.ui.location

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cl.uct.ojociudadano.domain.model.LocationResult
import cl.uct.ojociudadano.ui.theme.OjoCiudadanoTheme

@Composable
fun LocationScreen(viewModel: LocationViewModel) {
    val locationState by viewModel.locationState.collectAsState()

    LocationContent(
        locationState = locationState,
        onFetchLocation = { viewModel.fetchCurrentLocation() }
    )
}

@Composable
fun LocationContent(
    locationState: LocationResult,
    onFetchLocation: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (val state = locationState) {
            is LocationResult.Success -> {
                Text(text = "Lat: ${state.latitude}, Lng: ${state.longitude}")
            }
            is LocationResult.MissingPermissions -> {
                Text(text = "Se requieren permisos de ubicación (Gestionados en Issue 81).")
            }
            is LocationResult.NotAvailable -> {
                Text(text = "Ubicación no disponible en este momento.")
            }
            is LocationResult.Error -> {
                Text(text = "Error: ${state.message}")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onFetchLocation) {
            Text("Obtener Ubicación GPS")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LocationScreenPreview() {
    OjoCiudadanoTheme {
        LocationContent(
            locationState = LocationResult.Success(-38.7396, -72.5984),
            onFetchLocation = {}
        )
    }
}