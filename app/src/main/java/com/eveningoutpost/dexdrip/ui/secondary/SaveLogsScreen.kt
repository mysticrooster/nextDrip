@file:JvmName("SaveLogsScreen")

package com.eveningoutpost.dexdrip.ui.secondary

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.eveningoutpost.dexdrip.R
import com.eveningoutpost.dexdrip.utilitymodels.SaveLogs

/**
 * Track V V5 — `SaveLogs`: informational screen shown from the event log with the packed log text
 * passed as a `generic_text` extra. Java entry point: [installSaveLogs].
 */
fun installSaveLogs(activity: SaveLogs) {
    activity.setContent {
        SaveLogsScreen(
            logData = activity.logData,
            onSave = { activity.saveLogs() },
            onClose = { activity.finish() },
        )
    }
}

@Composable
internal fun SaveLogsScreen(
    logData: String,
    onSave: () -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    SecondaryScreen(title = context.getString(R.string.title_activity_save_logs), onBack = onClose) {
        Text(
            text = context.getString(R.string.log_confidential_note),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
        Text(
            text = displayText(logData),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("savelogs_text"),
        )
        Button(
            onClick = onSave,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("savelogs_save"),
        ) {
            Text(context.getString(R.string.save_logs))
        }
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .testTag("savelogs_close"),
        ) {
            Icon(painter = painterResource(android.R.drawable.ic_delete), contentDescription = null)
        }
    }
}

/** Legacy formatting: hide the payload when it is long, showing only its size. */
internal fun displayText(logData: String): String =
    if (logData.length > 300) {
        "\n\nAttached ${logData.length} characters of log data. (hidden)\n\n"
    } else {
        logData
    }
