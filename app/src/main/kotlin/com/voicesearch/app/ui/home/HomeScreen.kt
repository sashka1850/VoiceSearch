package com.voicesearch.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voicesearch.core.ui.components.ActionFab
import com.voicesearch.core.ui.components.PrimaryButton
import com.voicesearch.core.ui.theme.LocalElevation
import com.voicesearch.core.ui.theme.VoiceSearchTheme

/**
 * Home screen.
 *
 * Two visual states for now:
 *  - **Empty** (no tables yet): centered illustration + a single "Добавить таблицу" CTA.
 *  - **With table** (placeholder for Stage 4): big green mic FAB centered, small "+" FAB
 *    in corner to add another table, top bar with table name and menu.
 *
 * Wired via the [HomeUiState] parameter — Stage 4 will plug a real ViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddTable: () -> Unit,
    onOpenMenu: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeScreenContent(
        onAddTable = onAddTable,
        onOpenMenu = onOpenMenu,
        state = state,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreenContent(
    onAddTable: () -> Unit,
    onOpenMenu: () -> Unit,
    state: HomeUiState,
) {
    Scaffold(
        topBar = {
            if (state is HomeUiState.WithTable) {
                TopAppBar(
                    title = {
                        Text(
                            text = state.tableName,
                            style = MaterialTheme.typography.titleLarge,
                        )
                    },
                    actions = {
                        IconButton(onClick = onOpenMenu) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Меню",
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                )
            }
        },
        floatingActionButton = {
            if (state is HomeUiState.WithTable) {
                FloatingActionButton(
                    onClick = onAddTable,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = LocalElevation.current.high,
                        pressedElevation = LocalElevation.current.low,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResourceOrNull("Добавить таблицу"),
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            when (state) {
                HomeUiState.Empty -> EmptyState(onAddTable = onAddTable)
                is HomeUiState.WithTable -> ActiveTableState(tableName = state.tableName)
            }
        }
    }
}

@Composable
private fun EmptyState(onAddTable: () -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            modifier = Modifier.size(96.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.TableChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(48.dp),
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(
            text = "Нет ни одной таблицы",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Добавьте первую, чтобы начать поиск",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(48.dp))
        PrimaryButton(
            text = "+ Добавить таблицу",
            onClick = onAddTable,
        )
    }
}

@Composable
private fun ActiveTableState(tableName: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Зажмите и говорите",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(32.dp))
        ActionFab(
            icon = Icons.Default.Mic,
            contentDescription = "Микрофон поиска",
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = tableName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// Small helper to keep contentDescription lint clean without pulling resources here yet.
private fun stringResourceOrNull(value: String): String = value

@Suppress("UnusedPrivateMember")
@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun HomeScreenEmptyPreview() {
    VoiceSearchTheme {
        HomeScreenContent(onAddTable = {}, onOpenMenu = {}, state = HomeUiState.Empty)
    }
}

@Suppress("UnusedPrivateMember")
@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun HomeScreenWithTablePreview() {
    VoiceSearchTheme {
        HomeScreenContent(
            onAddTable = {},
            onOpenMenu = {},
            state = HomeUiState.WithTable("Прайс поставщика"),
        )
    }
}
