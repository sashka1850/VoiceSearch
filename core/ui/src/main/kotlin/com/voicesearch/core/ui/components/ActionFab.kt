package com.voicesearch.core.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.voicesearch.core.ui.theme.LocalElevation

/**
 * Large circular action surface — used once per screen as the primary
 * call-to-action (e.g. mic on the search screen).
 *
 * Visual: large circle, coloured container, contrasting icon, soft shadow that
 * grows when active. Press feedback is driven by the caller via [isActive].
 *
 * Colour: defaults to the green secondary (the historical "single dominant
 * action" colour). The mic on the new dock layout overrides this to the blue
 * [MaterialTheme.colorScheme.primary] so it reads as part of the blue control
 * cluster — pass [containerColor]/[contentColor] to do that. Existing call
 * sites keep the green default and are unaffected.
 */
@Composable
fun ActionFab(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    size: Dp = 96.dp,
    containerColor: Color = MaterialTheme.colorScheme.secondary,
    contentColor: Color = MaterialTheme.colorScheme.onSecondary,
) {
    val elevation = LocalElevation.current
    val tonalElevation by animateDpAsState(
        targetValue = if (isActive) elevation.pressed else elevation.floating,
        label = "ActionFab/tonalElevation",
    )

    Surface(
        modifier = modifier.size(size),
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
        shadowElevation = tonalElevation,
        tonalElevation = tonalElevation,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(size / 2.4f),
            )
        }
    }
}
