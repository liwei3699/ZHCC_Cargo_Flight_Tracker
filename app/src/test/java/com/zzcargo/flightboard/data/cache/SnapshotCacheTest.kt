package com.zzcargo.flightboard.data.cache

import com.zzcargo.flightboard.data.model.Flight
import com.zzcargo.flightboard.data.model.FlightSnapshot
import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SnapshotCacheTest {

    private fun tempFile(name: String = "snapshot.json"): File =
        File(Files.createTempDirectory("snapshot-cache").toFile(), name)

    private fun snapshot(fetchedAt: Long, flightId: Long = 1): FlightSnapshot =
        FlightSnapshot(
            fetchedAtEpochMillis = fetchedAt,
            flights = listOf(
                Flight(id = flightId, airlinesIataCd = "CZ", flightNo = "6376")
            )
        )

    @Test
    fun `writes and reads a snapshot`() {
        val cache = SnapshotCache(tempFile())
        cache.write(snapshot(123L))

        val loaded = cache.read()

        assertNotNull(loaded)
        assertEquals(123L, loaded!!.fetchedAtEpochMillis)
        assertEquals(1, loaded.flights.size)
        assertEquals("CZ6376", loaded.flights.first().displayFlightNo)
    }

    @Test
    fun `returns null for a missing file`() {
        val cache = SnapshotCache(tempFile("missing.json"))
        assertNull(cache.read())
    }

    @Test
    fun `returns null for corrupt json instead of crashing`() {
        val file = tempFile()
        file.writeText("{not valid json", Charsets.UTF_8)
        val cache = SnapshotCache(file)
        assertNull(cache.read())
    }

    @Test
    fun `returns null for an unsupported schema version`() {
        val file = tempFile()
        file.writeText(
            """{"schemaVersion":999,"fetchedAtEpochMillis":1,"flights":[]}""",
            Charsets.UTF_8
        )
        assertNull(SnapshotCache(file).read())
    }

    @Test
    fun `overwrites an existing snapshot and leaves no temp file`() {
        val file = tempFile()
        val cache = SnapshotCache(file)
        cache.write(snapshot(1L, flightId = 1))
        cache.write(snapshot(2L, flightId = 2))

        val loaded = cache.read()

        assertEquals(2L, loaded!!.fetchedAtEpochMillis)
        assertEquals(2L, loaded.flights.first().id)
        assertFalse(File(file.parentFile, file.name + ".tmp").exists())
    }
}
