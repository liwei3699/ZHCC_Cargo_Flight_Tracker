package com.zzcargo.flightboard.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zzcargo.flightboard.domain.SortDirection
import com.zzcargo.flightboard.domain.SortField
import com.zzcargo.flightboard.domain.SortState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortSheet(
    current: SortState,
    onDismiss: () -> Unit,
    onApply: (SortState) -> Unit
) {
    var draft by remember { mutableStateOf(current) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("排序", style = MaterialTheme.typography.titleLarge)
            Text(
                "默认排序为航班日期 + 计划起飞时间；空值始终排最后。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SortDirection.values().forEach { direction ->
                    FilterChip(
                        selected = draft.direction == direction,
                        onClick = { draft = draft.copy(direction = direction) },
                        label = { Text(direction.label) }
                    )
                }
            }
            SortField.values().forEach { field ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { draft = draft.copy(field = field) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = draft.field == field,
                        onClick = { draft = draft.copy(field = field) }
                    )
                    Text(field.label)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("取消")
                }
                Button(onClick = { onApply(draft) }, modifier = Modifier.weight(1f)) {
                    Text("应用")
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
