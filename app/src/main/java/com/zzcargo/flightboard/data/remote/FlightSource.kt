package com.zzcargo.flightboard.data.remote

import com.zzcargo.flightboard.data.model.Airline
import com.zzcargo.flightboard.data.model.Flight

interface FlightSource {
    suspend fun fetchAll(onProgress: (Int, Int) -> Unit = { _, _ -> }): List<Flight>

    suspend fun fetchAirlines(): List<Airline> = emptyList()
}
