package com.eveningoutpost.dexdrip.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Shared harness for in-file `@Preview` composables.
 *
 * Wraps content in [XdripTheme] with dynamic color disabled so previews render
 * deterministically (see the `LocalInspectionMode` guard in [XdripTheme]) and
 * fills the background so light/dark variants are visually distinct.
 */
@Composable
fun XdripPreview(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    XdripTheme(darkTheme = darkTheme, dynamicColor = false) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            content()
        }
    }
}
