@file:JvmName("SnoozeScreen")

package com.eveningoutpost.dexdrip.ui.secondary

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.databinding.Observable
import com.eveningoutpost.dexdrip.R
import com.eveningoutpost.dexdrip.SnoozeActivity
import com.eveningoutpost.dexdrip.models.ActiveBgAlert
import com.eveningoutpost.dexdrip.ui.theme.XdripPreview
import com.eveningoutpost.dexdrip.utilitymodels.Pref
import java.text.DateFormat
import java.util.Date
import kotlin.math.roundToInt

/** Snapshot of the legacy `displayStatus()` / `showDisableEnableButtons()` UI state. */
internal data class SnoozeState(
    val status: String,
    val showSnooze: Boolean,
    val showRemoteSnooze: Boolean,
    val snoozeLabels: List<String>,
    val selectedIndex: Int,
    val showDisableAll: Boolean,
    val showEnableAll: Boolean,
    val showDisableLow: Boolean,
    val showEnableLow: Boolean,
    val showDisableHigh: Boolean,
    val showEnableHigh: Boolean,
)

/**
 * Track V pass 5 — `SnoozeActivity`: alert snooze, per-type disable/re-enable and remote snooze.
 * The activity keeps the static snooze helpers (`snoozeForType`, `recheckAlerts`) and the
 * preference actions; the screen derives the status/visibility state. Java entry point:
 * [installSnooze].
 */
fun installSnooze(activity: SnoozeActivity) {
    activity.setContent {
        val context = LocalContext.current
        val prefs = activity.prefs

        var tick by remember { mutableStateOf(activity.refreshTick.get() ?: 0) }
        DisposableEffect(activity) {
            val callback = object : Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: Observable, propertyId: Int) {
                    tick = activity.refreshTick.get() ?: 0
                }
            }
            activity.refreshTick.addOnPropertyChangedCallback(callback)
            onDispose { activity.refreshTick.removeOnPropertyChangedCallback(callback) }
        }

        val state = remember(tick) { snoozeUiState(context, prefs) }

        SnoozeScreen(
            state = state,
            onSnooze = { minutes -> activity.snoozeNow(minutes) },
            onDisable = { type, minutes -> activity.disableType(minutes, type) },
            onClear = { type -> activity.clearDisabled(type) },
            onRemoteSnooze = { activity.sendRemoteSnooze() },
            onBack = { activity.finish() },
        )
    }
}

internal fun snoozeUiState(context: Context, prefs: SharedPreferences): SnoozeState {
    val now = Date().time
    val allUntil = prefs.getLong(SnoozeActivity.SnoozeType.ALL_ALERTS.getPrefKey(), 0)
    val lowUntil = prefs.getLong(SnoozeActivity.SnoozeType.LOW_ALERTS.getPrefKey(), 0)
    val highUntil = prefs.getLong(SnoozeActivity.SnoozeType.HIGH_ALERTS.getPrefKey(), 0)
    val allDisabled = allUntil > now
    val lowDisabled = lowUntil > now
    val highDisabled = highUntil > now

    val aba = ActiveBgAlert.getOnly()
    val activeBgAlert = if (aba != null) ActiveBgAlert.alertTypegetOnly() else null

    var status: String
    var showSnooze: Boolean
    var showRemote: Boolean
    var selectedIndex = 0

    if (activeBgAlert == null) {
        showRemote = Pref.getBooleanDefaultFalse("send_snooze_to_remote")
        status = if (allDisabled || (lowDisabled && highDisabled)) {
            ""
        } else {
            context.getString(R.string.no_active_alert_exists)
        }
        showSnooze = false
    } else {
        showRemote = false
        showSnooze = true
        val nextAlertAt = aba?.next_alert_at ?: now
        status = if (aba != null && !aba.ready_to_alarm()) {
            context.getString(
                if (aba.is_snoozed) R.string.active_alert_snoozed_until else R.string.active_alert_rerise_at,
                activeBgAlert.name,
                DateFormat.getTimeInstance().format(Date(nextAlertAt)),
                (nextAlertAt - now) / 60000,
            )
        } else {
            context.getString(R.string.active_alert_exists_named) + " \"" + activeBgAlert.name + "\" " +
                context.getString(R.string.bracket_not_snoozed)
        }
        val defaultSnooze = activeBgAlert.default_snooze
        selectedIndex = SnoozeActivity.getSnoozeLocation(
            if (defaultSnooze != 0) defaultSnooze else SnoozeActivity.getDefaultSnooze(activeBgAlert.above),
        )
    }

    status = if (allDisabled) {
        context.getString(R.string.all_alerts_disabled_until) + disabledUntilText(context, allUntil, now)
    } else {
        var result = status
        if (lowDisabled) {
            result += "\n\n" + context.getString(R.string.low_alerts_disabled_until) + disabledUntilText(context, lowUntil, now)
        }
        if (highDisabled) {
            result += "\n\n" + context.getString(R.string.high_alerts_disabled_until) + disabledUntilText(context, highUntil, now)
        }
        result
    }

    return SnoozeState(
        status = status,
        showSnooze = showSnooze,
        showRemoteSnooze = showRemote,
        snoozeLabels = SnoozeActivity.snoozeValues.map { SnoozeActivity.getNameFromTime(it) },
        selectedIndex = selectedIndex,
        showDisableAll = !allDisabled,
        showEnableAll = allDisabled,
        showDisableLow = !allDisabled && !lowDisabled,
        showEnableLow = !allDisabled && lowDisabled,
        showDisableHigh = !allDisabled && !highDisabled,
        showEnableHigh = !allDisabled && highDisabled,
    )
}

private fun disabledUntilText(context: Context, disabledUntil: Long, now: Long): String =
    if (disabledUntil > now + (SnoozeActivity.infiniteSnoozeValueInMinutes - 365 * 24 * 60) * 60 * 1000) {
        context.getString(R.string.you_reenable)
    } else {
        DateFormat.getTimeInstance().format(Date(disabledUntil))
    }

@Composable
internal fun SnoozeScreen(
    state: SnoozeState,
    onSnooze: (Int) -> Unit,
    onDisable: (SnoozeActivity.SnoozeType, Long) -> Unit,
    onClear: (SnoozeActivity.SnoozeType) -> Unit,
    onRemoteSnooze: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var selectedIndex by remember(state.selectedIndex) { mutableStateOf(state.selectedIndex) }
    var disableTarget by remember { mutableStateOf<SnoozeActivity.SnoozeType?>(null) }

    SecondaryScreen(title = context.getString(R.string.snooze_alert), onBack = onBack) {
        if (state.showSnooze) {
            Button(
                onClick = { onSnooze(SnoozeActivity.getTimeFromSnoozeValue(selectedIndex)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("snooze_button"),
            ) {
                Text(context.getString(R.string.snooze), style = MaterialTheme.typography.headlineSmall)
            }
            Text(
                text = state.snoozeLabels.getOrElse(selectedIndex) { "" },
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("snooze_value"),
            )
            Slider(
                value = selectedIndex.toFloat(),
                onValueChange = { selectedIndex = it.roundToInt() },
                valueRange = 0f..(state.snoozeLabels.size - 1).toFloat(),
                steps = (state.snoozeLabels.size - 2).coerceAtLeast(0),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("snooze_picker"),
            )
        }
        Text(
            text = state.status,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("snooze_status"),
        )
        val disableTypes = listOf(
            Triple(SnoozeActivity.SnoozeType.LOW_ALERTS, state.showDisableLow, state.showEnableLow),
            Triple(SnoozeActivity.SnoozeType.HIGH_ALERTS, state.showDisableHigh, state.showEnableHigh),
            Triple(SnoozeActivity.SnoozeType.ALL_ALERTS, state.showDisableAll, state.showEnableAll),
        )
        disableTypes.forEach { (type, showDisable, showEnable) ->
            if (showDisable) {
                Button(
                    onClick = { disableTarget = type },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .testTag("snooze_disable_${type.name.lowercase()}"),
                ) {
                    Text(disableLabel(context, type))
                }
            }
            if (showEnable) {
                Button(
                    onClick = { onClear(type) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .testTag("snooze_enable_${type.name.lowercase()}"),
                ) {
                    Text(enableLabel(context, type))
                }
            }
        }
        if (state.showRemoteSnooze) {
            Button(
                onClick = onRemoteSnooze,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("snooze_remote"),
            ) {
                Text(context.getString(R.string.send_remote_snooze))
            }
        }
    }

    disableTarget?.let { type ->
        var chosen by remember(type) { mutableStateOf(SnoozeActivity.getSnoozeLocation(60)) }
        val options = state.snoozeLabels + context.getString(R.string.until_you_reenable)
        AlertDialog(
            onDismissRequest = { disableTarget = null },
            title = { Text(context.getString(R.string.default_snooze)) },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    itemsIndexed(options) { index, label ->
                        Text(
                            text = label,
                            color = if (index == chosen) MaterialTheme.colorScheme.primary else Color.Unspecified,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { chosen = index }
                                .padding(vertical = 14.dp)
                                .testTag("snooze_option_$index"),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        disableTarget = null
                        val minutes = if (chosen == options.lastIndex) {
                            SnoozeActivity.infiniteSnoozeValueInMinutes
                        } else {
                            SnoozeActivity.getTimeFromSnoozeValue(chosen).toLong()
                        }
                        onDisable(type, minutes)
                    },
                ) {
                    Text(context.getString(R.string.set))
                }
            },
            dismissButton = {
                TextButton(onClick = { disableTarget = null }) {
                    Text(context.getString(R.string.cancel))
                }
            },
        )
    }
}

private fun disableLabel(context: Context, type: SnoozeActivity.SnoozeType): String = when (type) {
    SnoozeActivity.SnoozeType.ALL_ALERTS -> context.getString(R.string.disable_all_alerts)
    SnoozeActivity.SnoozeType.LOW_ALERTS -> context.getString(R.string.disable_low_alerts)
    SnoozeActivity.SnoozeType.HIGH_ALERTS -> context.getString(R.string.disable_high_alerts)
}

private fun enableLabel(context: Context, type: SnoozeActivity.SnoozeType): String = when (type) {
    SnoozeActivity.SnoozeType.ALL_ALERTS -> context.getString(R.string.re_enable_all_alerts)
    SnoozeActivity.SnoozeType.LOW_ALERTS -> context.getString(R.string.re_enable_low_alerts)
    SnoozeActivity.SnoozeType.HIGH_ALERTS -> context.getString(R.string.re_enable_high_alerts)
}

// region Previews

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun SnoozeScreenPreview() {
    XdripPreview {
        SnoozeScreen(
            state = SnoozeState(
                status = "No active alert",
                showSnooze = true,
                showRemoteSnooze = true,
                snoozeLabels = listOf("15 minutes", "30 minutes", "1 hour"),
                selectedIndex = 0,
                showDisableAll = true,
                showEnableAll = false,
                showDisableLow = false,
                showEnableLow = false,
                showDisableHigh = false,
                showEnableHigh = false,
            ),
            onSnooze = {},
            onDisable = { _, _ -> },
            onClear = {},
            onRemoteSnooze = {},
            onBack = {},
        )
    }
}

// endregion
