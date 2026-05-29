package com.voicesearch.core.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Brand palette.
 *
 * - Primary = blue. Used for app chrome, headers, neutral CTAs, links, info.
 * - Secondary (Action) = green. Used for the single dominant action surface per screen
 *   (mic FAB, "Найти", success markers). One accent so the user always knows where to look.
 * - Error = M3 red.
 *
 * Hex values picked from Material 3 tonal palettes so light/dark parity is consistent.
 */

// Blue (primary)
internal val Blue10 = Color(0xFF001A41)
internal val Blue20 = Color(0xFF002E69)
internal val Blue30 = Color(0xFF004494)
internal val Blue40 = Color(0xFF1B5FFF)
internal val Blue50 = Color(0xFF4A7BFF)
internal val Blue80 = Color(0xFFB4C5FF)
internal val Blue90 = Color(0xFFDCE1FF)
internal val Blue95 = Color(0xFFEDF0FF)

// Green (action / success)
internal val Green10 = Color(0xFF002111)
internal val Green20 = Color(0xFF00391F)
internal val Green30 = Color(0xFF005230)
internal val Green40 = Color(0xFF1FAB55)
internal val Green80 = Color(0xFF8CDBA8)
internal val Green90 = Color(0xFFADF2C5)

// Neutral
internal val Neutral10 = Color(0xFF1B1B1F)
internal val Neutral20 = Color(0xFF303034)
internal val Neutral90 = Color(0xFFE3E2E6)
internal val Neutral95 = Color(0xFFF2F0F4)
internal val Neutral99 = Color(0xFFFDFBFF)
internal val NeutralWhite = Color(0xFFFFFFFF)
internal val NeutralBlack = Color(0xFF000000)

// Neutral variant (outlines etc)
internal val NeutralVariant30 = Color(0xFF454751)
internal val NeutralVariant50 = Color(0xFF757680)
internal val NeutralVariant80 = Color(0xFFC5C6D0)
internal val NeutralVariant90 = Color(0xFFE1E2EC)

// Error
internal val Error40 = Color(0xFFBA1A1A)
internal val Error80 = Color(0xFFFFB4AB)
internal val Error90 = Color(0xFFFFDAD6)
internal val Error10 = Color(0xFF410002)
