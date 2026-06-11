package com.voicesearch.app.ui.tableslist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voicesearch.core.ui.components.rememberTableGridColor
import com.voicesearch.core.ui.components.tableGridBackground
import java.util.concurrent.TimeUnit

@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TablesListScreen(
    onBack: () -> Unit,
    onTableChosen: () -> Unit,
    viewModel: TablesListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<TableListItem?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Мои таблицы") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        val gridColor = rememberTableGridColor()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .tableGridBackground(lineColor = gridColor),
        ) {
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.items.isEmpty() -> EmptyTablesList()
                else -> LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item { Spacer(Modifier.height(8.dp)) }
                    items(items = state.items, key = { it.table.id }) { item ->
                        SwipeableTableRow(
                            item = item,
                            onClick = {
                                viewModel.switchTo(item.table.id)
                                onTableChosen()
                            },
                            onSwipeDelete = { pendingDelete = item },
                        )
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }

    pendingDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Удалить таблицу?") },
            text = {
                Text(
                    text = "«${item.table.name}» и все её отметки будут удалены с устройства. " +
                        "Файл на Я.Диске не затрагивается.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(item.table.id)
                    pendingDelete = null
                }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Отмена") }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableTableRow(
    item: TableListItem,
    onClick: () -> Unit,
    onSwipeDelete: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onSwipeDelete()
                // false → the box settles back to neutral; the AlertDialog handles the
                // actual deletion so a stray swipe never hard-removes data.
                false
            } else {
                false
            }
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.large,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = "Удалить",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        },
    ) {
        TableRow(item = item, onClick = onClick)
    }
}

@Composable
private fun TableRow(item: TableListItem, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = scheme.surface,
        tonalElevation = if (item.isActive) 2.dp else 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Active indicator: green check pill on the left, blank circle otherwise.
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                val bg = if (item.isActive) scheme.secondary else scheme.surfaceVariant
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(color = bg, shape = CircleShape),
                )
                if (item.isActive) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Активная таблица",
                        tint = scheme.onSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.table.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = if (item.isActive) FontWeight.SemiBold else FontWeight.Normal,
                    ),
                    color = scheme.onSurface,
                )
                Spacer(Modifier.size(2.dp))
                Text(
                    text = "${item.table.rowCount} строк · ${item.markedCount} отмечено",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun EmptyTablesList() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.TableChart,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(64.dp),
        )
        Spacer(Modifier.size(16.dp))
        Text(
            text = "Пока ни одной таблицы",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.size(4.dp))
        Text(
            text = "Импортируйте файл или вставьте ссылку с Яндекс.Документов на главном экране",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Suppress("unused", "ReturnCount") // Future use for "imported N ago" caption.
private fun relativeTimeShort(thenMillis: Long, nowMillis: Long): String {
    val diff = nowMillis - thenMillis
    if (diff < TimeUnit.HOURS.toMillis(1)) return "только что"
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    if (hours < HOURS_IN_DAY) return "$hours ч назад"
    val days = TimeUnit.MILLISECONDS.toDays(diff)
    if (days < DAYS_IN_WEEK) return "$days дн назад"
    return "давно"
}

private const val HOURS_IN_DAY = 24
private const val DAYS_IN_WEEK = 7
