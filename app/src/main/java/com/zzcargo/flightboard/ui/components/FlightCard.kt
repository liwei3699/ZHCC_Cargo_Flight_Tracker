package com.zzcargo.flightboard.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zzcargo.flightboard.data.model.Flight

@Composable
fun FlightCard(flight: Flight, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = flight.displayFlightNo.ifBlank { "—" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                FlightStatusChip(flight.displayStatus)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${flight.startStationCn.ifBlank { "—" }} → ${flight.terminalStationCn.ifBlank { "—" }}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = flight.execDate.ifBlank { "—" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TimeRow("计划", flight.scheTakeOffTime, flight.scheLandingTime)
            TimeRow("实际", flight.actlTakeOffTime, flight.actlLandingTime)
        }
    }
}

@Composable
private fun TimeRow(label: String, takeOff: String, landing: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(32.dp)
        )
        Text(
            text = "起飞 ${takeOff.ifBlank { "—" }}",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "降落 ${landing.ifBlank { "—" }}",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun FlightStatusChip(status: String) {
    val (background, foreground) = when (status) {
        "延误", "取消" -> Color(0xFFFFE0B2) to Color(0xFFB53D00)
        "到达" -> Color(0xFFD7F5EA) to Color(0xFF00695C)
        "起飞" -> Color(0xFFDCE9F7) to Color(0xFF0D47A1)
        else -> Color(0xFFE0E0E0) to Color(0xFF424242)
    }
    Surface(
        color = background,
        contentColor = foreground,
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Text(
            text = status.ifBlank { "—" },
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium
        )
    }
}
