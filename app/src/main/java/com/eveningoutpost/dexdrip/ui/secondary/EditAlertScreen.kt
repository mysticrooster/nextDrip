@file:JvmName("EditAlertScreen")

package com.eveningoutpost.dexdrip.ui.secondary

import android.content.res.Configuration
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.databinding.Observable
import com.eveningoutpost.dexdrip.EditAlertActivity
import com.eveningoutpost.dexdrip.R
import com.eveningoutpost.dexdrip.SnoozeActivity
import com.eveningoutpost.dexdrip.ui.theme.XdripPreview

/**
 * Track V (Alerts) — `EditAlertActivity` form. Thresholds, tone, snooze/pre-snooze, time range,
 * override-silent/force-speaker/vibrate/disable toggles. All validation and side effects stay in
 * the activity. Java entry point: [installEditAlert].
 */
@OptIn(ExperimentalMaterial3Api::class)
fun installEditAlert(activity: EditAlertActivity) {
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

        var name by remember(tick) { mutableStateOf(activity.name.get().orEmpty()) }
        var threshold by remember(tick) { mutableStateOf(activity.thresholdText.get().orEmpty()) }
        var snooze by remember(tick) { mutableStateOf(activity.snoozeText.get().orEmpty()) }
        var reraise by remember(tick) { mutableStateOf(activity.reraiseText.get().orEmpty()) }

        EditAlertScreen(
            header = remember(tick) { activity.header.get().orEmpty() },
            name = name,
            threshold = threshold,
            snooze = snooze,
            reraise = reraise,
            tone = remember(tick) { activity.toneText.get().orEmpty() },
            startTime = remember(tick) { activity.startTimeText.get().orEmpty() },
            endTime = remember(tick) { activity.endTimeText.get().orEmpty() },
            allDay = remember(tick) { activity.allDay.get() ?: true },
            vibrate = remember(tick) { activity.vibrate.get() ?: true },
            disabled = remember(tick) { activity.disabled.get() ?: false },
            overrideSilent = remember(tick) { activity.overrideSilent.get() ?: true },
            forceSpeaker = remember(tick) { activity.forceSpeaker.get() ?: true },
            editable = remember(tick) { activity.editable.get() ?: true },
            removable = remember(tick) { activity.removable.get() ?: false },
            onNameChange = { name = it; activity.setName(it) },
            onThresholdChange = { threshold = it; activity.setThreshold(it) },
            onSnoozeChange = { snooze = it; activity.setSnooze(it) },
            onReraiseChange = { reraise = it; activity.setReraise(it) },
            onAllDayChange = { activity.setAllDay(it) },
            onVibrateChange = { activity.setVibrate(it) },
            onDisabledChange = { activity.setDisabled(it) },
            onOverrideSilentChange = { activity.setOverrideSilent(it) },
            onForceSpeakerChange = { activity.setForceSpeaker(it) },
            onPickStartTime = { activity.pickStartTime() },
            onPickEndTime = { activity.pickEndTime() },
            onChooseRingtone = { activity.chooseRingtone() },
            onChooseToneFile = { activity.chooseToneFile() },
            onDefaultTone = { activity.useDefaultTone() },
            onApplySnooze = { activity.applySnoozeChoice(it) },
            onPreSnooze = { activity.preSnooze(it) },
            onTest = { activity.testAlert() },
            onSave = { activity.saveAlert() },
            onRemove = { activity.removeAlert() },
            onBack = { activity.finish() },
        )
    }
}

@Composable
internal fun EditAlertScreen(
    header: String,
    name: String,
    threshold: String,
    snooze: String,
    reraise: String,
    tone: String,
    startTime: String,
    endTime: String,
    allDay: Boolean,
    vibrate: Boolean,
    disabled: Boolean,
    overrideSilent: Boolean,
    forceSpeaker: Boolean,
    editable: Boolean,
    removable: Boolean,
    onNameChange: (String) -> Unit,
    onThresholdChange: (String) -> Unit,
    onSnoozeChange: (String) -> Unit,
    onReraiseChange: (String) -> Unit,
    onAllDayChange: (Boolean) -> Unit,
    onVibrateChange: (Boolean) -> Unit,
    onDisabledChange: (Boolean) -> Unit,
    onOverrideSilentChange: (Boolean) -> Unit,
    onForceSpeakerChange: (Boolean) -> Unit,
    onPickStartTime: () -> Unit,
    onPickEndTime: () -> Unit,
    onChooseRingtone: () -> Unit,
    onChooseToneFile: () -> Unit,
    onDefaultTone: () -> Unit,
    onApplySnooze: (Int) -> Unit,
    onPreSnooze: (Int) -> Unit,
    onTest: () -> Unit,
    onSave: () -> Unit,
    onRemove: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val decoration = if (disabled) TextDecoration.LineThrough else TextDecoration.None
    var showToneDialog by remember { mutableStateOf(false) }
    var showSnoozeDialog by remember { mutableStateOf(false) }
    var showPreSnoozeDialog by remember { mutableStateOf(false) }

    SecondaryScreen(title = context.getString(R.string.title_activity_edit_alert), onBack = onBack) {
        Text(
            text = header,
            style = MaterialTheme.typography.titleLarge,
            textDecoration = decoration,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .testTag("edit_alert_header"),
        )
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            singleLine = true,
            enabled = editable,
            label = { Text(context.getString(R.string.alert_name_colon), textDecoration = decoration) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .testTag("edit_alert_name"),
        )
        OutlinedTextField(
            value = threshold,
            onValueChange = onThresholdChange,
            singleLine = true,
            enabled = editable,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            label = { Text(context.getString(R.string.threshold_colon), textDecoration = decoration) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .testTag("edit_alert_threshold"),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
        ) {
            Text(
                text = context.getString(R.string.default_snooze_colon),
                textDecoration = decoration,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            OutlinedButton(
                onClick = { showSnoozeDialog = true },
                modifier = Modifier.testTag("edit_alert_snooze"),
            ) {
                Text(snooze)
            }
        }
        OutlinedTextField(
            value = reraise,
            onValueChange = onReraiseChange,
            singleLine = true,
            enabled = editable,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            label = { Text(context.getString(R.string.re_raise_every_x_minutes_if_unaknowledged), textDecoration = decoration) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .testTag("edit_alert_reraise"),
        )
        OutlinedTextField(
            value = tone,
            onValueChange = {},
            singleLine = true,
            readOnly = true,
            label = { Text(context.getString(R.string.alert_tone_colon), textDecoration = decoration) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .testTag("edit_alert_tone"),
        )
        Button(
            onClick = { showToneDialog = true },
            enabled = editable,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
                .testTag("edit_alert_choose_tone"),
        ) {
            Text(context.getString(R.string.choose_file))
        }
        SwitchRow(
            title = context.getString(R.string.all_day),
            checked = allDay,
            enabled = editable,
            decoration = decoration,
            onCheckedChange = onAllDayChange,
            tag = "edit_alert_allday",
        )
        if (!allDay) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
            ) {
                Text(
                    text = startTime,
                    textDecoration = decoration,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onPickStartTime() }
                        .padding(vertical = 12.dp)
                        .testTag("edit_alert_start_time"),
                )
                Text("--")
                Text(
                    text = endTime,
                    textDecoration = decoration,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onPickEndTime() }
                        .padding(start = 8.dp, top = 12.dp, bottom = 12.dp)
                        .testTag("edit_alert_end_time"),
                )
            }
            Text(
                text = context.getString(R.string.tap_to_change),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            )
        }
        SwitchRow(
            title = context.getString(R.string.vibrate_on_alert),
            checked = vibrate,
            enabled = editable,
            decoration = decoration,
            onCheckedChange = onVibrateChange,
            tag = "edit_alert_vibrate",
        )
        SwitchRow(
            title = context.getString(R.string.override_phone_silent_mode_colon),
            checked = overrideSilent,
            enabled = editable,
            decoration = decoration,
            onCheckedChange = onOverrideSilentChange,
            tag = "edit_alert_override_silent",
        )
        if (!overrideSilent) {
            Text(
                text = context.getString(R.string.warning_no_alert_will_be_played_in_silent_mode),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .testTag("edit_alert_silent_warning"),
            )
        }
        SwitchRow(
            title = context.getString(R.string.force_speaker_colon),
            checked = forceSpeaker,
            enabled = editable,
            decoration = decoration,
            onCheckedChange = onForceSpeakerChange,
            tag = "edit_alert_force_speaker",
        )
        SwitchRow(
            title = context.getString(R.string.disable_alert),
            checked = disabled,
            enabled = true,
            decoration = decoration,
            onCheckedChange = onDisabledChange,
            tag = "edit_alert_disable",
        )
        Button(
            onClick = onTest,
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .testTag("edit_alert_test"),
        ) {
            Text(context.getString(R.string.test_alert))
        }
        Row(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
            Button(
                onClick = onSave,
                modifier = Modifier
                    .weight(1f)
                    .testTag("edit_alert_save"),
            ) {
                Text(context.getString(R.string.save_alert))
            }
            if (removable) {
                OutlinedButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                        .testTag("edit_alert_remove"),
                ) {
                    Text(context.getString(R.string.remove_alert))
                }
                OutlinedButton(
                    onClick = { showPreSnoozeDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                        .testTag("edit_alert_pre_snooze"),
                ) {
                    Text(context.getString(R.string.snooze_alert_before_it_fires))
                }
            }
        }
    }

    if (showToneDialog) {
        val options = context.resources.getStringArray(R.array.alertType)
        AlertDialog(
            onDismissRequest = { showToneDialog = false },
            title = { Text(context.getString(R.string.what_type_of_alert)) },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    itemsIndexed(options.toList()) { index, option ->
                        Text(
                            text = option,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showToneDialog = false
                                    when (index) {
                                        0 -> onChooseRingtone()
                                        1 -> onChooseToneFile()
                                        else -> onDefaultTone()
                                    }
                                }
                                .padding(vertical = 14.dp)
                                .testTag("edit_alert_tone_option_$index"),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showToneDialog = false }) {
                    Text(context.getString(R.string.cancel))
                }
            },
        )
    }

    if (showSnoozeDialog || showPreSnoozeDialog) {
        val preSnooze = showPreSnoozeDialog
        val labels = SnoozeActivity.snoozeValues.map { SnoozeActivity.getNameFromTime(it) }
        var chosen by remember {
            mutableStateOf(SnoozeActivity.getSnoozeLocation(snooze.toIntOrNull() ?: 0))
        }
        AlertDialog(
            onDismissRequest = {
                showSnoozeDialog = false
                showPreSnoozeDialog = false
            },
            title = { Text(if (preSnooze) "Snooze this alert" else context.getString(R.string.default_snooze)) },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    itemsIndexed(labels) { index, label ->
                        Text(
                            text = label,
                            color = if (index == chosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { chosen = index }
                                .padding(vertical = 14.dp)
                                .testTag("edit_alert_snooze_option_$index"),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSnoozeDialog = false
                        showPreSnoozeDialog = false
                        val minutes = SnoozeActivity.getTimeFromSnoozeValue(chosen)
                        if (preSnooze) onPreSnooze(minutes) else onApplySnooze(minutes)
                    },
                ) {
                    Text(context.getString(if (preSnooze) R.string.pre_snooze else R.string.set))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSnoozeDialog = false
                        showPreSnoozeDialog = false
                    },
                ) {
                    Text(context.getString(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun SwitchRow(
    title: String,
    checked: Boolean,
    enabled: Boolean,
    decoration: TextDecoration,
    onCheckedChange: (Boolean) -> Unit,
    tag: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text = title,
            textDecoration = decoration,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            modifier = Modifier.testTag(tag),
        )
    }
}

// region Previews

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 1200)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 1200)
@Composable
private fun EditAlertScreenPreview() {
    XdripPreview {
        EditAlertScreen(
            header = "Editing high alert",
            name = "High",
            threshold = "180",
            snooze = "120",
            reraise = "1",
            tone = "xDrip Default",
            startTime = "00:00",
            endTime = "23:59",
            allDay = true,
            vibrate = true,
            disabled = false,
            overrideSilent = true,
            forceSpeaker = true,
            editable = true,
            removable = true,
            onNameChange = {},
            onThresholdChange = {},
            onSnoozeChange = {},
            onReraiseChange = {},
            onAllDayChange = {},
            onVibrateChange = {},
            onDisabledChange = {},
            onOverrideSilentChange = {},
            onForceSpeakerChange = {},
            onPickStartTime = {},
            onPickEndTime = {},
            onChooseRingtone = {},
            onChooseToneFile = {},
            onDefaultTone = {},
            onApplySnooze = {},
            onPreSnooze = {},
            onTest = {},
            onSave = {},
            onRemove = {},
            onBack = {},
        )
    }
}

// endregion
