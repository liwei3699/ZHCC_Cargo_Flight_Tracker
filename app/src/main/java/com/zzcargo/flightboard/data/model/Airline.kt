package com.zzcargo.flightboard.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Airline(
    val code: String = "",
    val name: String = ""
)
