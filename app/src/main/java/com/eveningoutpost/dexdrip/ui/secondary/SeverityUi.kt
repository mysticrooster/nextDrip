package com.eveningoutpost.dexdrip.ui.secondary

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Severity codes and their filter labels, shared by the Errors and Event Log screens. */
internal val severityLabels = listOf(1 to "Low", 2 to "Mid", 3 to "High", 5 to "EL", 6 to "EH")

@Composable
internal fun severityContainerColor(severity: Int): Color = when (severity) {
    1 -> MaterialTheme.colorScheme.tertiaryContainer
    2 -> MaterialTheme.colorScheme.secondaryContainer
    3 -> MaterialTheme.colorScheme.errorContainer
    5 -> MaterialTheme.colorScheme.primaryContainer
    6 -> MaterialTheme.colorScheme.surfaceVariant
    else -> MaterialTheme.colorScheme.surface
}

@Composable
internal fun severityTitleColor(severity: Int, background: Color): Color = when (severity) {
    1 -> MaterialTheme.colorScheme.onTertiaryContainer
    2 -> MaterialTheme.colorScheme.onSecondaryContainer
    3 -> MaterialTheme.colorScheme.onErrorContainer
    5 -> MaterialTheme.colorScheme.onPrimaryContainer
    else -> if (background == MaterialTheme.colorScheme.surfaceVariant) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurface
    }
}
