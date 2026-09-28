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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.uct.ojociudadano.ui.theme.OjoCiudadanoTheme


// ============================================================
// REPORTS SCREEN
// ============================================================

@Composable
fun ReportsScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onCreateReportClick: () -> Unit = {}
) {
    var showFilters by remember {
        mutableStateOf(false)
    }

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
            }
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
    onToggleFilters: () -> Unit
) {
    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
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

        ReportsList()
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
        modifier = Modifier.fillMaxWidth(),
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
private fun ReportsList() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {

            Text(
                text = "Reportes",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text = "Aquí se mostrarán los reportes realizados.",
                fontSize = 14.sp,
                color = Color.Gray
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
// PREVIEW
// ============================================================

@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
private fun ReportsScreenPreview() {
    OjoCiudadanoTheme {
        ReportsScreen()
    }
}