package com.eveningoutpost.dexdrip.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import com.eveningoutpost.dexdrip.utilitymodels.ColorCacheBridge

/**
 * The xDrip Material 3 theme.
 *
 * On Android 12+ the color scheme derives from the device's dynamic color
 * (Material You); on older devices it falls back to the brand palette in
 * [Color.kt]. The scheme is intentionally limited to app chrome (surfaces,
 * typography, controls) — user-picked data colors are exposed separately via
 * [xdripColor].
 */
@Composable
fun XdripTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val inspectionMode = LocalInspectionMode.current
    val colorScheme = when {
        dynamicColor && !inspectionMode && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    val revision by ColorCacheBridge.revision.collectAsState()
    val resolvedScheme = remember(colorScheme, revision) { resolveXdripColorScheme(colorScheme) }

    MaterialTheme(
        colorScheme = resolvedScheme,
        typography = AppTypography,
    ) {
        CompositionLocalProvider(LocalXdripColors provides currentXdripColors()) {
            content()
        }
    }
}
