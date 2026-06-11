package com.voicesearch.app.ui.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SyncDisabled
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.voicesearch.core.ui.components.rememberTableGridColor
import com.voicesearch.core.ui.components.tableGridBackground
import com.voicesearch.core.ui.theme.LocalElevation
import com.voicesearch.core.ui.theme.VoiceSearchTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddTable: () -> Unit,
    onOpenSettings: (tableId: String) -> Unit,
    onOpenAppSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    var pendingMicPress by remember { mutableStateOf(false) }

    // ACTION_SEND chooser — UI consumes one-shot events from the ViewModel.
    LaunchedEffect(Unit) {
        viewModel.shareEvents.collect { shareable ->
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = shareable.mimeType
                putExtra(Intent.EXTRA_STREAM, shareable.uri)
                putExtra(Intent.EXTRA_SUBJECT, shareable.displayName)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Отправить таблицу"))
        }
    }

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

    val activeTableId = (state as? HomeUiState.Active)?.tableId

    HomeScreenContent(
        onAddTable = onAddTable,
        onOpenSettings = { activeTableId?.let(onOpenSettings) },
        onOpenAppSettings = onOpenAppSettings,
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
            onShare = viewModel::shareCurrentTable,
            onConnectYandex = viewModel::connectYandex,
            onSyncNow = viewModel::syncNow,
            onSignOutYandex = viewModel::signOutYandex,
            onYandexCodeSubmit = viewModel::submitYandexCode,
            onYandexCodeCancel = viewModel::cancelYandexAuth,
            onToggleAutoSync = viewModel::toggleAutoSync,
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
    val onShare: () -> Unit = {},
    val onConnectYandex: () -> Unit = {},
    val onSyncNow: () -> Unit = {},
    val onSignOutYandex: () -> Unit = {},
    val onYandexCodeSubmit: (String) -> Unit = {},
    val onYandexCodeCancel: () -> Unit = {},
    val onToggleAutoSync: () -> Unit = {},
)

@Suppress("LongMethod") // Compose entry-point that wires Scaffold + topbar + content + dialogs.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreenContent(
    onAddTable: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAppSettings: () -> Unit,
    state: HomeUiState,
    callbacks: HomeScreenCallbacks = HomeScreenCallbacks(),
    snackbar: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        topBar = {
            if (state is HomeUiState.Active) {
                TopAppBar(
                    title = { Text(text = state.tableName, style = MaterialTheme.typography.titleLarge) },
                    actions = {
                        IconButton(onClick = callbacks.onToggleAutoSync) {
                            Icon(
                                imageVector = if (state.autoSyncEnabled) {
                                    Icons.Filled.Sync
                                } else {
                                    Icons.Outlined.SyncDisabled
                                },
                                contentDescription = if (state.autoSyncEnabled) {
                                    "Авто-синхронизация включена"
                                } else {
                                    "Авто-синхронизация выключена"
                                },
                                tint = if (state.autoSyncEnabled) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                        TableMenu(
                            yandexAuthenticated = state.yandexAuthenticated,
                            onShare = callbacks.onShare,
                            onOpenSettings = onOpenSettings,
                            onOpenAppSettings = onOpenAppSettings,
                            onConnectYandex = callbacks.onConnectYandex,
                            onSyncNow = callbacks.onSyncNow,
                            onSignOutYandex = callbacks.onSignOutYandex,
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                )
            }
        },
        // Manage the snackbar ourselves below so we can anchor it at the top.
        snackbarHost = {},
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        val gridColor = rememberTableGridColor()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .tableGridBackground(lineColor = gridColor),
        ) {
            when (state) {
                HomeUiState.Empty -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    EmptyState(onAddTable = onAddTable)
                }
                is HomeUiState.Active -> ActiveDock(
                    state = state,
                    onAddTable = onAddTable,
                    callbacks = callbacks,
                )
            }

            // Top-anchored snackbar: stays below the app bar, above any keyboard.
            SnackbarHost(
                hostState = snackbar,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        val outcome = (state as? HomeUiState.Active)?.lastOutcome
        if (outcome is SearchOutcome.MultipleCandidates) {
            MultipleMatchesSheet(
                outcome = outcome,
                onDismiss = callbacks.onSheetDismiss,
                onApply = callbacks.onApplySelection,
            )
        }

        if (state is HomeUiState.Active && state.awaitingYandexCode) {
            YandexCodeDialog(
                isSubmitting = state.yandexCodeSubmitting,
                onSubmit = callbacks.onYandexCodeSubmit,
                onCancel = callbacks.onYandexCodeCancel,
            )
        }
    }
}

@Composable
private fun YandexCodeDialog(
    isSubmitting: Boolean,
    onSubmit: (String) -> Unit,
    onCancel: () -> Unit,
) {
    var code by remember { mutableStateOf("") }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = { if (!isSubmitting) onCancel() },
        title = { Text("Введите код от Яндекса") },
        text = {
            Column {
                Text(
                    text = "После согласия Яндекс показал короткий код на странице. " +
                        "Скопируй и вставь его сюда.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.trim() },
                    placeholder = { Text("XXXXXXX") },
                    singleLine = true,
                    enabled = !isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = { onSubmit(code) },
                enabled = code.isNotBlank() && !isSubmitting,
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp),
                    )
                } else {
                    Text("Подтвердить")
                }
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(
                onClick = onCancel,
                enabled = !isSubmitting,
            ) { Text("Отмена") }
        },
    )
}

/**
 * Variant C — "Нижний док". The status stage (hint / result / mic caption)
 * floats in the upper area; the input field plus the blue control cluster sit
 * in a bottom dock within thumb reach. Pressing the keyboard FAB swaps the
 * control row for the in-app [NumericKeypad] — the field stays right above the
 * keys, never covered.
 *
 * For [PromptHint.FullValue] tables (no prefix) the keypad's limited charset
 * isn't enough, so we drop back to a system-IME [OutlinedTextField] in the
 * same slot.
 */
@Suppress("LongMethod") // Compose entry-point that wires the dock + status stage.
@Composable
private fun ActiveDock(
    state: HomeUiState.Active,
    onAddTable: () -> Unit,
    callbacks: HomeScreenCallbacks,
) {
    val slotInfo = state.hint.toSlotInfo()
    val typing = state.manualInput.isVisible
    val filled = if (typing) state.manualInput.text else state.voiceTranscript
    val status = fieldStatus(state.mic, state.lastOutcome, typing)

    // Tick once a minute so the "5 мин назад" caption stays fresh while the user
    // sits on the screen. produceState would also work; remember/LaunchedEffect
    // is cheaper for a once-a-minute pulse.
    var nowMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            kotlinx.coroutines.delay(NOW_TICK_MS)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // ── status stage ──
        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 24.dp),
            ) {
                // Info card always visible when the table is loaded — the
                // success notification flies as a top snackbar so it doesn't
                // shove the card around.
                if (state.info != null) {
                    TableInfoCard(info = state.info, nowMillis = nowMillis)
                    Spacer(Modifier.height(16.dp))
                }
                if (state.mic == MicState.Idle && state.lastOutcome == null && !typing) {
                    Text(
                        text = state.hint.toDisplayString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.height(16.dp))
                MicCaption(mic = state.mic)
            }
        }

        // ── bottom dock ──
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = LocalElevation.current.medium,
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                SearchFieldBox(
                    prefix = slotInfo?.prefix.orEmpty(),
                    filled = filled,
                    slotCount = slotInfo?.slots ?: 0,
                    status = status,
                )

                // Keypad slides up from the bottom (250 ms tween) when the
                // user opens manual input; on close it slides back down. Same
                // animation in reverse for the 3-button control cluster — they
                // exit the screen via the bottom edge so visually the dock
                // smoothly "swaps" content rather than popping.
                AnimatedVisibility(
                    visible = typing,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(DOCK_ANIMATION_MS),
                    ) + expandVertically(animationSpec = tween(DOCK_ANIMATION_MS)) + fadeIn(),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(DOCK_ANIMATION_MS),
                    ) + shrinkVertically(animationSpec = tween(DOCK_ANIMATION_MS)) + fadeOut(),
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            TextButton(onClick = callbacks.onManualInputDismiss) {
                                Text("Свернуть")
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                            }
                        }
                        if (slotInfo != null) {
                            NumericKeypad(
                                onKey = { key ->
                                    appendCapped(state.manualInput.text, key, slotInfo.slots, callbacks.onManualInputChange)
                                },
                                onBackspace = { callbacks.onManualInputChange(state.manualInput.text.dropLast(1)) },
                                onDone = callbacks.onManualInputSubmit,
                                canDone = state.manualInput.text.length == slotInfo.slots,
                            )
                        } else {
                            SystemKeyboardField(
                                text = state.manualInput.text,
                                onChange = callbacks.onManualInputChange,
                                onSubmit = callbacks.onManualInputSubmit,
                            )
                        }
                    }
                }
                AnimatedVisibility(
                    visible = !typing,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(DOCK_ANIMATION_MS),
                    ) + expandVertically(animationSpec = tween(DOCK_ANIMATION_MS)) + fadeIn(),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(DOCK_ANIMATION_MS),
                    ) + shrinkVertically(animationSpec = tween(DOCK_ANIMATION_MS)) + fadeOut(),
                ) {
                    DockControls(
                        onKeyboard = callbacks.onToggleManualInput,
                        onMicPress = callbacks.onMicPress,
                        onMicRelease = callbacks.onMicRelease,
                        micActive = state.mic == MicState.Listening,
                        onAddTable = onAddTable,
                    )
                }
            }
        }
    }
}

@Composable
private fun DockControls(
    onKeyboard: () -> Unit,
    onMicPress: () -> Unit,
    onMicRelease: () -> Unit,
    micActive: Boolean,
    onAddTable: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BlueFab(onClick = onKeyboard, icon = Icons.Outlined.Keyboard, description = "Поиск с клавиатуры")

        // Press-and-hold mic — blue, part of the control cluster. detectTapGestures
        // gives us onPress + tryAwaitRelease() (covers finger-up and gesture cancel).
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
                isActive = micActive,
                size = 84.dp,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        }

        BlueFab(onClick = onAddTable, icon = Icons.Default.Add, description = "Добавить таблицу")
    }
}

@Composable
private fun BlueFab(onClick: () -> Unit, icon: ImageVector, description: String) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = CircleShape,
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = LocalElevation.current.high,
            pressedElevation = LocalElevation.current.low,
        ),
    ) {
        Icon(icon, contentDescription = description)
    }
}

@Composable
private fun MicCaption(mic: MicState) {
    when (mic) {
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
            Spacer(Modifier.size(8.dp))
            Text(
                text = "Ищем…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * System-IME fallback used when there's no prefix to constrain input.
 * Auto-focuses on appear so the soft keyboard opens immediately; the IME
 * Search action submits.
 */
@Composable
private fun SystemKeyboardField(
    text: String,
    onChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    OutlinedTextField(
        value = text,
        onValueChange = onChange,
        modifier = Modifier
            .fillMaxWidth()
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
                IconButton(onClick = { onChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Очистить")
                }
            }
        },
    )
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
private fun TableMenu(
    yandexAuthenticated: Boolean,
    onShare: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onConnectYandex: () -> Unit,
    onSyncNow: () -> Unit,
    onSignOutYandex: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "Меню")
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text("Поделиться файлом") },
                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                onClick = { open = false; onShare() },
            )
            DropdownMenuItem(
                text = { Text("Настройки таблицы") },
                leadingIcon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                onClick = { open = false; onOpenSettings() },
            )
            DropdownMenuItem(
                text = { Text("Настройки приложения") },
                leadingIcon = { Icon(Icons.Outlined.Tune, contentDescription = null) },
                onClick = { open = false; onOpenAppSettings() },
            )
            if (yandexAuthenticated) {
                DropdownMenuItem(
                    text = { Text("Синхронизировать с Я.Диском") },
                    leadingIcon = { Icon(Icons.Default.CloudUpload, contentDescription = null) },
                    onClick = { open = false; onSyncNow() },
                )
                DropdownMenuItem(
                    text = { Text("Выйти из Я.Диска") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) },
                    onClick = { open = false; onSignOutYandex() },
                )
            } else {
                DropdownMenuItem(
                    text = { Text("Войти в Я.Диск") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null) },
                    onClick = { open = false; onConnectYandex() },
                )
            }
        }
    }
}

/**
 * Repaint cadence for the table info card's "X мин назад" caption.
 *
 * 15 s lets the boundary crossings ("только что" → "1 мин назад" → …) land
 * within ≤15 s of the real change. Compared to 60 s it's invisible on power
 * and well below the threshold of perceived staleness.
 */
private const val NOW_TICK_MS = 15_000L

/** Slide / expand timing for swapping the dock controls ↔ keypad. */
private const val DOCK_ANIMATION_MS = 250

private data class SlotInfo(val prefix: String, val slots: Int)

private fun PromptHint.toSlotInfo(): SlotInfo? = when (this) {
    is PromptHint.FixedSuffix -> SlotInfo(prefix = prefix, slots = length)
    is PromptHint.VariableSuffix -> SlotInfo(prefix = prefix, slots = maxLength)
    PromptHint.FullValue, PromptHint.NotConfigured -> null
}

internal fun HomeUiState.snackMessage(): String? = when (val outcome = (this as? HomeUiState.Active)?.lastOutcome) {
    is SearchOutcome.Marked -> {
        val tail = listOfNotNull(
            outcome.cellValue.takeIf { it.isNotBlank() },
            outcome.name?.takeIf { it.isNotBlank() },
        ).joinToString(" — ")
        if (tail.isEmpty()) "Отмечено" else "Отмечено: $tail"
    }
    is SearchOutcome.NotFound -> "Не нашли: ${outcome.spokenText}"
    is SearchOutcome.Failed -> outcome.message
    SearchOutcome.NeedsSettings -> "Сначала настройте таблицу в меню «⋮»"
    else -> null
}

@Suppress("UnusedPrivateMember")
@Preview(showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun HomeScreenEmptyPreview() {
    VoiceSearchTheme {
        HomeScreenContent(
            onAddTable = {},
            onOpenSettings = {},
            onOpenAppSettings = {},
            state = HomeUiState.Empty,
        )
    }
}

@Suppress("UnusedPrivateMember")
@Preview(showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun HomeScreenDockPreview() {
    VoiceSearchTheme {
        HomeScreenContent(
            onAddTable = {},
            onOpenSettings = {},
            onOpenAppSettings = {},
            state = HomeUiState.Active(
                tableName = "Прайс поставщика",
                tableId = "t1",
                hint = PromptHint.FixedSuffix(prefix = "52ОМ139W", length = 6),
                mic = MicState.Idle,
                manualInput = ManualInputState(),
                lastOutcome = SearchOutcome.Marked(cellValue = "52ОМ139W204517", name = "Шланг РВД 2SN DN12"),
                yandexAuthenticated = false,
                voiceTranscript = "204517",
                awaitingYandexCode = false,
                yandexCodeSubmitting = false,
                autoSyncEnabled = false,
                info = com.voicesearch.core.domain.model.TableInfo(
                    tableId = "t1",
                    totalRows = 1547,
                    markedRows = 23,
                    syncedMarkedRows = 23,
                    sync = com.voicesearch.core.domain.model.TableSyncStatus(
                        lastSuccessAt = System.currentTimeMillis() - 120_000L,
                        lastAttemptAt = System.currentTimeMillis() - 120_000L,
                        lastError = null,
                    ),
                ),
            ),
        )
    }
}
