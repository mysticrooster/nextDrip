@file:JvmName("BgReadingTableScreen")

package com.eveningoutpost.dexdrip.ui.secondary

import android.content.res.Configuration
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.databinding.Observable
import com.eveningoutpost.dexdrip.R
import com.eveningoutpost.dexdrip.models.BgReading
import com.eveningoutpost.dexdrip.models.JoH
import com.eveningoutpost.dexdrip.tables.BgReadingTable
import com.eveningoutpost.dexdrip.ui.theme.XdripPreview
import com.eveningoutpost.dexdrip.utilitymodels.BgGraphBuilder
import java.util.Date

/**
 * Track V (Data tables) — `BgReadingTable`: raw BG reading rows with a long-press flag-for-stats
 * action. The activity loads the readings and persists the flag; the screen renders them. Java
 * entry point: [installBgReadingTable].
 */
fun installBgReadingTable(activity: BgReadingTable) {
    activity.setContent {
        var tick by remember { mutableStateOf(activity.tick.get() ?: 0) }
        DisposableEffect(activity) {
            val callback = object : Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: Observable, propertyId: Int) {
                    tick = activity.tick.get() ?: 0
                }
            }
            activity.tick.addOnPropertyChangedCallback(callback)
            onDispose { activity.tick.removeOnPropertyChangedCallback(callback) }
        }

        BgReadingTableScreen(
            subtitle = remember(tick) { activity.subtitle.get().orEmpty() },
            readings = remember(tick) { activity.readingsSnapshot },
            onFlag = { reading, ignore -> activity.flagReading(reading, ignore) },
            onBack = { activity.finish() },
        )
    }
}

@Composable
internal fun BgReadingTableScreen(
    subtitle: String,
    readings: List<BgReading>,
    onFlag: (BgReading, Boolean) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var dialogReading by remember { mutableStateOf<BgReading?>(null) }

    SecondaryScreenList(title = "BG Data Table", onBack = onBack) {
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .testTag("bg_table_subtitle"),
        )
        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
            items(readings) { reading ->
                BgReadingRow(reading) { dialogReading = reading }
            }
        }
    }

    dialogReading?.let { reading ->
        AlertDialog(
            onDismissRequest = { dialogReading = null },
            title = { Text("Flag reading") },
            text = { Text("Flag reading as \"bad\".\nFlagged readings have no impact on the statistics.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        dialogReading = null
                        onFlag(reading, true)
                    },
                ) {
                    Text(context.getString(R.string.yes))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        dialogReading = null
                        onFlag(reading, false)
                    },
                ) {
                    Text(context.getString(R.string.no))
                }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BgReadingRow(reading: BgReading, onLongClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (reading.ignoreForStats) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            )
            .combinedClickable(onClick = {}, onLongClick = onLongClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("bg_table_row_${reading.getId()}"),
    ) {
        val value = BgGraphBuilder.unitized_string_with_units_static(reading.calculated_value) +
            "  " + JoH.qs(reading.calculated_value, 1) +
            " " + if (!reading.isBackfilled) reading.slopeArrow() else ""
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
        )
        if (reading.age_adjusted_raw_value > 0) {
            Text(
                text = "Aged raw: " + JoH.qs(reading.age_adjusted_raw_value, 2),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Text(
            text = if (reading.isBackfilled) {
                "Backfilled " + (reading.source_info ?: "")
            } else {
                "Raw: " + JoH.qs(reading.raw_data, 2) + " " + (reading.source_info ?: "")
            },
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = Date(reading.timestamp).toString(),
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

// region Previews

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun BgReadingTableScreenPreview() {
    XdripPreview {
        BgReadingTableScreen(
            subtitle = "120 in 24h, bf:5% mis:2",
            readings = emptyList(),
            onFlag = { _, _ -> },
            onBack = {},
        )
    }
}

// endregion
