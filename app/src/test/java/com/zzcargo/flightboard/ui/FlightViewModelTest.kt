package com.zzcargo.flightboard.ui

import com.zzcargo.flightboard.data.model.Flight
import com.zzcargo.flightboard.data.model.FlightSnapshot
import com.zzcargo.flightboard.data.repository.FlightRepository
import com.zzcargo.flightboard.data.repository.RefreshResult
import com.zzcargo.flightboard.domain.SortDirection
import com.zzcargo.flightboard.domain.SortField
import com.zzcargo.flightboard.domain.SortState
import com.zzcargo.flightboard.export.ExportedFile
import com.zzcargo.flightboard.export.FlightExporter
import com.zzcargo.flightboard.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FlightViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val exporter = FakeExporter()

    private class FakeRepository(
        var cached: FlightSnapshot? = null,
        var progress: List<Pair<Int, Int>> = listOf(1 to 1),
        var result: () -> RefreshResult = { throw IllegalStateException("not configured") }
    ) : FlightRepository {
        override fun loadCached(): FlightSnapshot? = cached
        override suspend fun refresh(onProgress: (Int, Int) -> Unit): RefreshResult {
            progress.forEach { (completed, total) -> onProgress(completed, total) }
            return result()
        }
    }

    private class FakeExporter : FlightExporter {
        var lastExported: List<Flight>? = null
        override suspend fun export(flights: List<Flight>, nowMillis: Long): ExportedFile {
            lastExported = flights
            return ExportedFile("content://test/1", "test.xlsx")
        }
        override fun open(file: ExportedFile): Boolean = true
        override fun share(file: ExportedFile): Boolean = true
    }

    private fun flight(id: Long, no: String, date: String = "2026-11-05"): Flight = Flight(
        id = id,
        airlinesIataCd = "CZ",
        flightNo = no,
        execDate = date,
        scheTakeOffTime = "08:00",
        startStationCn = "郑州",
        terminalStationCn = "上海",
        normalStateCn = "起飞"
    )

    private fun snapshot(vararg flights: Flight): FlightSnapshot =
        FlightSnapshot(fetchedAtEpochMillis = 1_000L, flights = flights.toList())

    @Test
    fun `restores cached snapshot and filters locally`() {
        val repository = FakeRepository(cached = snapshot(flight(1, "100"), flight(2, "200")))
        val viewModel = FlightViewModel(repository, exporter, clock = { 1_000L })

        assertEquals(2, viewModel.state.value.allFlights.size)
        assertEquals(2, viewModel.state.value.visibleFlights.size)

        viewModel.setKeyword("CZ100")
        assertEquals(listOf(1L), viewModel.state.value.visibleFlights.map { it.id })

        viewModel.resetFilters()
        assertEquals(2, viewModel.state.value.visibleFlights.size)
    }

    @Test
    fun `refresh replaces snapshot and clears refreshing state`() = runTest {
        val repository = FakeRepository(
            result = { RefreshResult(snapshot(flight(3, "300")), cacheSaved = true) }
        )
        val viewModel = FlightViewModel(repository, exporter, clock = { 2_000L })

        viewModel.refresh()

        assertEquals(listOf(3L), viewModel.state.value.allFlights.map { it.id })
        assertEquals(1_000L, viewModel.state.value.lastUpdatedEpochMillis)
        assertFalse(viewModel.state.value.isRefreshing)
    }

    @Test
    fun `refresh failure exposes a friendly error and keeps cached data`() = runTest {
        val repository = FakeRepository(
            cached = snapshot(flight(1, "100")),
            result = { throw java.net.UnknownHostException("offline") }
        )
        val viewModel = FlightViewModel(repository, exporter, clock = { 2_000L })

        viewModel.refresh()

        assertEquals(1, viewModel.state.value.allFlights.size)
        assertEquals("网络不可用，请检查网络连接", viewModel.state.value.errorMessage)
        assertFalse(viewModel.state.value.isRefreshing)
    }

    @Test
    fun `refresh failure maps certificate errors to a Chinese message`() = runTest {
        val repository = FakeRepository(
            result = {
                throw com.zzcargo.flightboard.data.remote.ApiException(
                    "第 1 页获取失败",
                    javax.net.ssl.SSLHandshakeException("bad certificate")
                )
            }
        )
        val viewModel = FlightViewModel(repository, exporter, clock = { 1_000L })

        viewModel.refresh()

        assertEquals(
            "HTTPS 证书校验失败，请检查网络或更新应用",
            viewModel.state.value.errorMessage
        )
    }

    @Test
    fun `exports only the currently visible flights`() = runTest {
        val repository = FakeRepository(cached = snapshot(flight(1, "100"), flight(2, "200")))
        val viewModel = FlightViewModel(repository, exporter, clock = { 1_000L })
        viewModel.setKeyword("CZ100")

        val events = mutableListOf<UiEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.collect { events.add(it) }
        }

        viewModel.exportCurrent()
        advanceUntilIdle()

        assertEquals(listOf(1L), exporter.lastExported!!.map { it.id })
        assertTrue(events.any { it is UiEvent.ExportReady })
        job.cancel()
    }

    @Test
    fun `auto refresh delay is computed from the cached timestamp`() {
        val repository = FakeRepository(cached = snapshot(flight(1, "100")))
        val viewModel = FlightViewModel(repository, exporter, clock = { 61_000L })

        assertEquals(30 * 60 * 1000L - 60_000L, viewModel.autoRefreshDelayMillis())
    }

    @Test
    fun `out of order progress keeps completed pages monotonic`() = runTest {
        val repository = FakeRepository(
            progress = listOf(2 to 3, 1 to 3),
            result = { RefreshResult(snapshot(flight(4, "400")), cacheSaved = true) }
        )
        val viewModel = FlightViewModel(repository, exporter, clock = { 2_000L })

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(2, viewModel.state.value.refreshCompletedPages)
        assertEquals(3, viewModel.state.value.refreshTotalPages)
        assertEquals(listOf(4L), viewModel.state.value.allFlights.map { it.id })
    }

    @Test
    fun `sort selection reorders visible flights`() = runTest {
        val repository = FakeRepository(
            cached = snapshot(
                flight(1, "100", date = "2026-11-05"),
                flight(2, "200", date = "2026-11-06")
            )
        )
        val viewModel = FlightViewModel(repository, exporter, clock = { 1_000L })

        viewModel.setSort(SortState(SortField.EXEC_DATE, SortDirection.DESC))

        assertEquals(listOf(2L, 1L), viewModel.state.value.visibleFlights.map { it.id })
    }
}
