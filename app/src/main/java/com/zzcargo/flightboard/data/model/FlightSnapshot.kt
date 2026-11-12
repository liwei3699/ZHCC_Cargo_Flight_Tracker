package com.zzcargo.flightboard.data.model

import kotlinx.serialization.Serializable

@Serializable
data class FlightSnapshot(
    val schemaVersion: Int = 1,
    val fetchedAtEpochMillis: Long,
    val flights: List<Flight>,
    val airlines: List<Airline> = emptyList()
)
