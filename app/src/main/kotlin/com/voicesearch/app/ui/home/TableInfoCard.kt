package com.voicesearch.app.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.TableRows
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.voicesearch.core.domain.model.TableInfo
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

/**
 * Inline summary of the active table:
 *
 *   📊  Всего: 1547
 *   ✅  Отмечено: 23
 *   ☁️  Синхронизировано: 23
 *
 *   Синхронизация: 2 мин назад
 *
 * The "synced" line is the live channel for sync feedback. On a successful
 * upload it briefly washes the line green; on a failure the whole card border
 * fades red and the caption shows the humanised error message until the next
 * attempt overwrites it.
 */
@Suppress("LongMethod") // Composable layout that wires three metric rows + caption — splitting buys nothing.
@Composable
fun TableInfoCard(
    info: TableInfo,
    nowMillis: Long,
    yandexAuthenticated: Boolean,
    modifier: Modifier = Modifier,
) {
    // Pulse green for exactly GREEN_PULSE_MS after a *new* successful sync
    // lands. Driven by a dedicated coroutine, NOT the nowMillis clock — that
    // ticks every 15 s and would leave the wash on for much longer than intended.
    var pulsing by remember(info.tableId) { mutableStateOf(false) }
    val initialLastSuccess = remember(info.tableId) { info.sync.lastSuccessAt }
    LaunchedEffect(info.tableId, info.sync.lastSuccessAt) {
        val current = info.sync.lastSuccessAt
        if (current != null && current != initialLastSuccess) {
            pulsing = true
            delay(GREEN_PULSE_MS)
            pulsing = false
        }
    }

    val scheme = MaterialTheme.colorScheme
    val showError = info.sync.lastError != null

    val targetBorder = when {
        showError -> scheme.error
        pulsing -> scheme.secondary
        else -> scheme.outlineVariant
    }
    val borderColor by animateColorAsState(
        targetValue = targetBorder,
        animationSpec = tween(durationMillis = BORDER_FADE_MS),
        label = "TableInfoCard/border",
    )

    val targetContainer = when {
        showError -> scheme.errorContainer.copy(alpha = 0.45f)
        pulsing -> scheme.secondaryContainer.copy(alpha = 0.45f)
        else -> scheme.surface
    }
    val containerColor by animateColorAsState(
        targetValue = targetContainer,
        animationSpec = tween(durationMillis = BORDER_FADE_MS),
        label = "TableInfoCard/container",
    )

    val shape = RoundedCornerShape(20.dp)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = containerColor,
    ) {
        Box(
            modifier = Modifier
                .border(1.5.dp, borderColor, shape)
                .padding(horizontal = 18.dp, vertical = 14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricRow(
                    icon = Icons.Outlined.TableRows,
                    iconTint = scheme.onSurfaceVariant,
                    label = "Всего",
                    value = info.totalRows.toString(),
                )
                MetricRow(
                    icon = Icons.Filled.CheckCircle,
                    iconTint = scheme.secondary,
                    label = "Отмечено",
                    value = info.markedRows.toString(),
                )
                SyncMetricRow(
                    state = when {
                        showError || !yandexAuthenticated -> SyncIndicatorState.NoConnection
                        pulsing -> SyncIndicatorState.Syncing
                        else -> SyncIndicatorState.Connected
                    },
                    value = info.syncedMarkedRows.toString(),
                    valueColor = when {
                        showError -> scheme.error
                        pulsing -> scheme.secondary
                        else -> scheme.onSurface
                    },
                )

                Spacer(Modifier.height(2.dp))

                SyncCaption(
                    info = info,
                    nowMillis = nowMillis,
                    showError = showError,
                )
            }
        }
    }
}

/**
 * Same row shape as [MetricRow] but the leading 28dp slot hosts the Lottie
 * sync indicator instead of a static icon. We don't tint the Lottie — its
 * own animation carries the state (offline → cloud → spin).
 */
@Composable
private fun SyncMetricRow(
    state: SyncIndicatorState,
    value: String,
    valueColor: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        SyncStatusLottie(
            state = state,
            modifier = Modifier.size(28.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = "Синхронизировано",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = valueColor,
        )
    }
}

@Composable
private fun MetricRow(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(
                    color = iconTint.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = valueColor,
        )
    }
}

@Composable
private fun SyncCaption(
    info: TableInfo,
    nowMillis: Long,
    showError: Boolean,
) {
    val lastSuccess = info.sync.lastSuccessAt
    val text = when {
        showError -> info.sync.lastError.orEmpty()
        lastSuccess != null -> "Синхронизация: ${relativeTime(lastSuccess, nowMillis)}"
        else -> "Синхронизаций ещё не было"
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = if (showError) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
    )
}

/** "только что" / "5 мин назад" / "2 ч назад" / "3 дн назад" / "давно". */
@Suppress("ReturnCount") // One short branch per time bucket reads better than a `when` chain here.
private fun relativeTime(thenMillis: Long, nowMillis: Long): String {
    val diffMs = nowMillis - thenMillis
    if (diffMs < TimeUnit.MINUTES.toMillis(1)) return "только что"
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMs)
    if (minutes < FIFTY_NINE) return "$minutes мин назад"
    val hours = TimeUnit.MILLISECONDS.toHours(diffMs)
    if (hours < HOURS_IN_DAY) return "$hours ч назад"
    val days = TimeUnit.MILLISECONDS.toDays(diffMs)
    if (days < DAYS_IN_WEEK) return "$days дн назад"
    return "давно"
}

private const val GREEN_PULSE_MS = 1600L
private const val BORDER_FADE_MS = 450
private const val FIFTY_NINE = 60
private const val HOURS_IN_DAY = 24
private const val DAYS_IN_WEEK = 7
