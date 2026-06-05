@file:Suppress("MatchingDeclarationName") // SearchFieldBox (function) is the primary; FieldStatus is its parameter type.

package com.voicesearch.app.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Border / wash state surfaced by [SearchFieldBox]. */
enum class FieldStatus { Idle, Active, Success, Error }

/**
 * The hero input element.
 *
 * A rounded field that shows `prefix + masked slots` ([PrefixedSlotsRow]) and
 * — crucially — *communicates the search result by colour*: its border fades
 * softly to green on a successful match and red on a miss, then back to
 * neutral. The fade is a 450ms tween so it reads as a calm pulse, never a
 * harsh flash.
 *
 * Status is derived in the UI from existing state (mic + lastOutcome) via
 * [fieldStatus]; no ViewModel change is required.
 */
@Composable
fun SearchFieldBox(
    prefix: String,
    filled: String,
    slotCount: Int,
    status: FieldStatus,
    modifier: Modifier = Modifier,
    placeholder: String = "●",
) {
    val scheme = MaterialTheme.colorScheme

    val targetBorder = when (status) {
        FieldStatus.Success -> scheme.secondary
        FieldStatus.Error -> scheme.error
        FieldStatus.Active -> scheme.primary
        FieldStatus.Idle -> scheme.outlineVariant
    }
    val borderColor by animateColorAsState(
        targetValue = targetBorder,
        animationSpec = tween(durationMillis = BORDER_TWEEN_MS),
        label = "SearchFieldBox/border",
    )

    // A faint container wash that tints with the same result colour — keeps the
    // success/error read legible without shouting.
    val targetContainer = when (status) {
        FieldStatus.Success -> scheme.secondaryContainer.copy(alpha = 0.45f)
        FieldStatus.Error -> scheme.errorContainer.copy(alpha = 0.40f)
        else -> scheme.surfaceVariant.copy(alpha = 0.50f)
    }
    val containerColor by animateColorAsState(
        targetValue = targetContainer,
        animationSpec = tween(durationMillis = BORDER_TWEEN_MS),
        label = "SearchFieldBox/container",
    )

    val shape = MaterialTheme.shapes.large

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = containerColor,
    ) {
        Box(
            modifier = Modifier
                .border(width = 1.5.dp, color = borderColor, shape = shape)
                .heightIn(min = 64.dp)
                .padding(horizontal = 18.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (slotCount > 0) {
                PrefixedSlotsRow(
                    prefix = prefix,
                    filled = filled,
                    slotCount = slotCount,
                    placeholder = placeholder,
                )
            } else {
                // FullValue mode (no fixed prefix/slots): show the raw text.
                Text(
                    text = filled.ifBlank { "Назовите значение" },
                    color = if (filled.isBlank()) scheme.outline else scheme.onSurface,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 22.sp,
                )
            }
        }
    }
}

/**
 * Maps the existing screen state to a [FieldStatus]. Keep this the single
 * source of truth so voice and keyboard paths glow identically.
 */
fun fieldStatus(
    mic: MicState,
    lastOutcome: SearchOutcome?,
    manualInputVisible: Boolean,
): FieldStatus = when (lastOutcome) {
    is SearchOutcome.Marked -> FieldStatus.Success
    is SearchOutcome.NotFound -> FieldStatus.Error
    is SearchOutcome.Failed -> FieldStatus.Error
    else -> if (mic == MicState.Listening || manualInputVisible) FieldStatus.Active else FieldStatus.Idle
}

private const val BORDER_TWEEN_MS = 450
