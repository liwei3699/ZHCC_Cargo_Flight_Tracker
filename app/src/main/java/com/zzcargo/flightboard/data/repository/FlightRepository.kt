package com.zzcargo.flightboard.data.repository

import com.zzcargo.flightboard.data.cache.SnapshotCache
import com.zzcargo.flightboard.data.model.FlightSnapshot
import com.zzcargo.flightboard.data.remote.FlightSource
import com.zzcargo.flightboard.domain.FlightSorter
import kotlinx.coroutines.CancellationException

data class RefreshResult(
    val snapshot: FlightSnapshot,
    val cacheSaved: Boolean
)

interface FlightRepository {
    fun loadCached(): FlightSnapshot?
    suspend fun refresh(onProgress: (Int, Int) -> Unit = { _, _ -> }): RefreshResult
}

class DefaultFlightRepository(
    private val source: FlightSource,
    private val cache: SnapshotCache,
    private val clock: () -> Long = System::currentTimeMillis
) : FlightRepository {

    override fun loadCached(): FlightSnapshot? = cache.read()

    override suspend fun refresh(onProgress: (Int, Int) -> Unit): RefreshResult {
        val flights = source.fetchAll(onProgress)
        val previous = cache.read()
        val airlines = try {
            source.fetchAirlines()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            previous?.airlines.orEmpty()
        }.ifEmpty {
            previous?.airlines.orEmpty()
        }.filter { it.code.isNotBlank() }
            .distinctBy { it.code }

        val snapshot = FlightSnapshot(
            fetchedAtEpochMillis = clock(),
            flights = FlightSorter.sort(flights.distinctBy { it.id }),
            airlines = airlines
        )
        val cacheSaved = runCatching { cache.write(snapshot) }.isSuccess
        return RefreshResult(snapshot = snapshot, cacheSaved = cacheSaved)
    }
}
