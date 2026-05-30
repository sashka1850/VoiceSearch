package com.voicesearch.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.voicesearch.core.ui.components.PrimaryButton

@Suppress("LongMethod") // Single-screen sheet that wires header, list and action row.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultipleMatchesSheet(
    outcome: SearchOutcome.MultipleCandidates,
    onDismiss: () -> Unit,
    onApply: (Collection<Long>) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedIds by remember(outcome) {
        mutableStateOf<Set<Long>>(emptySet())
    }
    val allSelected = selectedIds.size == outcome.candidates.size && outcome.candidates.isNotEmpty()

    // Cap the candidate list at ~half the screen so the action row at the bottom
    // never gets pushed off-screen. The list itself scrolls within that band.
    val configuration = LocalConfiguration.current
    val maxListHeight = (configuration.screenHeightDp * LIST_MAX_FRACTION_OF_SCREEN).dp

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                text = "Найдено ${outcome.candidates.size} совпадений",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Распознано: «${outcome.spokenText}»",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        selectedIds = if (allSelected) emptySet()
                        else outcome.candidates.map { it.rowId }.toSet()
                    }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = allSelected, onCheckedChange = null)
                Spacer(Modifier.size(8.dp))
                Text(
                    text = "Выбрать все",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxListHeight),
            ) {
                items(items = outcome.candidates, key = { it.rowId }) { candidate ->
                    CandidateRow(
                        candidate = candidate,
                        checked = candidate.rowId in selectedIds,
                        onToggle = { checked ->
                            selectedIds = if (checked) selectedIds + candidate.rowId
                            else selectedIds - candidate.rowId
                        },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                ) { Text("Отмена") }

                PrimaryButton(
                    text = "Отметить (${selectedIds.size})",
                    onClick = {
                        onApply(selectedIds.toList())
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    enabled = selectedIds.isNotEmpty(),
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CandidateRow(candidate: Candidate, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!checked) }
            .background(
                if (candidate.isAlreadyMarked) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.surface,
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = onToggle)
        Spacer(Modifier.size(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = candidate.cellValue.ifBlank { "—" },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!candidate.name.isNullOrBlank()) {
                Text(
                    text = candidate.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (candidate.isAlreadyMarked) {
                Text(
                    text = "Уже отмечено",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

private const val LIST_MAX_FRACTION_OF_SCREEN = 0.5f
