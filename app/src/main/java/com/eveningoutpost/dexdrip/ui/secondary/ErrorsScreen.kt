@file:JvmName("ErrorsScreen")

package com.eveningoutpost.dexdrip.ui.secondary

import android.content.res.Configuration
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.databinding.Observable
import com.eveningoutpost.dexdrip.ErrorsActivity
import com.eveningoutpost.dexdrip.R
import com.eveningoutpost.dexdrip.models.UserError
import com.eveningoutpost.dexdrip.ui.theme.XdripPreview
import java.text.DateFormat
import java.util.Date

/**
 * Track V (Logs) — `ErrorsActivity`: legacy severity-filtered error log list. The activity keeps the
 * severity/auto-refresh state, the periodic refresh, keep-screen-on and the log packaging; the
 * screen renders the filter row, list and controls. Java entry point: [installErrors].
 */
fun installErrors(activity: ErrorsActivity) {
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

        ErrorsScreen(
            severityEnabled = mapOf(
                1 to activity.cbLow.get(),
                2 to activity.cbMid.get(),
                3 to activity.cbHigh.get(),
                5 to activity.cbEl.get(),
                6 to activity.cbEh.get(),
            ),
            autoRefresh = activity.switchAutoRefresh.get(),
            errors = remember(tick) { activity.errorsSnapshot },
            onSeverityChange = { severity, value -> activity.setSeverity(severity, value) },
            onAutoRefreshChange = { activity.setAutoRefresh(it) },
            onUpload = { activity.uploadLogs() },
            onBack = { activity.finish() },
        )
    }
}

@Composable
internal fun ErrorsScreen(
    severityEnabled: Map<Int, Boolean>,
    autoRefresh: Boolean,
    errors: List<UserError>,
    onSeverityChange: (Int, Boolean) -> Unit,
    onAutoRefreshChange: (Boolean) -> Unit,
    onUpload: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    SecondaryScreenList(title = ErrorsActivity.menu_name, onBack = onBack) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
            severityLabels.forEach { (severity, label) ->
                SeverityFilter(severity, label, severityEnabled, onSeverityChange)
            }
        }
        Button(
            onClick = onUpload,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("errors_upload"),
        ) {
            Text(context.getString(R.string.upload_logs))
        }
        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
            items(errors) { error ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(severityContainerColor(error.severity))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("errors_row_${error.getId()}"),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = error.shortError.orEmpty(),
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Text(
                            text = DateFormat.getDateTimeInstance().format(Date(error.timestamp.toLong())),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp),
                        )
                    }
                    Text(
                        text = error.message.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
        ) {
            Text(
                text = context.getString(R.string.auto_refresh),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = autoRefresh,
                onCheckedChange = onAutoRefreshChange,
                modifier = Modifier.testTag("errors_autorefresh"),
            )
        }
    }
}

@Composable
private fun SeverityFilter(
    severity: Int,
    label: String,
    severityEnabled: Map<Int, Boolean>,
    onSeverityChange: (Int, Boolean) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 4.dp)) {
        Checkbox(
            checked = severityEnabled[severity] ?: false,
            onCheckedChange = { onSeverityChange(severity, it) },
            modifier = Modifier.testTag("errors_severity_$severity"),
        )
        Text(text = label, style = MaterialTheme.typography.labelMedium)
    }
}

// region Previews

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun ErrorsScreenPreview() {
    XdripPreview {
        ErrorsScreen(
            severityEnabled = mapOf(1 to false, 2 to true, 3 to true, 5 to true, 6 to true),
            autoRefresh = false,
            errors = emptyList(),
            onSeverityChange = { _, _ -> },
            onAutoRefreshChange = {},
            onUpload = {},
            onBack = {},
        )
    }
}

// endregion
