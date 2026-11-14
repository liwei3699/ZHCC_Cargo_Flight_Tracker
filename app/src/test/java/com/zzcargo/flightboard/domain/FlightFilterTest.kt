package com.zzcargo.flightboard.domain

import com.zzcargo.flightboard.data.model.Flight
import org.junit.Assert.assertEquals
import org.junit.Test

class FlightFilterTest {

    private val flights = listOf(
        Flight(
            id = 1,
            airlinesIataCd = "CZ",
            flightNo = "6376",
            ioAttr = "A",
            execDate = "2026-11-05",
            startStationCn = "乌鲁木齐",
            terminalStationCn = "郑州",
            airrouteCn = "乌鲁木齐-郑州",
            agency = "南航",
            normalStateCn = "到达",
            craftType = "B738",
            craftNo = "B1400",
            terminal = "T2",
            stand = "223"
        ),
        Flight(
            id = 2,
            airlinesIataCd = "K4",
            flightNo = "819",
            ioAttr = "D",
            execDate = "2026-11-06",
            startStationCn = "郑州",
            terminalStationCn = "墨西哥城菲利普安吉利斯",
            airrouteCn = "墨西哥城-安克雷奇-郑州-首尔仁川-安克雷奇-墨西哥城",
            agency = "南航",
            abnormalStateCn = "延误",
            absreasonCn = "航空公司",
            craftType = "B744",
            craftNo = "N401KZ",
            terminal = "T2",
            stand = "224"
        ),
        Flight(
            id = 3,
            airlinesIataCd = "GI",
            flightNo = "4013",
            ioAttr = "D",
            execDate = "2026-11-05",
            startStationCn = "郑州",
            terminalStationCn = "潍坊",
            airrouteCn = "杭州-郑州-潍坊",
            agency = "中原龙浩",
            normalStateCn = "起飞",
            craftType = "B738"
        )
    )

    @Test
    fun `filters by flight number contains ignoring case`() {
        val result = FlightFilter.apply(flights, FilterState(flightNoQuery = "k48"))
        assertEquals(listOf(2L), result.map { it.id })
    }

    @Test
    fun `filters by date io agency and city`() {
        assertEquals(listOf(1L), FlightFilter.apply(flights, FilterState(execDate = "2026-11-05", ioAttr = "A")).map { it.id })
        assertEquals(listOf(3L), FlightFilter.apply(flights, FilterState(agency = "中原龙浩")).map { it.id })
        assertEquals(listOf(1L), FlightFilter.apply(flights, FilterState(startStationQuery = "乌鲁")).map { it.id })
        assertEquals(listOf(3L), FlightFilter.apply(flights, FilterState(terminalStationQuery = "潍坊")).map { it.id })
    }

    @Test
    fun `quick status filters arrivals departures and delays`() {
        assertEquals(listOf(1L), FlightFilter.apply(flights, FilterState(statusQuick = StatusQuick.ARRIVED)).map { it.id })
        assertEquals(listOf(3L), FlightFilter.apply(flights, FilterState(statusQuick = StatusQuick.DEPARTED)).map { it.id })
        assertEquals(listOf(2L), FlightFilter.apply(flights, FilterState(statusQuick = StatusQuick.DELAYED)).map { it.id })
    }

    @Test
    fun `keyword searches multiple fields and supports multiple words`() {
        assertEquals(listOf(1L), FlightFilter.apply(flights, FilterState(keyword = "B1400")).map { it.id })
        assertEquals(listOf(1L), FlightFilter.apply(flights, FilterState(keyword = "cz6376 乌鲁木齐")).map { it.id })
        assertEquals(listOf(2L), FlightFilter.apply(flights, FilterState(keyword = "延误 郑州")).map { it.id })
    }

    @Test
    fun `conditions are combined with and`() {
        val result = FlightFilter.apply(
            flights,
            FilterState(keyword = "郑州", ioAttr = "D", statusQuick = StatusQuick.DELAYED)
        )
        assertEquals(listOf(2L), result.map { it.id })
    }

    @Test
    fun `empty filters return all flights`() {
        assertEquals(3, FlightFilter.apply(flights, FilterState()).size)
    }

    @Test
    fun `sheet filter count excludes keyword and quick status`() {
        val state = FilterState(
            flightNoQuery = "CZ",
            execDate = "2026-11-05",
            startStationQuery = "郑州",
            keyword = "abc",
            statusQuick = StatusQuick.ARRIVED
        )
        assertEquals(3, state.sheetFilterCount)
    }

    @Test
    fun `filters by exact date alone`() {
        assertEquals(
            listOf(2L),
            FlightFilter.apply(flights, FilterState(execDate = "2026-11-06")).map { it.id }
        )
    }

    @Test
    fun `filters by airline code and aircraft type`() {
        assertEquals(
            listOf(2L),
            FlightFilter.apply(flights, FilterState(airline = "K4")).map { it.id }
        )
        assertEquals(
            listOf(1L, 3L),
            FlightFilter.apply(flights, FilterState(craftType = "B738")).map { it.id }
        )
        assertEquals(
            listOf(2L),
            FlightFilter.apply(flights, FilterState(keyword = "K4")).map { it.id }
        )
    }

    @Test
    fun `sheet filter count includes airline and aircraft type`() {
        val state = FilterState(
            airline = "CZ",
            craftType = "B738",
            keyword = "abc",
            statusQuick = StatusQuick.ARRIVED
        )
        assertEquals(2, state.sheetFilterCount)
    }

    @Test
    fun `filters by aircraft family and subtype`() {
        assertEquals(
            listOf(1L, 3L),
            FlightFilter.apply(flights, FilterState(craftFamily = "波音737")).map { it.id }
        )
        assertEquals(
            listOf(2L),
            FlightFilter.apply(flights, FilterState(craftFamily = "波音747")).map { it.id }
        )
        assertEquals(
            listOf(2L),
            FlightFilter.apply(flights, FilterState(craftType = "B744")).map { it.id }
        )
        assertEquals(
            emptyList<Long>(),
            FlightFilter.apply(
                flights,
                FilterState(craftFamily = "波音737", craftType = "B744")
            ).map { it.id }
        )
    }

    @Test
    fun `sheet filter count includes aircraft family`() {
        assertEquals(1, FilterState(craftFamily = "波音737").sheetFilterCount)
    }
}
