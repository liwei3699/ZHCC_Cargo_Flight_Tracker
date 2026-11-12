package com.zzcargo.flightboard.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zzcargo.flightboard.data.model.Flight
import com.zzcargo.flightboard.domain.SortDirection
import com.zzcargo.flightboard.domain.StatusQuick
import com.zzcargo.flightboard.export.ExportedFile
import com.zzcargo.flightboard.ui.FlightViewModel
import com.zzcargo.flightboard.ui.UiEvent
import com.zzcargo.flightboard.ui.components.DetailSheet
import com.zzcargo.flightboard.ui.components.FilterSheet
import com.zzcargo.flightboard.ui.components.FlightCard
import com.zzcargo.flightboard.ui.components.SortSheet
import com.zzcargo.flightboard.util.DateTimes
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlightListScreen(viewModel: FlightViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current

    var pendingExport by remember { mutableStateOf<ExportedFile?>(null) }
    var showFilter by remember { mutableStateOf(false) }
    var showSort by remember { mutableStateOf(false) }
    var detailFlight by remember { mutableStateOf<Flight?>(null) }
    var showExportConfirm by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.setForeground(true)
                Lifecycle.Event.ON_PAUSE -> viewModel.setForeground(false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.Message -> snackbarHostState.showSnackbar(event.text)
                is UiEvent.ExportFailed -> snackbarHostState.showSnackbar(event.text)
                is UiEvent.ExportReady -> {
                    pendingExport = event.file
                    snackbarHostState.showSnackbar(
                        message = "已保存到 下载/郑州机场航班动态/${event.file.displayName}",
                        duration = SnackbarDuration.Long
                    )
                    pendingExport = null
                }
            }
        }
    }

    var keywordInput by rememberSaveable { mutableStateOf(state.filters.keyword) }
    LaunchedEffect(keywordInput) {
        delay(200)
        viewModel.setKeyword(keywordInput)
    }
    LaunchedEffect(state.filters.keyword) {
        if (state.filters.keyword != keywordInput) {
            keywordInput = state.filters.keyword
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("郑州货运航班动态", fontWeight = FontWeight.Bold)
                        val lastUpdated = state.lastUpdatedEpochMillis
                        val subtitle = if (lastUpdated == null) {
                            "尚未更新"
                        } else {
                            val stale = System.currentTimeMillis() - lastUpdated >= 30 * 60 * 1000L
                            "最后更新 ${DateTimes.formatUpdated(lastUpdated)} · " +
                                if (stale) "数据可能已过期" else "每30分钟自动刷新"
                        }
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refresh() },
                        enabled = !state.isRefreshing
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                    TextButton(
                        onClick = { showExportConfirm = true },
                        enabled = state.visibleFlights.isNotEmpty() &&
                            !state.isRefreshing &&
                            !state.exportInProgress
                    ) {
                        Text("导出")
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text(data.visuals.message)
                        val file = pendingExport
                        if (file != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = {
                                    data.dismiss()
                                    viewModel.openExport(file)
                                }) {
                                    Text("打开")
                                }
                                TextButton(onClick = {
                                    data.dismiss()
                                    viewModel.shareExport(file)
                                }) {
                                    Text("分享")
                                }
                            }
                        } else if (data.visuals.actionLabel != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { data.performAction() }) {
                                    Text(data.visuals.actionLabel!!)
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = keywordInput,
                onValueChange = { keywordInput = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text("搜索航班号、城市、航司、机型…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (keywordInput.isNotEmpty()) {
                        IconButton(onClick = { keywordInput = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "清除搜索")
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.filters.statusQuick == StatusQuick.ALL,
                    onClick = { viewModel.setStatusQuick(StatusQuick.ALL) },
                    label = { Text("全部") }
                )
                FilterChip(
                    selected = state.filters.statusQuick == StatusQuick.ARRIVED,
                    onClick = { viewModel.setStatusQuick(StatusQuick.ARRIVED) },
                    label = { Text("到达") }
                )
                FilterChip(
                    selected = state.filters.statusQuick == StatusQuick.DEPARTED,
                    onClick = { viewModel.setStatusQuick(StatusQuick.DEPARTED) },
                    label = { Text("起飞") }
                )
                FilterChip(
                    selected = state.filters.statusQuick == StatusQuick.DELAYED,
                    onClick = { viewModel.setStatusQuick(StatusQuick.DELAYED) },
                    label = { Text("延误·取消") }
                )
                TextButton(onClick = { showFilter = true }) {
                    Text(
                        if (state.filters.sheetFilterCount > 0) {
                            "筛选 (${state.filters.sheetFilterCount})"
                        } else {
                            "筛选"
                        }
                    )
                }
            }

            if (state.isRefreshing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                if (state.refreshTotalPages > 0) {
                    Text(
                        text = "正在获取 ${state.refreshCompletedPages}/${state.refreshTotalPages} 页",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            state.errorMessage?.let { message ->
                if (state.allFlights.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = message,
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall
                            )
                            TextButton(onClick = { viewModel.refresh() }) {
                                Text("重试")
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "共 ${state.visibleFlights.size} 条",
                    style = MaterialTheme.typography.labelMedium
                )
                TextButton(onClick = { showSort = true }) {
                    Text(
                        text = "排序：${state.sort.field.shortLabel} ${
                            if (state.sort.direction == SortDirection.ASC) "↑" else "↓"
                        }",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when {
                    state.allFlights.isEmpty() && state.errorMessage != null && !state.isRefreshing -> {
                        FullErrorState(
                            message = state.errorMessage!!,
                            onRetry = { viewModel.refresh() }
                        )
                    }
                    state.allFlights.isEmpty() && state.isInitialLoading -> {
                        LoadingState(
                            completed = state.refreshCompletedPages,
                            total = state.refreshTotalPages
                        )
                    }
                    state.visibleFlights.isEmpty() -> {
                        EmptyState(
                            hasAnyData = state.allFlights.isNotEmpty(),
                            onReset = { viewModel.resetFilters() },
                            onRefresh = { viewModel.refresh() }
                        )
                    }
                    else -> {
                        PullToRefreshBox(
                            isRefreshing = state.isRefreshing,
                            onRefresh = { viewModel.refresh() },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            LazyColumn(
                                state = rememberLazyListState(),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(state.visibleFlights, key = { it.id }) { flight ->
                                    FlightCard(
                                        flight = flight,
                                        onClick = { detailFlight = flight }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Text(
                text = "非官方应用 · 数据来源：郑州机场货运站 zzcargo.com",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }

    if (showFilter) {
        FilterSheet(
            initial = state.filters,
            allFlights = state.allFlights,
            airlines = state.airlines,
            onDismiss = { showFilter = false },
            onApply = { draft ->
                viewModel.applySheetFilters(draft)
                showFilter = false
            },
            onResetAll = {
                viewModel.resetFilters()
                showFilter = false
            }
        )
    }

    if (showSort) {
        SortSheet(
            current = state.sort,
            onDismiss = { showSort = false },
            onApply = { sort ->
                viewModel.setSort(sort)
                showSort = false
            }
        )
    }

    detailFlight?.let { flight ->
        DetailSheet(
            flight = flight,
            onDismiss = { detailFlight = null }
        )
    }

    if (showExportConfirm) {
        val hasAnyFilter = state.filters.sheetFilterCount > 0 ||
            state.filters.keyword.isNotBlank() ||
            state.filters.statusQuick != StatusQuick.ALL
        val previewName = remember {
            "郑州机场货运航班动态_${DateTimes.fileStamp(System.currentTimeMillis())}.xlsx"
        }
        AlertDialog(
            onDismissRequest = { showExportConfirm = false },
            title = { Text("导出 xlsx") },
            text = {
                Text(
                    "将导出当前 ${state.visibleFlights.size} 条记录" +
                        (if (hasAnyFilter) "（已应用筛选条件）" else "") +
                        "。\n文件名：$previewName" +
                        "\n文件将保存到「下载/郑州机场航班动态」。"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showExportConfirm = false
                    viewModel.exportCurrent()
                }) {
                    Text("导出")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (state.exportInProgress) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("正在导出") },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("正在生成 xlsx…")
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
private fun LoadingState(completed: Int, total: Int) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(
            if (total > 0) "正在获取 $completed/$total 页…" else "正在加载航班数据…"
        )
    }
}

@Composable
private fun FullErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text("加载失败", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("重试") }
    }
}

@Composable
private fun EmptyState(
    hasAnyData: Boolean,
    onReset: () -> Unit,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (hasAnyData) "没有符合条件的航班" else "暂无航班数据",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(12.dp))
        if (hasAnyData) {
            OutlinedButton(onClick = onReset) { Text("重置筛选") }
        } else {
            Button(onClick = onRefresh) { Text("刷新") }
        }
    }
}
