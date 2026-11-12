package com.zzcargo.flightboard.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zzcargo.flightboard.data.model.Flight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailSheet(flight: Flight, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = flight.displayFlightNo.ifBlank { "—" },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(12.dp))
                FlightStatusChip(flight.displayStatus)
            }
            Text(
                text = "${flight.startStationCn.ifBlank { "—" }} → ${flight.terminalStationCn.ifBlank { "—" }}",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = flight.execDate.ifBlank { "—" },
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Section("时间") {
                InfoRow("计划起飞", flight.scheTakeOffTime)
                InfoRow("实际起飞", flight.actlTakeOffTime)
                InfoRow("计划降落", flight.scheLandingTime)
                InfoRow("实际降落", flight.actlLandingTime)
            }
            Section("航线") {
                InfoRow("完整航线", flight.airrouteCn)
                InfoRow("IATA", flight.airrouteIataCd)
                InfoRow("ICAO", flight.airrouteIcaoCd)
                InfoRow("国内/国际", flight.regionLabel)
            }
            Section("运行") {
                InfoRow("机型", flight.craftType)
                InfoRow("机号", flight.craftNo)
                InfoRow("航站楼", flight.terminal)
                InfoRow("机位", flight.stand)
                InfoRow("行李转盘", flight.carousel)
                InfoRow("代理", flight.agency)
                InfoRow("航班任务", flight.flightTaskCn)
            }
            Section("异常") {
                InfoRow("异常状态", flight.abnormalStateCn.ifBlank { "无" })
                InfoRow("延误原因", flight.absreasonCn.ifBlank { "无" })
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        content()
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(
            text = label,
            modifier = Modifier.width(88.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value.ifBlank { "—" },
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}
