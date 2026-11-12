package com.zzcargo.flightboard.ui

import com.zzcargo.flightboard.data.model.Airline
import com.zzcargo.flightboard.data.model.Flight
import com.zzcargo.flightboard.domain.FilterState
import com.zzcargo.flightboard.domain.SortState

data class FlightUiState(
    val allFlights: List<Flight> = emptyList(),
    val visibleFlights: List<Flight> = emptyList(),
    val filters: FilterState = FilterState(),
    val sort: SortState = SortState(),
    val airlines: List<Airline> = emptyList(),
    val isInitialLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val refreshCompletedPages: Int = 0,
    val refreshTotalPages: Int = 0,
    val lastUpdatedEpochMillis: Long? = null,
    val errorMessage: String? = null,
    val exportInProgress: Boolean = false
)
