@file:JvmName("CalibrationDataTableScreen")

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
import com.eveningoutpost.dexdrip.models.Calibration
import com.eveningoutpost.dexdrip.models.JoH
import com.eveningoutpost.dexdrip.tables.CalibrationDataTable
import com.eveningoutpost.dexdrip.ui.theme.XdripPreview
import com.eveningoutpost.dexdrip.utilitymodels.BgGraphBuilder

/**
 * Track V (Data tables) — `CalibrationDataTable`: calibration rows with a long-press
 * disable-calibration action. The activity loads the calibrations and persists the change; the
 * screen renders them. Java entry point: [installCalibrationDataTable].
 */
fun installCalibrationDataTable(activity: CalibrationDataTable) {
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

        CalibrationDataTableScreen(
            calibrations = remember(tick) { activity.calibrationsSnapshot },
            onDisable = { activity.disableCalibration(it) },
            onBack = { activity.finish() },
        )
    }
}

@Composable
internal fun CalibrationDataTableScreen(
    calibrations: List<Calibration>,
    onDisable: (Calibration) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var dialogCalibration by remember { mutableStateOf<Calibration?>(null) }

    SecondaryScreenList(title = "Calibration Data Table", onBack = onBack) {
        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
            items(calibrations) { calibration ->
                CalibrationRow(calibration) { dialogCalibration = calibration }
            }
        }
    }

    dialogCalibration?.let { calibration ->
        AlertDialog(
            onDismissRequest = { dialogCalibration = null },
            title = { Text("Disable calibration") },
            text = { Text("Disable this calibration?\nFlagged calibrations will no longer have an effect.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        dialogCalibration = null
                        onDisable(calibration)
                    },
                ) {
                    Text(context.getString(R.string.yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { dialogCalibration = null }) {
                    Text(context.getString(R.string.no))
                }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CalibrationRow(calibration: Calibration, onLongClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                when {
                    calibration.isNote -> MaterialTheme.colorScheme.tertiaryContainer
                    !calibration.isValid -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
            )
            .combinedClickable(onClick = {}, onLongClick = onLongClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("calibration_table_row_${calibration.getId()}"),
    ) {
        Text(
            text = JoH.qs(calibration.bg, 4) + "    " + BgGraphBuilder.unitized_string_static(calibration.bg),
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
        )
        Text(
            text = "raw: " + JoH.qs(calibration.estimate_raw_at_time_of_calibration, 4),
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = "slope: " + JoH.qs(calibration.slope, 4) + " intercept: " + JoH.qs(calibration.intercept, 4),
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = JoH.dateTimeText(calibration.timestamp) + "  (" + JoH.dateTimeText(calibration.raw_timestamp) + ")",
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

// region Previews

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun CalibrationDataTableScreenPreview() {
    XdripPreview {
        CalibrationDataTableScreen(
            calibrations = emptyList(),
            onDisable = {},
            onBack = {},
        )
    }
}

// endregion
