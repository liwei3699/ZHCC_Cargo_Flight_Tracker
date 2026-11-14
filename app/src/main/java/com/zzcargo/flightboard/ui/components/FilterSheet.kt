package com.zzcargo.flightboard.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zzcargo.flightboard.data.model.Airline
import com.zzcargo.flightboard.data.model.Flight
import com.zzcargo.flightboard.domain.AirlineLabels
import com.zzcargo.flightboard.domain.CraftTypes
import com.zzcargo.flightboard.domain.FilterState
import com.zzcargo.flightboard.domain.FlightFilter
import java.text.Collator
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSheet(
    initial: FilterState,
    allFlights: List<Flight>,
    airlines: List<Airline>,
    onDismiss: () -> Unit,
    onApply: (FilterState) -> Unit,
    onResetAll: () -> Unit
) {
    val dates = remember(allFlights) {
        allFlights.map { it.execDate }.filter { it.isNotBlank() }.distinct().sorted()
    }
    val collator = remember { Collator.getInstance(Locale.CHINA) }
    val agencies = remember(allFlights) {
        allFlights.map { it.agency }.filter { it.isNotBlank() }.distinct()
            .sortedWith(Comparator { a, b -> collator.compare(a, b) })
    }
    val craftTypes = remember(allFlights) {
        allFlights.map { it.craftType }.filter { it.isNotBlank() }.distinct()
            .sortedWith(Comparator { a, b -> collator.compare(a, b) })
    }
    val craftFamilies = remember(craftTypes) {
        craftTypes.map(CraftTypes::familyOf).distinct()
            .sortedWith(Comparator { a, b -> collator.compare(a, b) })
    }
    val craftTypesByFamily = remember(craftTypes) {
        craftTypes.groupBy(CraftTypes::familyOf)
    }
    val craftSubtypeLabels = remember(craftTypes) {
        craftTypes.associateWith(CraftTypes::shortLabel)
    }
    val airlineCodes = remember(allFlights) {
        allFlights.map { it.airlinesIataCd }.filter { it.isNotBlank() }.distinct()
            .sortedWith(Comparator { a, b -> collator.compare(a, b) })
    }
    val officialAirlineNames = remember(airlines) {
        airlines.filter { it.code.isNotBlank() }.associate { it.code to it.name }
    }
    val airlineLabels = remember(airlineCodes, officialAirlineNames) {
        airlineCodes.associateWith { code ->
            AirlineLabels.label(code, officialAirlineNames[code])
        }
    }
    var draft by remember {
        mutableStateOf(
            FilterState(
                flightNoQuery = initial.flightNoQuery,
                execDate = initial.execDate,
                ioAttr = initial.ioAttr,
                agency = initial.agency,
                airline = initial.airline,
                craftFamily = initial.craftFamily,
                craftType = initial.craftType,
                startStationQuery = initial.startStationQuery,
                terminalStationQuery = initial.terminalStationQuery,
                statusQuick = initial.statusQuick,
                keyword = initial.keyword
            )
        )
    }
    val subtypeOptions = if (draft.craftFamily == null) {
        craftTypes
    } else {
        craftTypesByFamily[draft.craftFamily].orEmpty()
    }
    val matchCount = remember(draft, allFlights) {
        FlightFilter.apply(allFlights, draft).size
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("筛选航班", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = draft.flightNoQuery,
                onValueChange = { draft = draft.copy(flightNoQuery = it) },
                label = { Text("航班号") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            DropdownField(
                label = "航班日期",
                selectedValue = draft.execDate,
                options = dates,
                onSelect = { draft = draft.copy(execDate = it) }
            )
            DropdownField(
                label = "进出港",
                selectedValue = draft.ioAttr?.let { if (it == "A") "到达" else "起飞" },
                options = listOf("A", "D"),
                labels = mapOf("A" to "到达", "D" to "起飞"),
                onSelect = { draft = draft.copy(ioAttr = it) }
            )
            DropdownField(
                label = "代理",
                selectedValue = draft.agency,
                options = agencies,
                onSelect = { draft = draft.copy(agency = it) }
            )
            DropdownField(
                label = "航司",
                selectedValue = draft.airline,
                options = airlineCodes,
                labels = airlineLabels,
                onSelect = { draft = draft.copy(airline = it) }
            )
            DropdownField(
                label = "机型大类",
                selectedValue = draft.craftFamily,
                options = craftFamilies,
                onSelect = { family ->
                    val keepType = family != null &&
                        draft.craftType?.let { CraftTypes.familyOf(it) == family } == true
                    draft = draft.copy(
                        craftFamily = family,
                        craftType = if (keepType) draft.craftType else null
                    )
                }
            )
            DropdownField(
                label = "机型小类",
                selectedValue = draft.craftType,
                options = subtypeOptions,
                labels = craftSubtypeLabels,
                onSelect = { type ->
                    draft = draft.copy(
                        craftType = type,
                        craftFamily = type?.let(CraftTypes::familyOf) ?: draft.craftFamily
                    )
                }
            )
            OutlinedTextField(
                value = draft.startStationQuery,
                onValueChange = { draft = draft.copy(startStationQuery = it) },
                label = { Text("始发站") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = draft.terminalStationQuery,
                onValueChange = { draft = draft.copy(terminalStationQuery = it) },
                label = { Text("目的站") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Text("命中 $matchCount 条", style = MaterialTheme.typography.bodyMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(onClick = onResetAll, modifier = Modifier.weight(1f)) {
                    Text("重置")
                }
                Button(onClick = { onApply(draft) }, modifier = Modifier.weight(1f)) {
                    Text("应用")
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DropdownField(
    label: String,
    selectedValue: String?,
    options: List<String>,
    labels: Map<String, String> = emptyMap(),
    onSelect: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = selectedValue?.let { labels[it] ?: it } ?: label,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Start
            )
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            DropdownMenuItem(
                text = { Text("全部") },
                onClick = {
                    onSelect(null)
                    expanded = false
                }
            )
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(labels[option] ?: option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
