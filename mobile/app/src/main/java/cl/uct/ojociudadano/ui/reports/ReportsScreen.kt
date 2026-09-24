package cl.uct.ojociudadano.ui.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cl.uct.ojociudadano.ui.theme.OjoCiudadanoTheme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

// =========================
// Simulated reports wrapper
// =========================
@Composable
fun ReportsScreen( modifier: Modifier = Modifier, onBack: () -> Unit = {}) {
    // Vars
    val reports = listOf(
        ReportsFromState(
            title = "Incendio forestal",
            description = "Ruta S-93, Carahue",
            date = "27 may, 2026 - 14:32",
            status = "en revisión"
        ),
        ReportsFromState(
            title = "Semáforo apagado",
            description = "Diego portales, Temuco",
            date = "27 may, 2026 - 07:32",
            status = "pendiente"
        )
    )

    // Interface ("HTML")
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {

        // Header report ***********
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Mis reportes",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            TextButton(
                onClick = {},
                colors = ButtonDefaults.textButtonColors(
                    containerColor = Color(0x00FFFFFF)
                )
            ) {
                Text(
                    text = "Ver todos",
                    color = Color(0xFF0000FF),
                )
            }
        }

        // Header END
        Spacer(
            modifier = Modifier.height(16.dp)
        )

        // Report list maker ***********
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(reports) { report ->

                ReportItem(
                    report = report,
                    onClick = { }
                )
            }
        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        // Close button
        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors()
        ) {
            Text("Volver")
        }
    }
}


// =========================
// Report section
// =========================
@Composable
private fun ReportItem ( report: ReportsFromState, onClick: () -> Unit ) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Color.LightGray,
                shape = RoundedCornerShape(size = 5.dp)
            )
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Image
        Box(
            modifier = Modifier
                .weight(0.25f)
                .wrapContentWidth(Alignment.Start),
            contentAlignment = Alignment.Center
        ) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(Color.LightGray, shape = RoundedCornerShape(5.dp))
            )
        }

        Spacer(modifier = Modifier.width(5.dp))

        // Description
        Column(
            modifier = Modifier
                .weight(0.40f)
                .wrapContentWidth(Alignment.Start)
        ) {
            Text(
                text = report.title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
            Text(
                text = report.description,
                fontSize = 12.sp,
                maxLines = 1,
            )
            Text(
                text = report.date,
                fontSize = 12.sp,
                color = Color.Gray
            )
        }

        Spacer(modifier = Modifier.width(5.dp))

        // Status
        Box(
            modifier = Modifier
                .weight(0.25f)
                .wrapContentWidth(Alignment.Start),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = report.status,
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

        Spacer(modifier = Modifier.width(5.dp))

        // Button
        Box(
            modifier = Modifier
                .weight(0.10f)
                .wrapContentWidth(Alignment.Start),
            contentAlignment = Alignment.Center
        ) {
            TextButton( onClick = onClick ) {
                Text(
                    text = ">",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
private fun ReportsScreenPreview() {
    OjoCiudadanoTheme {
        ReportsScreen()
    }
}