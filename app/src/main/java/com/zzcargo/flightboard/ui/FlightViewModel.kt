package com.zzcargo.flightboard.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zzcargo.flightboard.data.model.Flight
import com.zzcargo.flightboard.data.model.FlightSnapshot
import com.zzcargo.flightboard.data.repository.FlightRepository
import com.zzcargo.flightboard.domain.FilterState
import com.zzcargo.flightboard.domain.FlightFilter
import com.zzcargo.flightboard.domain.FlightSorter
import com.zzcargo.flightboard.domain.SortState
import com.zzcargo.flightboard.domain.StatusQuick
import com.zzcargo.flightboard.export.ExportedFile
import com.zzcargo.flightboard.export.FlightExporter
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.security.cert.CertPathValidatorException
import javax.net.ssl.SSLHandshakeException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FlightViewModel(
    private val repository: FlightRepository,
    private val exporter: FlightExporter,
    private val clock: () -> Long = System::currentTimeMillis
) : ViewModel() {

    private val _state = MutableStateFlow(FlightUiState())
    val state: StateFlow<FlightUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    private var refreshJob: Job? = null
    private var foregroundJob: Job? = null
    private var nextAutoRefreshAt: Long = 0L

    init {
        val cached = repository.loadCached()
        if (cached != null) {
            applySnapshot(cached)
            nextAutoRefreshAt = cached.fetchedAtEpochMillis + REFRESH_INTERVAL_MILLIS
        } else {
            _state.update { it.copy(isInitialLoading = true) }
        }
    }

    fun setForeground(active: Boolean) {
        if (active) {
            if (foregroundJob?.isActive == true) return
            foregroundJob = viewModelScope.launch {
                if (clock() >= nextAutoRefreshAt) {
                    refresh()
                    refreshJob?.join()
                }
                while (isActive) {
                    val waitMs = (nextAutoRefreshAt - clock()).coerceAtLeast(1_000L)
                    delay(waitMs)
                    if (clock() >= nextAutoRefreshAt) {
                        refresh()
                        refreshJob?.join()
                    }
                }
            }
        } else {
            foregroundJob?.cancel()
            foregroundJob = null
        }
    }

    fun refresh() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    isRefreshing = true,
                    isInitialLoading = it.allFlights.isEmpty(),
                    errorMessage = null,
                    refreshCompletedPages = 0,
                    refreshTotalPages = 0
                )
            }
            var success = false
            try {
                val result = repository.refresh { completed, total ->
                    _state.update {
                        it.copy(
                            refreshCompletedPages = maxOf(it.refreshCompletedPages, completed),
                            refreshTotalPages = total
                        )
                    }
                }
                applySnapshot(result.snapshot)
                success = true
                if (!result.cacheSaved) {
                    _events.emit(UiEvent.Message("缓存保存失败，本次数据仍可正常使用"))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = friendlyError(e)) }
            } finally {
                if (!success) {
                    nextAutoRefreshAt = clock() + REFRESH_INTERVAL_MILLIS
                }
                _state.update { it.copy(isRefreshing = false, isInitialLoading = false) }
            }
        }
    }

    fun setKeyword(value: String) = updateFilters { it.copy(keyword = value) }

    fun setStatusQuick(value: StatusQuick) = updateFilters { it.copy(statusQuick = value) }

    fun setSort(value: SortState) {
        _state.update { current ->
            current.copy(
                sort = value,
                visibleFlights = computeVisible(current.allFlights, current.filters, value)
            )
        }
    }

    fun applySheetFilters(draft: FilterState) {
        updateFilters { current ->
            draft.copy(keyword = current.keyword, statusQuick = current.statusQuick)
        }
    }

    fun resetFilters() = updateFilters { FilterState() }

    fun dismissError() {
        _state.update { it.copy(errorMessage = null) }
    }

    fun exportCurrent() {
        val current = _state.value
        if (current.visibleFlights.isEmpty() || current.isRefreshing || current.exportInProgress) {
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(exportInProgress = true) }
            try {
                val file = exporter.export(current.visibleFlights, clock())
                _events.emit(UiEvent.ExportReady(file))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _events.emit(UiEvent.ExportFailed("导出失败：${e.message ?: "未知错误"}"))
            } finally {
                _state.update { it.copy(exportInProgress = false) }
            }
        }
    }

    fun openExport(file: ExportedFile) {
        if (!exporter.open(file)) {
            viewModelScope.launch { _events.emit(UiEvent.Message("未找到可打开 xlsx 的应用")) }
        }
    }

    fun shareExport(file: ExportedFile) {
        if (!exporter.share(file)) {
            viewModelScope.launch { _events.emit(UiEvent.Message("未找到可分享的应用")) }
        }
    }

    fun autoRefreshDelayMillis(): Long = (nextAutoRefreshAt - clock()).coerceAtLeast(0L)

    private fun updateFilters(transform: (FilterState) -> FilterState) {
        _state.update { current ->
            val filters = transform(current.filters)
            current.copy(
                filters = filters,
                visibleFlights = computeVisible(current.allFlights, filters, current.sort)
            )
        }
    }

    private fun applySnapshot(snapshot: FlightSnapshot) {
        val sorted = FlightSorter.sort(snapshot.flights)
        _state.update { current ->
            current.copy(
                allFlights = sorted,
                visibleFlights = computeVisible(sorted, current.filters, current.sort),
                airlines = snapshot.airlines.ifEmpty { current.airlines },
                lastUpdatedEpochMillis = snapshot.fetchedAtEpochMillis,
                errorMessage = null,
                isInitialLoading = false
            )
        }
        nextAutoRefreshAt = snapshot.fetchedAtEpochMillis + REFRESH_INTERVAL_MILLIS
    }

    private fun computeVisible(
        allFlights: List<Flight>,
        filters: FilterState,
        sort: SortState
    ): List<Flight> = FlightSorter.sort(FlightFilter.apply(allFlights, filters), sort)

    private fun friendlyError(e: Exception): String {
        var cause: Throwable? = e
        while (cause != null) {
            when (cause) {
                is UnknownHostException -> return "网络不可用，请检查网络连接"
                is SocketTimeoutException -> return "请求超时，请稍后重试"
                is SSLHandshakeException, is CertPathValidatorException ->
                    return "HTTPS 证书校验失败，请检查网络或更新应用"
            }
            cause = cause.cause
        }
        return e.message?.takeIf { it.isNotBlank() } ?: "未知错误"
    }

    companion object {
        private const val REFRESH_INTERVAL_MILLIS = 30 * 60 * 1000L

        fun factory(
            repository: FlightRepository,
            exporter: FlightExporter
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                FlightViewModel(repository, exporter) as T
        }
    }
}
