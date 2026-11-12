package com.zzcargo.flightboard.data.remote

import com.zzcargo.flightboard.data.model.Airline
import kotlinx.serialization.Serializable

@Serializable
data class AirlinePage(
    val code: Int = 0,
    val data: AirlinePageData? = null
)

@Serializable
data class AirlinePageData(
    val current: Int = 0,
    val pages: Int = 0,
    val records: List<Airline> = emptyList()
)
