package com.zzcargo.flightboard.domain

import com.zzcargo.flightboard.data.model.Flight
import org.junit.Assert.assertEquals
import org.junit.Test

class FlightSorterTest {

    private fun flight(
        id: Long,
        date: String,
        time: String,
        no: String = id.toString(),
        start: String = "",
        terminal: String = "",
        craftType: String = ""
    ) = Flight(
        id = id,
        airlinesIataCd = "CZ",
        flightNo = no,
        execDate = date,
        scheTakeOffTime = time,
        startStationCn = start,
        terminalStationCn = terminal,
        craftType = craftType
    )

    @Test
    fun `sorts by date then planned takeoff and puts blanks last`() {
        val flights = listOf(
            flight(1, "2026-11-06", "03:00"),
            flight(2, "2026-11-05", "22:00"),
            flight(3, "2026-11-05", "06:00"),
            flight(4, "2026-11-05", ""),
            flight(5, "", "01:00")
        )

        val sorted = FlightSorter.sort(flights)

        assertEquals(listOf(3L, 2L, 4L, 1L, 5L), sorted.map { it.id })
    }

    @Test
    fun `sorts by flight number when date and time are equal`() {
        val flights = listOf(
            flight(1, "2026-11-05", "08:00", "CZ900"),
            flight(2, "2026-11-05", "08:00", "CZ100")
        )

        val sorted = FlightSorter.sort(flights)

        assertEquals(listOf(2L, 1L), sorted.map { it.id })
    }

    @Test
    fun `sorts by planned takeoff in both directions and keeps blanks last`() {
        val flights = listOf(
            flight(1, "2026-11-05", "22:00"),
            flight(2, "2026-11-06", "06:00"),
            flight(3, "2026-11-05", "")
        )

        assertEquals(
            listOf(2L, 1L, 3L),
            FlightSorter.sort(
                flights,
                SortState(SortField.SCHE_TAKEOFF, SortDirection.ASC)
            ).map { it.id }
        )
        assertEquals(
            listOf(1L, 2L, 3L),
            FlightSorter.sort(
                flights,
                SortState(SortField.SCHE_TAKEOFF, SortDirection.DESC)
            ).map { it.id }
        )
    }

    @Test
    fun `sorts by airport and aircraft type`() {
        val flights = listOf(
            flight(1, "2026-11-05", "08:00", start = "C", terminal = "C", craftType = "B744"),
            flight(2, "2026-11-05", "08:00", start = "A", terminal = "A", craftType = "A320"),
            flight(3, "2026-11-05", "08:00", start = "B", terminal = "B", craftType = "B738")
        )

        assertEquals(
            listOf(2L, 3L, 1L),
            FlightSorter.sort(flights, SortState(SortField.START_STATION)).map { it.id }
        )
        assertEquals(
            listOf(1L, 3L, 2L),
            FlightSorter.sort(
                flights,
                SortState(SortField.START_STATION, SortDirection.DESC)
            ).map { it.id }
        )
        assertEquals(
            listOf(2L, 3L, 1L),
            FlightSorter.sort(flights, SortState(SortField.TERMINAL_STATION)).map { it.id }
        )
        assertEquals(
            listOf(2L, 3L, 1L),
            FlightSorter.sort(flights, SortState(SortField.CRAFT_TYPE)).map { it.id }
        )
    }

    @Test
    fun `sorts by date and flight number with default tie breaks`() {
        val flights = listOf(
            flight(1, "2026-11-06", "08:00", "200"),
            flight(2, "2026-11-05", "08:00", "100"),
            flight(3, "2026-11-05", "09:00", "300")
        )

        assertEquals(
            listOf(2L, 3L, 1L),
            FlightSorter.sort(flights, SortState(SortField.EXEC_DATE)).map { it.id }
        )
        assertEquals(
            listOf(1L, 2L, 3L),
            FlightSorter.sort(
                flights,
                SortState(SortField.EXEC_DATE, SortDirection.DESC)
            ).map { it.id }
        )
        assertEquals(
            listOf(2L, 1L, 3L),
            FlightSorter.sort(flights, SortState(SortField.FLIGHT_NO)).map { it.id }
        )
    }
}
