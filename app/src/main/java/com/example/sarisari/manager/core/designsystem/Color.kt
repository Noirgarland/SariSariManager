package com.example.sarisari.manager.core.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Brand palette: store-friendly green (primary), teal (secondary), warm amber (tertiary).

// Light
private val GreenPrimaryLight = Color(0xFF14513A)
private val GreenOnPrimaryLight = Color(0xFFFFFFFF)
private val GreenPrimaryContainerLight = Color(0xFFA6F4B4)
private val GreenOnPrimaryContainerLight = Color(0xFF00210D)
private val TealSecondaryLight = Color(0xFF006A6A)
private val TealOnSecondaryLight = Color(0xFFFFFFFF)
private val TealSecondaryContainerLight = Color(0xFF9CF1F0)
private val TealOnSecondaryContainerLight = Color(0xFF002020)
private val AmberTertiaryLight = Color(0xFF7A5900)
private val AmberOnTertiaryLight = Color(0xFFFFFFFF)
private val AmberTertiaryContainerLight = Color(0xFFFFE08A)
private val AmberOnTertiaryContainerLight = Color(0xFF261900)
private val ErrorLight = Color(0xFFBA1A1A)
private val OnErrorLight = Color(0xFFFFFFFF)
private val BackgroundLight = Color(0xFFFAF6EA)
private val OnBackgroundLight = Color(0xFF191C19)
private val SurfaceVariantLight = Color(0xFFDDE5DB)
private val OnSurfaceVariantLight = Color(0xFF414941)
private val OutlineLight = Color(0xFF717971)

// Dark
private val GreenPrimaryDark = Color(0xFF8BD79A)
private val GreenOnPrimaryDark = Color(0xFF00391B)
private val GreenPrimaryContainerDark = Color(0xFF00522A)
private val GreenOnPrimaryContainerDark = Color(0xFFA6F4B4)
private val TealSecondaryDark = Color(0xFF80D5D4)
private val TealOnSecondaryDark = Color(0xFF003737)
private val TealSecondaryContainerDark = Color(0xFF004F4F)
private val TealOnSecondaryContainerDark = Color(0xFF9CF1F0)
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
    secondary = TealSecondaryLight,
    onSecondary = TealOnSecondaryLight,
    secondaryContainer = TealSecondaryContainerLight,
    onSecondaryContainer = TealOnSecondaryContainerLight,
    tertiary = AmberTertiaryLight,
    onTertiary = AmberOnTertiaryLight,
    tertiaryContainer = AmberTertiaryContainerLight,
    onTertiaryContainer = AmberOnTertiaryContainerLight,
    error = ErrorLight,
    onError = OnErrorLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = BackgroundLight,
    onSurface = OnBackgroundLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight
)

internal val DarkBrandColorScheme: ColorScheme = darkColorScheme(
    primary = GreenPrimaryDark,
    onPrimary = GreenOnPrimaryDark,
    primaryContainer = GreenPrimaryContainerDark,
    onPrimaryContainer = GreenOnPrimaryContainerDark,
    secondary = TealSecondaryDark,
    onSecondary = TealOnSecondaryDark,
    secondaryContainer = TealSecondaryContainerDark,
    onSecondaryContainer = TealOnSecondaryContainerDark,
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
    outline = OutlineDark
)
