package cl.uct.ojociudadano.ui.home

import android.view.ViewDebug
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
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.mutableStateListOf

import cl.uct.ojociudadano.OjoCiudadanoApplication
import cl.uct.ojociudadano.data.model.Category
import cl.uct.ojociudadano.data.model.Incident
import cl.uct.ojociudadano.data.model.IncidentLocation
import cl.uct.ojociudadano.data.model.IncidentStatus
import cl.uct.ojociudadano.ui.theme.OjoCiudadanoTheme

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,

    onReportsClick: () -> Unit = {},
    onCreateReportClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},

    recentIncidents: List<Incident>,
    incidentsUnderReview: Int,
    incidentsInViewOf: Int,
    solvedIncidents: Int
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF0F0F0))
    ) {
        // HEADER (10% de la pantalla)
        HomeHeader(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.10f)
        )

        // CONTENIDO (80% de la pantalla)
        HomeContent(
            onCreateReportClick = onCreateReportClick,
            onReportsClick = onReportsClick,

            modifier = Modifier
                .fillMaxWidth()
                .weight(0.80f),

            recentIncidents = recentIncidents,
            incidentsUnderReview = incidentsUnderReview,
            incidentsInViewOf = incidentsInViewOf,
            solvedIncidents = solvedIncidents
        )

        // FOOTER (10% de la pantalla)
        HomeFooter(
            onHomeClick = {},    // Pendiente
            onReportClick = onCreateReportClick,
            onMapClick = {},     // Pendiente
            onProfileClick = {}, // Pendiente
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.10f)
        )
    }
}

// ========================= //
// HEADER //
// ========================= //

@Composable
private fun HomeHeader( modifier: Modifier = Modifier ) {
    Row(
        modifier = modifier
            .background(Color(0xFF023889))
            .padding(horizontal = 16.dp, vertical = 8.dp),
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
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "Ciudadano", // Fijo temporalmente
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ========================= //
// CONTENT //
// ========================= //
@Composable
private fun HomeContent(
    onCreateReportClick: () -> Unit,
    onReportsClick: () -> Unit,

    recentIncidents: List<Incident>,
    incidentsUnderReview: Int,
    incidentsInViewOf: Int,
    solvedIncidents: Int,

    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // BIENVENIDA
        Card(
            modifier = Modifier
                .fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 5.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "¡Bienvenido!",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Gracias por ayudar a mejorar nuestra ciudad.",
                    fontSize = 14.sp
                )
            }
        }

        // NUEVO REPORTE
        Button(
            onClick = onCreateReportClick,
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.05f),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
        ) {
            Text(
                text = "Nuevo reporte",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // ESTADOS DE REPORTES
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ReportStatusCard(
                value = incidentsUnderReview.toString(),
                title = "En revisión",
                description = "Pendientes",
                modifier = Modifier.weight(1f)
            )

            ReportStatusCard(
                value = incidentsInViewOf.toString(),
                title = "En atención",
                description = "Atendidos",
                modifier = Modifier.weight(1f)
            )

            ReportStatusCard(
                value = solvedIncidents.toString(),
                title = "Cerrados",
                description = "Aprobados",
                modifier = Modifier.weight(1f)
            )
        }

        // REPORTES RECIENTES
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.35f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            ) {

                // Header - 15% del alto disponible
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.15f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Reportes recientes",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    TextButton(
                        modifier = Modifier
                            .fillMaxHeight(),
                        onClick = onReportsClick
                    ) {
                        Text(
                            text = "Ver todos",
                            color = Color(0xFF0000FF),
                            fontSize = 14.sp
                        )
                    }
                }

                // Content - 75% del alto disponible
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.85f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    items(recentIncidents) { incident ->
                        RecentReportItem(
                            incidentTitle = incident.title,
                            incidentDescription = incident.description,
                            incidentDate = incident.createdAt,
                            onReportsClick = onReportsClick
                        )
                    }
                }
            }
        }

        // MAPA
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.30f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            // Mapa
        }
    }
}

// CAJAS DE ESTADOS: PENDIENTES - ANTENDIDOS - APROVADOS
@Composable
private fun ReportStatusCard(
    value: String,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(
                            color = Color(0xFFE0E0E0),
                            shape = RoundedCornerShape(4.dp)
                        )
                )

                Text(
                    text = value, // Valor fijo temporalmente
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = description,
                fontSize = 10.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF5D5D5D),
                textAlign = TextAlign.Center
            )
        }
    }
}


// REPORTES RECIENTES
@Composable
private fun RecentReportItem(
    incidentTitle: String,
    incidentDescription: String,
    incidentDate: String,

    onReportsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .border(
                width = 1.dp,
                color = Color.LightGray,
                shape = RoundedCornerShape(size = 5.dp)
            )
            .padding(5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        // Image
        Box(
            modifier = Modifier
                .weight(0.20f)
                .wrapContentWidth(Alignment.Start),
            contentAlignment = Alignment.Center
        ) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(Color.LightGray, shape = RoundedCornerShape(5.dp))
            )
        }

        // Description
        Column(
            modifier = Modifier
                .weight(0.40f)
                .wrapContentWidth(Alignment.Start),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = incidentTitle,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                lineHeight = 1.sp,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = incidentDescription,
                fontSize = 10.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 2,
                lineHeight = 12.sp,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = incidentDate,
                fontSize = 10.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                lineHeight = 1.sp,
                overflow = TextOverflow.Ellipsis,
                color = Color.Gray
            )
        }

        // Status
        Row(
            modifier = Modifier
                .weight(0.20f)
                .wrapContentWidth(Alignment.Start),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Pendiente" ,// report.status
                modifier = Modifier
                    .background(
                        color = Color(0xEDFDA836),
                        shape = RoundedCornerShape(5.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 1.dp),
                color = Color.Black,
                fontWeight = FontWeight.Normal,
                fontSize = 10.sp
            )
        }

        // Button
        Box(
            modifier = Modifier
                .weight(0.10f)
                .wrapContentWidth(Alignment.Start),
            contentAlignment = Alignment.Center
        ) {
            TextButton( onClick = onReportsClick ) {
                Text(
                    text = ">",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
        }
    }
}


// MAPA: EN UN FUTURO

// ========================= //
// FOOTER //
// ========================= //
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
            .padding(horizontal = 8.dp, vertical = 4.dp),
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

// ELEMENTOS DEL FOOTER
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

            // Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = text,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}


// ========================= //
// TEMPORAL //
// ========================= //
@Composable
fun HomeScreen(
    onReportsClick: () -> Unit = {},
    onCreateReportClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {}
) {
    val application = LocalContext.current.applicationContext as OjoCiudadanoApplication
    val viewModel: HomeViewModel = viewModel(
        factory = remember(application) {
            HomeViewModelFactory(application.container.incidentRepository)
        }
    )

    val incidents by viewModel.incidents.collectAsState()
    val recentIncidents = incidents
        .sortedByDescending{ it.createdAt }
        .take(2)
    val incidentsUnderReview = incidents.count{ incident ->
        incident.status.name == "En revisión"
    }
    val incidentsInViewOf = incidents.count{ incident ->
        incident.status.name == "En atención"
    }
    val solvedIncidents = incidents.count{ incident ->
        incident.status.name == "Cerrado"
    }

    HomeScreen(
        onReportsClick = onReportsClick,
        onCreateReportClick = onCreateReportClick,
        onNotificationsClick = onNotificationsClick,

        recentIncidents = recentIncidents,
        incidentsUnderReview = incidentsUnderReview,
        incidentsInViewOf = incidentsInViewOf,
        solvedIncidents = solvedIncidents
    )
}

// ========================= //
// RENDER PREVIEW//
// ========================= //
@Preview(
    showBackground = true,
    showSystemUi = true
)

@Composable
private fun HomeScreenPreview() {
    OjoCiudadanoTheme {
        HomeScreen(
            recentIncidents = listOf(
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
            ),
            incidentsUnderReview = 5,
            incidentsInViewOf = 6,
            solvedIncidents = 7
        )
    }
}