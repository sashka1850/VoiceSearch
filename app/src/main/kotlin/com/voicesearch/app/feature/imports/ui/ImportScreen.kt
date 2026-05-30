package com.voicesearch.app.feature.imports.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voicesearch.core.ui.theme.LocalElevation
import com.voicesearch.core.ui.theme.VoiceSearchTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    onBack: () -> Unit,
    onImportComplete: (tableId: String) -> Unit,
    viewModel: ImportViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val draft by viewModel.yandexLinkDraft.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(viewModel::importLocalFile) }

    LaunchedEffect(state) {
        when (val s = state) {
            is ImportUiState.Success -> {
                onImportComplete(s.tableId)
                viewModel.consumeSuccess()
            }
            is ImportUiState.Error -> {
                snackbar.showSnackbar(s.message)
                viewModel.consumeError()
            }
            else -> Unit
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Добавить таблицу") },
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
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Spacer(Modifier.height(8.dp))

                YandexLinkCard(
                    link = draft,
                    onLinkChange = viewModel::onYandexLinkChange,
                    onImport = viewModel::importYandexLink,
                    isLoading = state is ImportUiState.Loading,
                )

                LocalFileCard(
                    onPick = { filePicker.launch(arrayOf(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "text/csv",
                        "text/comma-separated-values",
                        "application/csv",
                        "text/plain",
                    )) },
                    isLoading = state is ImportUiState.Loading,
                )

                AnimatedVisibility(visible = state is ImportUiState.Loading) {
                    LoadingRow(label = (state as? ImportUiState.Loading)?.label.orEmpty())
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun YandexLinkCard(
    link: String,
    onLinkChange: (String) -> Unit,
    onImport: () -> Unit,
    isLoading: Boolean,
) {
    SourceCard(icon = Icons.Outlined.Link, title = "Ссылка с Яндекс.Диска") {
        Text(
            text = "Вставьте публичную ссылку. Файл скачается и сохранится в приложении.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = link,
            onValueChange = onLinkChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("https://disk.yandex.ru/i/...") },
            singleLine = true,
            enabled = !isLoading,
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onImport,
            enabled = link.isNotBlank() && !isLoading,
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) { Text("Скачать") }
    }
}

@Composable
private fun LocalFileCard(onPick: () -> Unit, isLoading: Boolean) {
    SourceCard(icon = Icons.Outlined.AttachFile, title = "Файл с устройства") {
        Text(
            text = "Поддерживаются .xlsx и .csv. Файл скопируется в хранилище приложения.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onPick,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) { Text("Выбрать файл") }
    }
}

@Composable
private fun SourceCard(
    icon: ImageVector,
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = LocalElevation.current.medium),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun LoadingRow(label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 4.dp),
    ) {
        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            text = label.ifBlank { "Загрузка…" },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Suppress("UnusedPrivateMember")
@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun ImportScreenIdlePreview() {
    VoiceSearchTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                YandexLinkCard(link = "", onLinkChange = {}, onImport = {}, isLoading = false)
                LocalFileCard(onPick = {}, isLoading = false)
            }
        }
    }
}

@Suppress("UnusedPrivateMember")
@Composable
@Preview(showBackground = true, widthDp = 360, heightDp = 720)
private fun ImportScreenSuccessPreview() {
    VoiceSearchTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row {
                    Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(8.dp))
                    Text("Импортировано")
                }
            }
        }
    }
}
