@file:JvmName("StopSensorScreen")

package com.eveningoutpost.dexdrip.ui.secondary

import android.content.res.Configuration
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.eveningoutpost.dexdrip.R
import com.eveningoutpost.dexdrip.StopSensor
import com.eveningoutpost.dexdrip.ui.theme.XdripPreview

/**
 * Track V pass 5 — `StopSensor`: confirm-gated stop plus the "reset all calibrations" alternative
 * (shown only when the collector supports it). Java entry point: [installStopSensor].
 */
fun installStopSensor(activity: StopSensor) {
    activity.setContent {
        StopSensorScreen(
            resettableCals = activity.viewModel.resettableCals(),
            stopConfirmMessage = activity.stopConfirmMessage(),
            onConfirmStop = { activity.confirmStop() },
            onConfirmResetCalibrations = { activity.confirmResetCalibrations() },
            onBack = { activity.finish() },
        )
    }
}

@Composable
internal fun StopSensorScreen(
    resettableCals: Boolean,
    stopConfirmMessage: String,
    onConfirmStop: () -> Unit,
    onConfirmResetCalibrations: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var showStopConfirm by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }

    SecondaryScreen(title = context.getString(R.string.title_activity_stop_sensor), onBack = onBack) {
        Text(
            text = context.getString(R.string.stop_sensor),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        )
        Text(
            text = context.getString(R.string.only_stop_your_sensor_when_you_actually_plan),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        )
        Button(
            onClick = { showStopConfirm = true },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 30.dp)
                .testTag("stop_sensor_stop"),
        ) {
            Text(context.getString(R.string.stop_sensor))
        }
        if (resettableCals) {
            Button(
                onClick = { showResetConfirm = true },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 20.dp)
                    .testTag("stop_sensor_reset"),
            ) {
                Text(context.getString(R.string.dont_stop_just_reset_all_calibrations))
            }
        }
    }

    if (showStopConfirm) {
        AlertDialog(
            onDismissRequest = { showStopConfirm = false },
            title = { Text(context.getString(R.string.are_you_sure)) },
            text = { Text(stopConfirmMessage) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showStopConfirm = false
                        onConfirmStop()
                    },
                ) {
                    Text(context.getString(R.string.yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { showStopConfirm = false }) {
                    Text(context.getString(R.string.no))
                }
            },
        )
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text(context.getString(R.string.are_you_sure)) },
            text = { Text(context.getString(R.string.do_you_want_to_delete_and_reset_the_calibrations_for_this_sensor)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetConfirm = false
                        onConfirmResetCalibrations()
                    },
                ) {
                    Text(context.getString(R.string.yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text(context.getString(R.string.no))
                }
            },
        )
    }
}

// region Previews

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun StopSensorScreenPreview() {
    XdripPreview {
        StopSensorScreen(
            resettableCals = true,
            stopConfirmMessage = "Stop the current sensor?",
            onConfirmStop = {},
            onConfirmResetCalibrations = {},
            onBack = {},
        )
    }
}

// endregion
