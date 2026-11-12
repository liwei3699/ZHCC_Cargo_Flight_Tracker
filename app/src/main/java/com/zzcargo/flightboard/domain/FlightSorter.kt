package com.zzcargo.flightboard.domain

import com.zzcargo.flightboard.data.model.Flight
import java.text.Collator
import java.util.Locale

enum class SortField(val label: String) {
    DEFAULT("默认（日期+计划起飞）"),
    START_STATION("起飞机场"),
    TERMINAL_STATION("降落机场"),
    SCHE_TAKEOFF("计划起飞时间"),
    ACTL_TAKEOFF("实际起飞时间"),
    SCHE_LANDING("计划降落时间"),
    ACTL_LANDING("实际降落时间"),
    EXEC_DATE("航班日期"),
    CRAFT_TYPE("机型"),
    FLIGHT_NO("航班号");

    val shortLabel: String get() = if (this == DEFAULT) "默认" else label
}

enum class SortDirection(val label: String) {
    ASC("升序"),
    DESC("降序")
}

data class SortState(
    val field: SortField = SortField.DEFAULT,
    val direction: SortDirection = SortDirection.ASC
)

object FlightSorter {
    private val collator: Collator = Collator.getInstance(Locale.CHINA)

    fun sort(flights: List<Flight>): List<Flight> = sort(flights, SortState())

    fun sort(flights: List<Flight>, state: SortState): List<Flight> {
        if (state.field == SortField.DEFAULT) {
            return flights.sortedWith(defaultComparator(state.direction))
        }

        val fallback = defaultComparator(SortDirection.ASC)
        val primary = Comparator<Flight> { a, b ->
            when (state.field) {
                SortField.DEFAULT -> 0
                SortField.START_STATION ->
                    compareText(a.startStationCn, b.startStationCn, state.direction, true)
                SortField.TERMINAL_STATION ->
                    compareText(a.terminalStationCn, b.terminalStationCn, state.direction, true)
                SortField.SCHE_TAKEOFF ->
                    compareText(a.scheTakeOffTime, b.scheTakeOffTime, state.direction)
                SortField.ACTL_TAKEOFF ->
                    compareText(a.actlTakeOffTime, b.actlTakeOffTime, state.direction)
                SortField.SCHE_LANDING ->
                    compareText(a.scheLandingTime, b.scheLandingTime, state.direction)
                SortField.ACTL_LANDING ->
                    compareText(a.actlLandingTime, b.actlLandingTime, state.direction)
                SortField.EXEC_DATE ->
                    compareText(a.execDate, b.execDate, state.direction)
                SortField.CRAFT_TYPE ->
                    compareText(a.craftType, b.craftType, state.direction, true)
                SortField.FLIGHT_NO ->
                    compareText(a.displayFlightNo, b.displayFlightNo, state.direction)
            }
        }

        return flights.sortedWith(Comparator { a, b ->
            val primaryResult = primary.compare(a, b)
            if (primaryResult != 0) primaryResult else fallback.compare(a, b)
        })
    }

    private fun defaultComparator(direction: SortDirection): Comparator<Flight> =
        Comparator { a, b ->
            var result = compareText(a.execDate, b.execDate, direction)
            if (result == 0) result = compareText(a.scheTakeOffTime, b.scheTakeOffTime, direction)
            if (result == 0) result = compareText(a.displayFlightNo, b.displayFlightNo, direction)
            result
        }

    private fun compareText(
        a: String,
        b: String,
        direction: SortDirection,
        localeAware: Boolean = false
    ): Int {
        val aBlank = a.isBlank()
        val bBlank = b.isBlank()
        if (aBlank && bBlank) return 0
        if (aBlank) return 1
        if (bBlank) return -1
        val comparison = if (localeAware) collator.compare(a, b) else a.compareTo(b)
        return if (direction == SortDirection.ASC) comparison else -comparison
    }
}
