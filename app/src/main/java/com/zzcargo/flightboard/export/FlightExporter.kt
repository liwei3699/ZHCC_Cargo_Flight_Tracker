package com.zzcargo.flightboard.export

import com.zzcargo.flightboard.data.model.Flight

data class ExportedFile(val uri: String, val displayName: String)

interface FlightExporter {
    suspend fun export(flights: List<Flight>, nowMillis: Long): ExportedFile
    fun open(file: ExportedFile): Boolean
    fun share(file: ExportedFile): Boolean
}
