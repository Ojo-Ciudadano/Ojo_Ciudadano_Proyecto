package cl.uct.ojociudadano.ui.home

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.uct.ojociudadano.ui.theme.OjoCiudadanoTheme

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onReportsClick: () -> Unit = {},
    onCreateReportClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {}
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
                .weight(0.80f)
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
                value = "3",
                title = "En revisión",
                description = "Pendientes",
                modifier = Modifier.weight(1f)
            )

            ReportStatusCard(
                value = "2",
                title = "En atención",
                description = "Atendidos",
                modifier = Modifier.weight(1f)
            )

            ReportStatusCard(
                value = "8",
                title = "Aprobados",
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
                            .fillMaxSize(),
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.75f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    RecentReportItem(
                        onReportsClick = onReportsClick
                    )

                    RecentReportItem(
                        onReportsClick = onReportsClick
                    )
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
            // Mapa futuro
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
    onReportsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
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
                text = "Incedio", // report.title
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 1.sp,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Se esta quemando el bosque", // report.description
                fontSize = 10.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 2,
                lineHeight = 12.sp,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Hoy", // report.date
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
// RENDER //
// ========================= //
@Preview(
    showBackground = true,
    showSystemUi = true
)

@Composable
private fun HomeScreenPreview() {
    OjoCiudadanoTheme {
        HomeScreen()
    }
}