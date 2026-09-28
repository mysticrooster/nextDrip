package com.eveningoutpost.dexdrip.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Brand fallback palette used on devices that do not support dynamic color
 * (Android < 12). On Android 12+ the theme derives from the device's dynamic
 * color scheme instead.
 */

private val PrimaryLight = Color(0xFF00658E)
private val OnPrimaryLight = Color(0xFFFFFFFF)
private val PrimaryContainerLight = Color(0xFFC8E6FF)
private val OnPrimaryContainerLight = Color(0xFF001E30)
private val SecondaryLight = Color(0xFF4F616E)
private val OnSecondaryLight = Color(0xFFFFFFFF)
private val SecondaryContainerLight = Color(0xFFD3E5F5)
private val OnSecondaryContainerLight = Color(0xFF0B1D29)
private val TertiaryLight = Color(0xFF62597C)
private val OnTertiaryLight = Color(0xFFFFFFFF)
private val TertiaryContainerLight = Color(0xFFE8DEFF)
private val OnTertiaryContainerLight = Color(0xFF1E1635)
private val ErrorLight = Color(0xFFBA1A1A)
private val OnErrorLight = Color(0xFFFFFFFF)
private val ErrorContainerLight = Color(0xFFFFDAD6)
private val OnErrorContainerLight = Color(0xFF410002)
private val BackgroundLight = Color(0xFFF7F9FF)
private val OnBackgroundLight = Color(0xFF181C20)
private val SurfaceLight = Color(0xFFF7F9FF)
private val OnSurfaceLight = Color(0xFF181C20)
private val SurfaceVariantLight = Color(0xFFDDE3EA)
private val OnSurfaceVariantLight = Color(0xFF41484D)
private val OutlineLight = Color(0xFF71787E)

private val PrimaryDark = Color(0xFF86CEFF)
private val OnPrimaryDark = Color(0xFF00344D)
private val PrimaryContainerDark = Color(0xFF004C6E)
private val OnPrimaryContainerDark = Color(0xFFC8E6FF)
private val SecondaryDark = Color(0xFFB7C9D8)
private val OnSecondaryDark = Color(0xFF22323F)
private val SecondaryContainerDark = Color(0xFF384956)
private val OnSecondaryContainerDark = Color(0xFFD3E5F5)
private val TertiaryDark = Color(0xFFCCC2E9)
private val OnTertiaryDark = Color(0xFF342C4C)
private val TertiaryContainerDark = Color(0xFF4A4264)
private val OnTertiaryContainerDark = Color(0xFFE8DEFF)
private val ErrorDark = Color(0xFFFFB4AB)
private val OnErrorDark = Color(0xFF690005)
private val ErrorContainerDark = Color(0xFF93000A)
private val OnErrorContainerDark = Color(0xFFFFDAD6)
private val BackgroundDark = Color(0xFF101417)
private val OnBackgroundDark = Color(0xFFE1E3E6)
private val SurfaceDark = Color(0xFF101417)
private val OnSurfaceDark = Color(0xFFE1E3E6)
private val SurfaceVariantDark = Color(0xFF41484D)
private val OnSurfaceVariantDark = Color(0xFFC0C7CE)
private val OutlineDark = Color(0xFF8A9298)

internal val LightColors = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
)

internal val DarkColors = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
)
