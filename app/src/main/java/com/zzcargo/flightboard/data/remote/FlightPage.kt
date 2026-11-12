package com.zzcargo.flightboard.data.remote

import com.zzcargo.flightboard.data.model.Flight
import kotlinx.serialization.Serializable

@Serializable
data class FlightPage(
    val code: Int = 0,
    val data: FlightPageData? = null
)

@Serializable
data class FlightPageData(
    val current: Int = 0,
    val pages: Int = 0,
    val records: List<Flight> = emptyList()
)
