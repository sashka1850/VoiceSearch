package com.voicesearch.core.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Elevation tokens — the "parение" effect.
 *
 * | Level     | Use case                                |
 * |-----------|-----------------------------------------|
 * | none      | Background, dividers                    |
 * | low       | Fields, inactive chips                  |
 * | medium    | Cards, dropdowns                        |
 * | high      | Primary buttons (Найти, Save)           |
 * | floating  | FAB, mic in active state                |
 *
 * In Compose: pass to Card(elevation = ...), or as `shadow(elevation, shape)` on Box.
 */
data class VoiceSearchElevation(
    val none: Dp = 0.dp,
    val low: Dp = 2.dp,
    val medium: Dp = 4.dp,
    val high: Dp = 6.dp,
    val floating: Dp = 8.dp,
    val pressed: Dp = 12.dp,
)

val LocalElevation = compositionLocalOf { VoiceSearchElevation() }
