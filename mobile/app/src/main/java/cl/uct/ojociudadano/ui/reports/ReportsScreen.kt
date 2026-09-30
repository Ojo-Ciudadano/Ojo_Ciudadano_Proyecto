package cl.uct.ojociudadano.ui.reports

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

import cl.uct.ojociudadano.OjoCiudadanoApplication
import cl.uct.ojociudadano.data.model.Category
import cl.uct.ojociudadano.data.model.Incident
import cl.uct.ojociudadano.data.model.IncidentLocation
import cl.uct.ojociudadano.data.model.IncidentStatus
import cl.uct.ojociudadano.ui.theme.OjoCiudadanoTheme


// ============================================================
// REPORTS SCREEN
// ============================================================

@Composable
fun ReportsScreen(
    modifier: Modifier = Modifier,

    onBack: () -> Unit = {},
    onCreateReportClick: () -> Unit = {},

    incidents: List<Incident>
) {
    var showFilters by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF0F0F0))
    ) {

        // ====================================================
        // HEADER
        // ====================================================

        HomeHeader(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.10f)
        )

        // ====================================================
        // CONTENIDO
        // ====================================================

        ReportsContent(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.80f),
            showFilters = showFilters,
            onToggleFilters = {
                showFilters = !showFilters
            },

            incidents = incidents
        )

        // ====================================================
        // FOOTER
        // ====================================================

        HomeFooter(
            onHomeClick = {
                // Pendiente
            },
            onReportClick = onCreateReportClick,
            onMapClick = {
                // Pendiente
            },
            onProfileClick = {
                // Pendiente
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.10f)
        )
    }
}


// ============================================================
// HEADER
// ============================================================

@Composable
private fun HomeHeader(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(Color(0xFF023889))
            .padding(
                horizontal = 16.dp,
                vertical = 8.dp
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Row(
            modifier = Modifier.weight(0.70f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Espacio reservado para el logo
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = Color.White,
                        shape = RoundedCornerShape(8.dp)
                    )
            )

            Text(
                text = "Ojo ciudadano",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Box(
            modifier = Modifier.weight(0.30f),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .background(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(25.dp)
                    )
                    .padding(
                        horizontal = 10.dp,
                        vertical = 5.dp
                    )
            ) {
                Text(
                    text = "Ciudadano",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}


// ============================================================
// CONTENIDO DE REPORTES
// ============================================================

@Composable
private fun ReportsContent(
    modifier: Modifier = Modifier,

    showFilters: Boolean,
    onToggleFilters: () -> Unit,

    incidents: List<Incident>
) {
    Column(
        modifier = modifier
            .padding(16.dp)
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // ====================================================
        // HEADER DE REPORTES
        // ====================================================

        ReportsHeader(
            showFilters = showFilters,
            onToggleFilters = onToggleFilters
        )

        // ====================================================
        // FILTROS
        // ====================================================

        AnimatedVisibility(
            visible = showFilters
        ) {
            ReportsFilters()
        }

        // ====================================================
        // REPORTES
        // ====================================================

        ReportsList( incidents = incidents )
    }
}


// ============================================================
// HEADER DE "MIS REPORTES"
// ============================================================

@Composable
private fun ReportsHeader(
    showFilters: Boolean,
    onToggleFilters: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().fillMaxHeight(0.10f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = 12.dp
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = "Mis reportes",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            TextButton(
                onClick = onToggleFilters,
                colors = ButtonDefaults.buttonColors( containerColor = Color(0xFFFF0000) ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (showFilters) {
                        "Ocultar"
                    } else {
                        "Filtros"
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}


// ============================================================
// FILTROS
// ============================================================

@Composable
private fun ReportsFilters() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Text(
                text = "Filtros",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            FilterField(
                label = "Categoría",
                value = "Seleccionar categoría"
            )

            FilterField(
                label = "Fecha",
                value = "Seleccionar período"
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Button(
                onClick = {
                    // Sin funcionalidad por ahora
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF0000),
                    disabledContainerColor = Color(0xFFFF0000)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Filtrar",
                    color = Color.White
                )
            }
        }
    }
}


// ============================================================
// CAMPO VISUAL DE FILTRO
// ============================================================

@Composable
private fun FilterField(
    label: String,
    value: String
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {

        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(
            modifier = Modifier.height(4.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .border(
                    width = 1.dp,
                    color = Color.Gray,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(
                    horizontal = 12.dp
                ),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = value,
                color = Color.Gray,
                fontSize = 14.sp
            )
        }
    }
}


// ============================================================
// LISTA DE REPORTES
// ============================================================

@Composable
private fun ReportsList(
    incidents: List<Incident>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors( containerColor = Color.White )
    ) {

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            items( incidents ) { incident ->
                ReportCard( incident = incident )
            }

        }
    }
}

// ============================================================
// ELEMENTO REUTILIZABLE DE REPORTE
// ============================================================
@Composable
private fun ReportCard(
    modifier: Modifier = Modifier,
    incident: Incident
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Color.Black,
                shape = RoundedCornerShape(10.dp)
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        // ====================================================
        // IMAGEN
        // ====================================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFE0E0E0)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Imagen",
                color = Color.Gray,
                fontSize = 14.sp
            )
        }

        // INFORMACIÓN
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {

            // TÍTULO
            Text(
                text = incident.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // DESCRIPCIÓN
            Text(
                text = incident.description,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis

            )

            // FECHA
            Text(
                text = incident.createdAt,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ============================================================
// FOOTER
// ============================================================

@Composable
private fun HomeFooter(
    onHomeClick: () -> Unit,
    onReportClick: () -> Unit,
    onMapClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(Color.White)
            .padding(
                horizontal = 8.dp,
                vertical = 4.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        HomeFooterItem(
            text = "Inicio",
            onClick = onHomeClick,
            modifier = Modifier.weight(1f)
        )

        HomeFooterItem(
            text = "Reportar",
            onClick = onReportClick,
            modifier = Modifier.weight(1f)
        )

        HomeFooterItem(
            text = "Mapa",
            onClick = onMapClick,
            modifier = Modifier.weight(1f)
        )

        HomeFooterItem(
            text = "Perfil",
            onClick = onProfileClick,
            modifier = Modifier.weight(1f)
        )
    }
}


// ============================================================
// ELEMENTO DEL FOOTER
// ============================================================

@Composable
private fun HomeFooterItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxHeight(),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color.Black
        ),
        contentPadding = PaddingValues(0.dp),
        shape = RoundedCornerShape(0.dp)
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        color = Color(0xFFE0E0E0),
                        shape = RoundedCornerShape(5.dp)
                    )
            )

            Text(
                text = text,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}


// ============================================================
// TEMPORAL
// ============================================================
@Composable
fun ReportsScreen(
    onBack: () -> Unit = {},
    onCreateReportClick: () -> Unit = {}
) {
    val application = LocalContext.current.applicationContext as OjoCiudadanoApplication
    val viewModel: ReportsViewModel = viewModel(
        factory = remember(application) {
            ReportsViewModelFactory(application.container.incidentRepository)
        }
    )

    val incidents by viewModel.incidents.collectAsState()

    ReportsScreen(
        onBack = onBack,
        onCreateReportClick = onCreateReportClick,
        incidents = incidents
    )
}


// ============================================================
// PREVIEW
// ============================================================

@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
private fun ReportsScreenPreview() {
    OjoCiudadanoTheme {
        ReportsScreen(
            incidents = listOf(
                Incident(
                    id = "40000000-0000-0000-0000-000000000001",
                    title = "Luminaria apagada en calzada principal",
                    description = "El poste de luz no enciende desde hace 3 días, dejando la cuadra muy oscura.",
                    category = Category(
                        id = "20000000-0000-0000-0000-000000000001",
                        name = "Alumbrado público",
                        description = "Fallas de alumbrado en calles y espacios públicos",
                        basePriority = 2,
                        isCriticalEmergency = false,
                        officialContactChannel = null
                    ),
                    status = IncidentStatus(
                        id = "10000000-0000-0000-0000-000000000001",
                        name = "Nuevo",
                        order = 1
                    ),
                    location = IncidentLocation(
                        id = "30000000-0000-0000-0000-000000000001",
                        latitude = -33.4372,
                        longitude = -70.6506,
                        referenceAddress = "Av. Libertador Bernardo O'Higgins 1234",
                        commune = "Santiago",
                        region = "Región Metropolitana",
                        sector = "Centro"
                    ),
                    reporterUserId = "a0000000-0000-0000-0000-000000000001",
                    priorityLevel = null,
                    createdAt = "2026-09-28T10:15:00Z",
                    updatedAt = "2026-09-28T10:15:00Z"
                ),
                Incident(
                    id = "40000000-0000-0000-0000-000000000003",
                    title = "Incendio en terreno baldío",
                    description = "Fuego sin control propagándose rápidamente hacia viviendas colindantes.",
                    category = Category(
                        id = "20000000-0000-0000-0000-000000000008",
                        name = "Incendio",
                        description = "Incendios estructurales, forestales o pastizales",
                        basePriority = 5,
                        isCriticalEmergency = true,
                        officialContactChannel = "Bomberos 132"
                    ),
                    status = IncidentStatus(
                        id = "10000000-0000-0000-0000-000000000003",
                        name = "En atención",
                        order = 3
                    ),
                    location = IncidentLocation(
                        id = "30000000-0000-0000-0000-000000000002",
                        latitude = -33.4261,
                        longitude = -70.6125,
                        referenceAddress = "Av. Providencia con Pedro de Valdivia",
                        commune = "Providencia",
                        region = "Región Metropolitana",
                        sector = "Pedro de Valdivia Norte"
                    ),
                    reporterUserId = "a0000000-0000-0000-0000-000000000002",
                    priorityLevel = 5,
                    createdAt = "2026-09-29T08:00:00Z",
                    updatedAt = "2026-09-29T08:05:00Z"
                )
            )
        )
    }
}