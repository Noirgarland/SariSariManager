package com.example.sarisari.manager.core.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Brand palette: deep store green (primary), fresh green (secondary), warm amber (tertiary).
// The light scheme uses white cards on a very light green-gray background for a bright, clean look.

// Light
private val GreenPrimaryLight = Color(0xFF1B5E3B)
private val GreenOnPrimaryLight = Color(0xFFFFFFFF)
private val GreenPrimaryContainerLight = Color(0xFFDDF3E4)
private val GreenOnPrimaryContainerLight = Color(0xFF0E3B24)
private val GreenSecondaryLight = Color(0xFF2E7D4F)
private val GreenOnSecondaryLight = Color(0xFFFFFFFF)
private val GreenSecondaryContainerLight = Color(0xFFE8F5EC)
private val GreenOnSecondaryContainerLight = Color(0xFF14432B)
private val AmberTertiaryLight = Color(0xFF8A5A00)
private val AmberOnTertiaryLight = Color(0xFFFFFFFF)
private val AmberTertiaryContainerLight = Color(0xFFFFE7A8)
private val AmberOnTertiaryContainerLight = Color(0xFF5A3A00)
private val ErrorLight = Color(0xFFC62828)
private val OnErrorLight = Color(0xFFFFFFFF)
private val ErrorContainerLight = Color(0xFFFDE2E1)
private val OnErrorContainerLight = Color(0xFF8C1D18)
private val BackgroundLight = Color(0xFFF4F7F4)
private val OnBackgroundLight = Color(0xFF17201B)
private val SurfaceLight = Color(0xFFFFFFFF)
private val SurfaceVariantLight = Color(0xFFEEF3EF)
private val OnSurfaceVariantLight = Color(0xFF55615A)
private val OutlineLight = Color(0xFF8A968F)
private val OutlineVariantLight = Color(0xFFDDE5DF)

// Dark
private val GreenPrimaryDark = Color(0xFF8BD79A)
private val GreenOnPrimaryDark = Color(0xFF00391B)
private val GreenPrimaryContainerDark = Color(0xFF00522A)
private val GreenOnPrimaryContainerDark = Color(0xFFA6F4B4)
private val GreenSecondaryDark = Color(0xFF9CD6AE)
private val GreenOnSecondaryDark = Color(0xFF00391E)
private val GreenSecondaryContainerDark = Color(0xFF1F4D32)
private val GreenOnSecondaryContainerDark = Color(0xFFB8F0C8)
private val AmberTertiaryDark = Color(0xFFF5BF48)
private val AmberOnTertiaryDark = Color(0xFF402D00)
private val AmberTertiaryContainerDark = Color(0xFF5C4300)
private val AmberOnTertiaryContainerDark = Color(0xFFFFDEA1)
private val ErrorDark = Color(0xFFFFB4AB)
private val OnErrorDark = Color(0xFF690005)
private val BackgroundDark = Color(0xFF191C19)
private val OnBackgroundDark = Color(0xFFE1E3DD)
private val SurfaceVariantDark = Color(0xFF414941)
private val OnSurfaceVariantDark = Color(0xFFC1C9BF)
private val OutlineDark = Color(0xFF8B938A)

internal val LightBrandColorScheme: ColorScheme = lightColorScheme(
    primary = GreenPrimaryLight,
    onPrimary = GreenOnPrimaryLight,
    primaryContainer = GreenPrimaryContainerLight,
    onPrimaryContainer = GreenOnPrimaryContainerLight,
    secondary = GreenSecondaryLight,
    onSecondary = GreenOnSecondaryLight,
    secondaryContainer = GreenSecondaryContainerLight,
    onSecondaryContainer = GreenOnSecondaryContainerLight,
    tertiary = AmberTertiaryLight,
    onTertiary = AmberOnTertiaryLight,
    tertiaryContainer = AmberTertiaryContainerLight,
    onTertiaryContainer = AmberOnTertiaryContainerLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = BackgroundLight,
    onSurface = OnBackgroundLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    surfaceContainerLowest = SurfaceLight,
    surfaceContainerLow = SurfaceLight,
    surfaceContainer = SurfaceLight,
    surfaceContainerHigh = SurfaceLight,
    surfaceContainerHighest = SurfaceVariantLight,
)

internal val DarkBrandColorScheme: ColorScheme = darkColorScheme(
    primary = GreenPrimaryDark,
    onPrimary = GreenOnPrimaryDark,
    primaryContainer = GreenPrimaryContainerDark,
    onPrimaryContainer = GreenOnPrimaryContainerDark,
    secondary = GreenSecondaryDark,
    onSecondary = GreenOnSecondaryDark,
    secondaryContainer = GreenSecondaryContainerDark,
    onSecondaryContainer = GreenOnSecondaryContainerDark,
    tertiary = AmberTertiaryDark,
    onTertiary = AmberOnTertiaryDark,
    tertiaryContainer = AmberTertiaryContainerDark,
    onTertiaryContainer = AmberOnTertiaryContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = BackgroundDark,
    onSurface = OnBackgroundDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
)
