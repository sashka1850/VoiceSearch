package com.voicesearch.app.feature.tablesettings.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voicesearch.core.domain.model.PrefixHint
import com.voicesearch.core.ui.components.PrimaryButton
import com.voicesearch.core.ui.theme.LocalElevation

@Suppress("LongMethod") // Compose entry-point that wires Scaffold + body; extracted parts already inlined as composables.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableSettingsScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: TableSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.savedTableId) {
        if (state.savedTableId != null) {
            viewModel.consumeSaved()
            onSaved()
        }
    }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = state.tableName.ifBlank { "Настройки таблицы" }) },
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
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PreviewCard(state)
                ColumnPickerCard(
                    label = "Столбец поиска",
                    headers = state.headers,
                    selectedIndex = state.searchColumnIndex,
                    onSelect = viewModel::onSearchColumnChange,
                )
                PrefixHintCard(state.prefixHint)
                ColumnPickerCard(
                    label = "Столбец наименования",
                    headers = state.headers,
                    selectedIndex = state.nameColumnIndex,
                    onSelect = viewModel::onNameColumnChange,
                )
                ColumnPickerCard(
                    label = "Столбец отметки",
                    headers = state.headers,
                    selectedIndex = state.markColumnIndex,
                    onSelect = viewModel::onMarkColumnChange,
                )
                MarkerCard(value = state.successMarker, onChange = viewModel::onSuccessMarkerChange)
                StartRowCard(value = state.startRowIndex, onChange = viewModel::onStartRowChange)
                if (state.autoSyncSupported) {
                    AutoSyncCard(enabled = state.autoSync, onChange = viewModel::onAutoSyncChange)
                }
                Spacer(Modifier.height(72.dp)) // room for sticky save button
            }

            PrimaryButton(
                text = if (state.isSaving) "Сохраняем…" else "Сохранить",
                onClick = viewModel::save,
                enabled = !state.isSaving,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
            )
        }
    }
}

@Composable
private fun PreviewCard(state: TableSettingsUiState) {
    SettingsCard(label = "Превью первых строк") {
        if (state.headers.isEmpty()) {
            Text("Пусто", style = MaterialTheme.typography.bodyMedium)
            return@SettingsCard
        }
        val horizontalScroll = rememberScrollState()
        Column(modifier = Modifier.horizontalScroll(horizontalScroll)) {
            Row(modifier = Modifier.padding(vertical = 4.dp)) {
                state.headers.forEach { h ->
                    Text(
                        text = h.ifBlank { "—" },
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.widthIn(min = 100.dp).padding(end = 12.dp),
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            state.previewRows.forEach { row ->
                Row(modifier = Modifier.padding(vertical = 4.dp)) {
                    state.headers.indices.forEach { idx ->
                        Text(
                            text = row.getOrNull(idx).orEmpty().ifBlank { "—" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.widthIn(min = 100.dp).padding(end = 12.dp),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColumnPickerCard(
    label: String,
    headers: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    SettingsCard(label = label) {
        var expanded by remember { mutableStateOf(false) }
        val current = headers.getOrNull(selectedIndex)?.ifBlank { "Колонка ${selectedIndex + 1}" }
            ?: "Колонка ${selectedIndex + 1}"
        Box {
            OutlinedTextField(
                value = current,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                },
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.heightIn(max = 320.dp),
            ) {
                headers.forEachIndexed { index, header ->
                    DropdownMenuItem(
                        text = { Text(text = header.ifBlank { "Колонка ${index + 1}" }) },
                        onClick = {
                            onSelect(index)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun PrefixHintCard(hint: PrefixHint) {
    SettingsCard(label = "Подсказка для голосового поиска") {
        val (title, body) = when (hint) {
            is PrefixHint.FixedSuffix -> Pair(
                "Поиск по последним ${hint.suffixLength} симв.",
                "Префикс «${hint.prefix}» одинаков у всех строк — назовите оставшуюся часть.",
            )
            is PrefixHint.VariableSuffix -> Pair(
                "Поиск по последним до ${hint.maxSuffixLength} симв.",
                "Префикс «${hint.prefix}» совпадает у большинства строк, длины отличаются.",
            )
            PrefixHint.FullMatch -> Pair(
                "Поиск по полному значению",
                "Общего префикса не нашлось — назовите значение целиком.",
            )
        }
        Text(text = title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(4.dp))
        Text(text = body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MarkerCard(value: String, onChange: (String) -> Unit) {
    SettingsCard(label = "Маркер успеха") {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Есть") },
        )
    }
}

@Composable
private fun StartRowCard(value: Int, onChange: (Int) -> Unit) {
    SettingsCard(label = "Начинать поиск со строки") {
        OutlinedTextField(
            value = (value + 1).toString(), // show as 1-based to the user
            onValueChange = { raw ->
                val n = raw.toIntOrNull()
                if (n != null && n >= 1) onChange(n - 1)
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
        )
    }
}

@Composable
private fun AutoSyncCard(enabled: Boolean, onChange: (Boolean) -> Unit) {
    SettingsCard(label = "Авто-синхронизация с Я.Диском") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Загружать изменения после каждой отметки",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Switch(checked = enabled, onCheckedChange = onChange)
        }
    }
}

@Composable
private fun SettingsCard(label: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = LocalElevation.current.medium),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            content()
        }
    }
}

