package com.voicesearch.app.ui.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voicesearch.core.ui.components.ActionFab
import com.voicesearch.core.ui.components.PrimaryButton
import com.voicesearch.core.ui.theme.LocalElevation
import com.voicesearch.core.ui.theme.VoiceSearchTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddTable: () -> Unit,
    onOpenMenu: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    var pendingMicPress by remember { mutableStateOf(false) }

    val micPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted && pendingMicPress) {
            viewModel.pressMic()
        }
        pendingMicPress = false
    }

    fun onMicPress() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) {
            viewModel.pressMic()
        } else {
            pendingMicPress = true
            micPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(state.snackMessage()) {
        state.snackMessage()?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeOutcome()
        }
    }

    HomeScreenContent(
        onAddTable = onAddTable,
        onOpenMenu = onOpenMenu,
        state = state,
        callbacks = HomeScreenCallbacks(
            onMicPress = ::onMicPress,
            onMicRelease = viewModel::releaseMic,
            onSheetDismiss = viewModel::consumeOutcome,
            onApplySelection = viewModel::applyMultipleSelection,
            onToggleManualInput = viewModel::toggleManualInput,
            onManualInputChange = viewModel::onManualInputChange,
            onManualInputSubmit = viewModel::submitManualSearch,
            onManualInputDismiss = viewModel::dismissManualInput,
        ),
        snackbar = snackbar,
    )
}

/** Bundle of screen-level callbacks; lets HomeScreenContent stay under detekt's parameter cap. */
data class HomeScreenCallbacks(
    val onMicPress: () -> Unit = {},
    val onMicRelease: () -> Unit = {},
    val onSheetDismiss: () -> Unit = {},
    val onApplySelection: (Collection<Long>) -> Unit = {},
    val onToggleManualInput: () -> Unit = {},
    val onManualInputChange: (String) -> Unit = {},
    val onManualInputSubmit: () -> Unit = {},
    val onManualInputDismiss: () -> Unit = {},
)

@Suppress("LongMethod") // Compose entry-point that wires Scaffold + FAB overlays + sheet.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreenContent(
    onAddTable: () -> Unit,
    onOpenMenu: () -> Unit,
    state: HomeUiState,
    callbacks: HomeScreenCallbacks = HomeScreenCallbacks(),
    snackbar: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        topBar = {
            if (state is HomeUiState.Active) {
                TopAppBar(
                    title = {
                        Text(
                            text = state.tableName,
                            style = MaterialTheme.typography.titleLarge,
                        )
                    },
                    actions = {
                        IconButton(onClick = onOpenMenu) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Меню")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Manual-input strip lives at the top so the system keyboard
                // animating in doesn't shove other content around.
                if (state is HomeUiState.Active && state.manualInput.isVisible) {
                    ManualSearchBar(
                        text = state.manualInput.text,
                        onTextChange = callbacks.onManualInputChange,
                        onSubmit = callbacks.onManualInputSubmit,
                        onDismiss = callbacks.onManualInputDismiss,
                    )
                }

                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    when (state) {
                        HomeUiState.Empty -> EmptyState(onAddTable = onAddTable)
                        is HomeUiState.Active -> ActiveState(
                            state = state,
                            onMicPress = callbacks.onMicPress,
                            onMicRelease = callbacks.onMicRelease,
                        )
                    }
                }
            }

            // Two FABs as overlays. Scaffold's slot only supports one, so we
            // place both manually: keyboard fallback on the left, add-table on the right.
            if (state is HomeUiState.Active) {
                FloatingActionButton(
                    onClick = callbacks.onToggleManualInput,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = LocalElevation.current.high,
                        pressedElevation = LocalElevation.current.low,
                    ),
                    modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
                ) {
                    Icon(Icons.Outlined.Keyboard, contentDescription = "Поиск с клавиатуры")
                }

                FloatingActionButton(
                    onClick = onAddTable,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = LocalElevation.current.high,
                        pressedElevation = LocalElevation.current.low,
                    ),
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Добавить таблицу")
                }
            }
        }

        val outcome = (state as? HomeUiState.Active)?.lastOutcome
        if (outcome is SearchOutcome.MultipleCandidates) {
            MultipleMatchesSheet(
                outcome = outcome,
                onDismiss = callbacks.onSheetDismiss,
                onApply = callbacks.onApplySelection,
            )
        }
    }
}

@Composable
private fun ManualSearchBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = LocalElevation.current.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                placeholder = { Text("Введите значение для поиска") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    onSubmit()
                    keyboard?.hide()
                }),
                trailingIcon = {
                    if (text.isNotEmpty()) {
                        IconButton(onClick = { onTextChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Очистить")
                        }
                    }
                },
            )
            Spacer(Modifier.size(8.dp))
            IconButton(onClick = {
                keyboard?.hide()
                onDismiss()
            }) {
                Icon(Icons.Default.Close, contentDescription = "Закрыть поиск")
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
        PrimaryButton(text = "+ Добавить таблицу", onClick = onAddTable)
    }
}

@Composable
private fun ActiveState(
    state: HomeUiState.Active,
    onMicPress: () -> Unit,
    onMicRelease: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = state.hint.toDisplayString(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(32.dp))

        // Press-and-hold mic. detectTapGestures gives us onPress + tryAwaitRelease(),
        // covering both normal release (finger up) and gesture cancel.
        Box(
            modifier = Modifier.pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onMicPress()
                        tryAwaitRelease()
                        onMicRelease()
                    },
                )
            },
        ) {
            ActionFab(
                icon = Icons.Default.Mic,
                contentDescription = "Микрофон поиска",
                isActive = state.mic == MicState.Listening,
            )
        }

        Spacer(Modifier.height(20.dp))
        when (state.mic) {
            MicState.Idle -> Text(
                text = "Зажмите и говорите",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            MicState.Listening -> Text(
                text = "Слушаю…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            MicState.Processing -> Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(Modifier.height(0.dp))
                Spacer(Modifier.size(8.dp))
                Text(
                    text = "Ищем…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Persistent success/failure card below; snackbar would also work but a card sticks
        // around longer and matches the "found and marked" feedback from the MVP.
        val outcome = state.lastOutcome
        if (outcome is SearchOutcome.Marked) {
            Spacer(Modifier.height(24.dp))
            MarkedCard(outcome)
        }
    }
}

@Composable
private fun MarkedCard(outcome: SearchOutcome.Marked) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Отмечено",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = listOfNotNull(outcome.cellValue.takeIf { it.isNotBlank() }, outcome.name)
                        .joinToString(" — "),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

internal fun HomeUiState.snackMessage(): String? = when (val outcome = (this as? HomeUiState.Active)?.lastOutcome) {
    is SearchOutcome.NotFound -> "Не нашли: ${outcome.spokenText}"
    is SearchOutcome.Failed -> outcome.message
    SearchOutcome.NeedsSettings -> "Сначала настройте таблицу в меню «⋮»"
    else -> null
}

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
            state = HomeUiState.Active(
                tableName = "Прайс поставщика",
                tableId = "t1",
                hint = PromptHint.FixedSuffix(5),
                mic = MicState.Idle,
                manualInput = ManualInputState(),
                lastOutcome = null,
            ),
        )
    }
}
