package com.zzcargo.flightboard.data.repository

import com.zzcargo.flightboard.data.cache.SnapshotCache
import com.zzcargo.flightboard.data.model.Airline
import com.zzcargo.flightboard.data.model.Flight
import com.zzcargo.flightboard.data.model.FlightSnapshot
import com.zzcargo.flightboard.data.remote.ApiException
import com.zzcargo.flightboard.data.remote.FlightSource
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultFlightRepositoryTest {

    private class FakeSource(
        private val airlineList: List<Airline> = emptyList(),
        private val block: () -> List<Flight>
    ) : FlightSource {
        override suspend fun fetchAll(onProgress: (Int, Int) -> Unit): List<Flight> {
            onProgress(1, 1)
            return block()
        }

        override suspend fun fetchAirlines(): List<Airline> = airlineList
    }

    private fun flight(id: Long, date: String, time: String): Flight = Flight(
        id = id,
        airlinesIataCd = "CZ",
        flightNo = id.toString(),
        execDate = date,
        scheTakeOffTime = time
    )

    private fun cache(): SnapshotCache =
        SnapshotCache(File(Files.createTempDirectory("repo-cache").toFile(), "snapshot.json"))

    @Test
    fun `refresh dedupes by id sorts and caches`() = runTest {
        val source = FakeSource {
            listOf(
                flight(2, "2026-11-06", "03:00"),
                flight(1, "2026-11-05", "21:00"),
                flight(1, "2026-11-05", "22:00")
            )
        }
        val cache = cache()
        val repository: FlightRepository =
            DefaultFlightRepository(source, cache, clock = { 1_000L })

        val result = repository.refresh()

        assertEquals(2, result.snapshot.flights.size)
        assertEquals(listOf(1L, 2L), result.snapshot.flights.map { it.id })
        assertEquals(1_000L, result.snapshot.fetchedAtEpochMillis)
        assertTrue(result.cacheSaved)
        assertEquals(2, cache.read()!!.flights.size)
    }

    @Test
    fun `failed refresh leaves the previous cache untouched`() = runTest {
        val cache = cache()
        val previous = FlightSnapshot(
            fetchedAtEpochMillis = 5L,
            flights = listOf(flight(9, "2026-11-05", "09:00"))
        )
        cache.write(previous)
        val repository: FlightRepository = DefaultFlightRepository(
            FakeSource { throw ApiException("boom") },
            cache,
            clock = { 2_000L }
        )

        var error: ApiException? = null
        try {
            repository.refresh()
        } catch (e: ApiException) {
            error = e
        }

        assertEquals("boom", error?.message)
        assertEquals(1, cache.read()!!.flights.size)
        assertEquals(9L, cache.read()!!.flights.first().id)
        assertEquals(5L, cache.read()!!.fetchedAtEpochMillis)
    }

    @Test
    fun `cache save failure is reported without losing the snapshot`() = runTest {
        val blockedParent = File(Files.createTempDirectory("repo-blocked").toFile(), "blocked")
        blockedParent.writeText("this is a file, not a directory", Charsets.UTF_8)
        val cache = SnapshotCache(File(blockedParent, "snapshot.json"))
        val repository: FlightRepository = DefaultFlightRepository(
            FakeSource { listOf(flight(1, "2026-11-05", "21:00")) },
            cache,
            clock = { 3_000L }
        )

        val result = repository.refresh()

        assertFalse(result.cacheSaved)
        assertEquals(1, result.snapshot.flights.size)
        assertEquals(3_000L, result.snapshot.fetchedAtEpochMillis)
    }

    @Test
    fun `refresh caches the airline directory`() = runTest {
        val cache = cache()
        val repository: FlightRepository = DefaultFlightRepository(
            FakeSource(
                airlineList = listOf(
                    Airline("CZ", "中国南方航空"),
                    Airline("K4", "卡利塔航空")
                )
            ) { listOf(flight(1, "2026-11-05", "21:00")) },
            cache,
            clock = { 4_000L }
        )

        val result = repository.refresh()

        assertEquals(2, result.snapshot.airlines.size)
        assertEquals("中国南方航空", result.snapshot.airlines.first().name)
        assertEquals(2, cache.read()!!.airlines.size)
    }
}
