package com.zzcargo.flightboard.data.cache

import com.zzcargo.flightboard.data.model.FlightSnapshot
import java.io.File
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlinx.serialization.json.Json

class SnapshotCache(
    private val file: File,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }
) {
    fun read(): FlightSnapshot? = runCatching {
        if (!file.exists()) {
            null
        } else {
            json.decodeFromString(FlightSnapshot.serializer(), file.readText(Charsets.UTF_8))
                .takeIf { it.schemaVersion == 1 }
        }
    }.getOrNull()

    fun write(snapshot: FlightSnapshot) {
        val parent = file.absoluteFile.parentFile ?: throw IOException("缓存目录无效")
        parent.mkdirs()
        val temp = File(parent, file.name + ".tmp")
        temp.writeText(json.encodeToString(FlightSnapshot.serializer(), snapshot), Charsets.UTF_8)
        try {
            Files.move(
                temp.toPath(),
                file.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        } catch (e: AtomicMoveNotSupportedException) {
            Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
