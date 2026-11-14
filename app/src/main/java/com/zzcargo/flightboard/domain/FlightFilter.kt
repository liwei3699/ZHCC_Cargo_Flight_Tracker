package com.zzcargo.flightboard.domain

import com.zzcargo.flightboard.data.model.Flight

enum class StatusQuick { ALL, ARRIVED, DEPARTED, DELAYED }

data class FilterState(
    val flightNoQuery: String = "",
    val execDate: String? = null,
    val ioAttr: String? = null,
    val agency: String? = null,
    val airline: String? = null,
    val craftFamily: String? = null,
    val craftType: String? = null,
    val startStationQuery: String = "",
    val terminalStationQuery: String = "",
    val statusQuick: StatusQuick = StatusQuick.ALL,
    val keyword: String = ""
) {
    val hasSheetFilters: Boolean
        get() = flightNoQuery.isNotBlank() || execDate != null || ioAttr != null ||
            agency != null || airline != null || craftFamily != null || craftType != null ||
            startStationQuery.isNotBlank() || terminalStationQuery.isNotBlank()

    val sheetFilterCount: Int
        get() = listOf(
            flightNoQuery.isNotBlank(),
            execDate != null,
            ioAttr != null,
            agency != null,
            airline != null,
            craftFamily != null,
            craftType != null,
            startStationQuery.isNotBlank(),
            terminalStationQuery.isNotBlank()
        ).count { it }
}

object FlightFilter {
    fun apply(flights: List<Flight>, filters: FilterState): List<Flight> {
        val flightNo = filters.flightNoQuery.trim()
        val start = filters.startStationQuery.trim()
        val terminal = filters.terminalStationQuery.trim()
        val keywords = filters.keyword.trim().lowercase()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        return flights.filter { flight ->
            (flightNo.isEmpty() || flight.displayFlightNo.contains(flightNo, ignoreCase = true)) &&
                (filters.execDate == null || flight.execDate == filters.execDate) &&
                (filters.ioAttr == null || flight.ioAttr == filters.ioAttr) &&
                (filters.agency == null || flight.agency == filters.agency) &&
                (filters.airline == null || flight.airlinesIataCd == filters.airline) &&
                (filters.craftFamily == null || CraftTypes.familyOf(flight.craftType) == filters.craftFamily) &&
                (filters.craftType == null || flight.craftType == filters.craftType) &&
                (start.isEmpty() || flight.startStationCn.contains(start, ignoreCase = true)) &&
                (terminal.isEmpty() || flight.terminalStationCn.contains(terminal, ignoreCase = true)) &&
                when (filters.statusQuick) {
                    StatusQuick.ALL -> true
                    StatusQuick.ARRIVED -> flight.displayStatus == "到达"
                    StatusQuick.DEPARTED -> flight.displayStatus == "起飞"
                    StatusQuick.DELAYED -> flight.abnormalStateCn.isNotBlank()
                } &&
                keywords.all { keyword -> flight.matchesKeyword(keyword) }
        }
    }

    private fun Flight.matchesKeyword(keyword: String): Boolean {
        val values = listOf(
            displayFlightNo,
            airlinesIataCd,
            startStationCn,
            terminalStationCn,
            airrouteCn,
            agency,
            normalStateCn,
            abnormalStateCn,
            absreasonCn,
            craftType,
            craftNo,
            terminal,
            stand
        )
        return values.any { value -> value.contains(keyword, ignoreCase = true) }
    }
}
