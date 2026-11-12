package com.zzcargo.flightboard.data.remote

import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ZzcargoApiTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun api(): FlightSource = ZzcargoApi(
        client = OkHttpClient(),
        baseUrl = server.url("/").toString().trimEnd('/'),
        retryDelayMillis = { 0L }
    )

    private fun record(
        id: Long,
        airline: String,
        flightNo: String,
        ioAttr: String = "A",
        extra: String = ""
    ): String =
        """{"id":$id,"airlinesIataCd":"$airline","flightNo":"$flightNo","ioAttr":"$ioAttr","execDate":"2026-11-05","startStationCn":"乌鲁木齐","terminalStationCn":"郑州","airrouteCn":"乌鲁木齐-郑州","airrouteIataCd":"URC-CGO","airrouteIcaoCd":"ZWWW-ZHCC","scheTakeOffTime":"21:00","actlTakeOffTime":"21:12","scheLandingTime":"00:50","actlLandingTime":"00:40","normalStateCn":"到达","abnormalStateCn":"","absreasonCn":"","agency":"南航","flightTaskCn":"正班","craftType":"B738","craftNo":"B1400","terminal":"T2","stand":"223","carousel":"6","regionAttr":"DOM"$extra}"""

    private fun pageJson(current: Int, pages: Int, vararg records: String): String =
        """{"code":200,"data":{"current":$current,"pages":$pages,"records":[${records.joinToString(",")}]}}"""

    private fun jsonResponse(body: String): MockResponse =
        MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json; charset=utf-8")
            .setBody(body)

    @Test
    fun `fetches all pages and reports final progress`() = runTest {
        server.enqueue(jsonResponse(pageJson(1, 2, record(1, "CZ", "6376"))))
        server.enqueue(jsonResponse(pageJson(2, 2, record(2, "K4", "819"))))

        val progress = mutableListOf<Pair<Int, Int>>()
        val flights = api().fetchAll { completed, total -> progress.add(completed to total) }

        assertEquals(2, flights.size)
        assertEquals(2 to 2, progress.last())
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `parses fields and preserves leading zeros`() = runTest {
        server.enqueue(jsonResponse(pageJson(1, 1, record(1, "CX", "051"))))

        val flight = api().fetchAll().single()

        assertEquals("CX051", flight.displayFlightNo)
        assertEquals("乌鲁木齐", flight.startStationCn)
        assertEquals("郑州", flight.terminalStationCn)
        assertEquals("21:00", flight.scheTakeOffTime)
        assertEquals("到达", flight.displayStatus)
        assertEquals("B738", flight.craftType)
        assertEquals("国内", flight.regionLabel)
    }

    @Test
    fun `retries a failing page`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))
        server.enqueue(jsonResponse(pageJson(1, 1, record(1, "CZ", "6376"))))

        val flights = api().fetchAll()

        assertEquals(1, flights.size)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `throws after three failed attempts`() = runTest {
        repeat(3) { server.enqueue(MockResponse().setResponseCode(500)) }

        var error: ApiException? = null
        try {
            api().fetchAll()
        } catch (e: ApiException) {
            error = e
        }

        assertNotNull(error)
        assertTrue(error!!.message!!.contains("第 1 页"))
    }

    @Test
    fun `throws when response code field is not 200`() = runTest {
        val body = """{"code":500,"data":null}"""
        repeat(3) { server.enqueue(jsonResponse(body)) }

        var error: ApiException? = null
        try {
            api().fetchAll()
        } catch (e: ApiException) {
            error = e
        }
        assertNotNull(error)
    }

    @Test
    fun `throws when data object is missing`() = runTest {
        val body = """{"code":200}"""
        repeat(3) { server.enqueue(jsonResponse(body)) }

        var error: ApiException? = null
        try {
            api().fetchAll()
        } catch (e: ApiException) {
            error = e
        }
        assertNotNull(error)
    }

    @Test
    fun `sends the expected form body and browser headers`() = runTest {
        server.enqueue(jsonResponse(pageJson(1, 1, record(1, "CZ", "6376"))))

        api().fetchAll()

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/flightDynamic/skipTo", request.path?.substringBefore('?'))
        assertEquals(
            "current=1&flightNo=&execDate=&ioAttr=&agency=&startStationCn=&terminalStationCn=",
            request.body.readUtf8()
        )
        assertTrue(request.getHeader("Referer")!!.endsWith("/airline/1"))
        assertEquals("XMLHttpRequest", request.getHeader("X-Requested-With"))
        assertTrue(request.getHeader("User-Agent")!!.startsWith("Mozilla/5.0"))
    }

    @Test
    fun `fetches airline directory names`() = runTest {
        server.enqueue(
            jsonResponse(
                """{"code":200,"data":{"current":1,"pages":1,"records":[{"code":"CZ","name":"中国南方航空"}]}}"""
            )
        )

        val airlines = api().fetchAirlines()

        assertEquals(1, airlines.size)
        assertEquals("CZ", airlines[0].code)
        assertEquals("中国南方航空", airlines[0].name)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/airline/airlineTwo", request.path?.substringBefore('?'))
        assertEquals("current=1", request.body.readUtf8())
    }
}
