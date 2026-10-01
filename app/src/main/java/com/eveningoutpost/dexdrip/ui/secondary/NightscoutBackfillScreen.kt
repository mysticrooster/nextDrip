@file:JvmName("NightscoutBackfillScreen")

package com.eveningoutpost.dexdrip.ui.secondary

import android.content.res.Configuration
import android.os.PowerManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.eveningoutpost.dexdrip.R
import com.eveningoutpost.dexdrip.models.BgReading
import com.eveningoutpost.dexdrip.models.JoH
import com.eveningoutpost.dexdrip.models.Treatments
import com.eveningoutpost.dexdrip.services.SyncService
import com.eveningoutpost.dexdrip.ui.theme.XdripPreview
import com.eveningoutpost.dexdrip.utilitymodels.Constants
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore
import com.eveningoutpost.dexdrip.utilitymodels.UploaderQueue
import com.eveningoutpost.dexdrip.utilitymodels.UploaderTask
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Track V — `NightscoutBackfillActivity`. Queues readings/treatments from a chosen date for upload.
 * The one-hour re-entry guard is factored into [BackfillGuard] so it is testable without the
 * background thread. Java entry point: [installNightscoutBackfill].
 */
internal object BackfillGuard {
    @Volatile
    private var lockedAt: Long = 0

    fun isLocked(now: Long = JoH.tsl()): Boolean =
        lockedAt != 0L && JoH.msSince(now, lockedAt) < Constants.HOUR_IN_MS

    fun markRun(now: Long = JoH.tsl()) {
        lockedAt = now
    }

    fun clear() {
        lockedAt = 0
    }
}

fun installNightscoutBackfill(activity: ComponentActivity) {
    if (BackfillGuard.isLocked()) {
        JoH.static_toast_long(activity.getString(R.string.still_processing_previous_backfill_request))
        activity.finish()
        return
    }
    activity.setContent {
        NightscoutBackfillScreen(
            onCancel = { activity.finish() },
            onRun = { fromMillis -> runBackfill(activity, fromMillis) },
        )
    }
}

private fun runBackfill(activity: ComponentActivity, fromMillis: Long) {
    BackfillGuard.markRun()
    JoH.static_toast_long(activity.getString(R.string.please_wait))
    Thread {
        val wl: PowerManager.WakeLock = JoH.getWakeLock("nightscout-backfill", 600000)
        try {
            val readings = BgReading.latestForGraphAsc(500000, fromMillis, JoH.tsl())
            if (!readings.isNullOrEmpty()) {
                PersistentStore.setBoolean(UploaderTask.BACKFILLING_BOOSTER, true)
                readings.forEach { UploaderQueue.newEntry("update", it) }
                val treatments = Treatments.latestForGraph(50000, fromMillis, JoH.tsl())
                treatments?.forEach { UploaderQueue.newEntry("update", it) }
                JoH.static_toast_long("Queued ${readings.size} glucose readings and ${treatments?.size ?: 0} treatments!")
                SyncService.startSyncService(500)
                BackfillGuard.clear()
            } else {
                JoH.static_toast_long(activity.getString(R.string.didnt_find_any_glucose_readings_in_that_time_period))
            }
        } finally {
            JoH.releaseWakeLock(wl)
        }
    }.start()
    activity.finish()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NightscoutBackfillScreen(
    onCancel: () -> Unit,
    onRun: (Long) -> Unit,
) {
    val context = LocalContext.current
    val formatter = remember { SimpleDateFormat("MMMM d, yyyy h:mm a", Locale.getDefault()) }
    var selectedMillis by remember { mutableLongStateOf(JoH.tsl() - Constants.DAY_IN_MS) }
    var showPicker by remember { mutableStateOf(false) }

    SecondaryScreen(title = "Nightscout Backfill", onBack = onCancel) {
        Text(
            text = "Choose the date for the oldest record to send to Nightscout. Use with care!",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        )
        Button(
            onClick = { showPicker = true },
            modifier = Modifier
                .align(androidx.compose.ui.Alignment.CenterHorizontally)
                .testTag("backfill_date"),
        ) {
            Text(formatter.format(Date(selectedMillis)))
        }
        Row(
            modifier = Modifier
                .align(androidx.compose.ui.Alignment.CenterHorizontally)
                .padding(top = 24.dp),
        ) {
            Button(
                onClick = { onRun(selectedMillis) },
                modifier = Modifier.testTag("backfill_run"),
            ) {
                Text("Do it!")
            }
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .padding(start = 16.dp)
                    .testTag("backfill_cancel"),
            ) {
                Text(context.getString(R.string.cancel))
            }
        }
    }

    if (showPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = selectedMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= System.currentTimeMillis()
            },
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { selectedMillis = it }
                    showPicker = false
                }) {
                    Text(context.getString(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text(context.getString(R.string.cancel))
                }
            },
        ) {
            DatePicker(state = state)
        }
    }
}

// region Previews

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun NightscoutBackfillScreenPreview() {
    XdripPreview {
        NightscoutBackfillScreen(onCancel = {}, onRun = {})
    }
}

// endregion
