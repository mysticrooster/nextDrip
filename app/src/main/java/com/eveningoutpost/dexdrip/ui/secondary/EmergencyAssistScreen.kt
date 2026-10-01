@file:JvmName("EmergencyAssistScreen")

package com.eveningoutpost.dexdrip.ui.secondary

import android.content.res.Configuration
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.databinding.Observable
import androidx.databinding.ObservableList
import androidx.databinding.ObservableMap
import com.eveningoutpost.dexdrip.R
import com.eveningoutpost.dexdrip.eassist.EmergencyAssist
import com.eveningoutpost.dexdrip.eassist.EmergencyAssistActivity
import com.eveningoutpost.dexdrip.eassist.EmergencyContact
import com.eveningoutpost.dexdrip.ui.theme.XdripPreview
import com.eveningoutpost.dexdrip.utilitymodels.Pref
import kotlin.math.roundToInt

private const val KEY_LOW_ALERT = "emergency_assist_low_alert"
private const val KEY_LOWEST_ALERT = "emergency_assist_lowest_alert"
private const val KEY_HIGH_ALERT = "emergency_assist_high_alert"
private const val KEY_INACTIVITY = "emergency_assist_inactivity"
private const val KEY_LOW_MINS = EmergencyAssist.EMERGENCY_LOW_MINS_PREF
private const val KEY_LOWEST_MINS = EmergencyAssist.EMERGENCY_LOWEST_MINS_PREF
private const val KEY_HIGH_MINS = EmergencyAssist.EMERGENCY_HIGH_MINS_PREF
private const val KEY_INACTIVITY_MINS = "emergency_assist_inactivity_minutes"

/**
 * Track V (rich Medium) — `EmergencyAssistActivity`: settings for the emergency assistance text
 * message feature. Keeps the Java activity as the flow owner (contacts permission + picker, SMS
 * permission, location) and bridges its `PrefsViewImpl`, `EmergencyAssist` model and contact
 * `ObservableList` into Compose. Java entry point: [installEmergencyAssist].
 */
fun installEmergencyAssist(activity: EmergencyAssistActivity) {
    activity.setContent {
        val context = LocalContext.current
        val prefs = activity.prefs
        val model = activity.model
        val contactsObservable = activity.contactModel.items

        var enabled by remember { mutableStateOf(prefs.getbool(EmergencyAssist.EMERGENCY_ASSIST_PREF)) }
        var lowAlert by remember { mutableStateOf(prefs.getbool(KEY_LOW_ALERT)) }
        var lowestAlert by remember { mutableStateOf(prefs.getbool(KEY_LOWEST_ALERT)) }
        var highAlert by remember { mutableStateOf(prefs.getbool(KEY_HIGH_ALERT)) }
        var inactivity by remember { mutableStateOf(prefs.getbool(KEY_INACTIVITY)) }

        var username by remember { mutableStateOf(model.username) }
        var previewText by remember { mutableStateOf(model.lastExtendedText.get() ?: "Preview Text") }
        var contacts by remember { mutableStateOf(contactsObservable.toList()) }

        // Legacy PrefsViewStringSnapDefaults: drag to 0 snaps back to the default, and an
        // unset/zero preference reads back as its default (lowest alert has no default).
        var lowMinutes by remember { mutableStateOf(snappedMinutes(KEY_LOW_MINS)) }
        var lowestMinutes by remember { mutableStateOf(snappedMinutes(KEY_LOWEST_MINS)) }
        var highMinutes by remember { mutableStateOf(snappedMinutes(KEY_HIGH_MINS)) }
        var inactivityMinutes by remember { mutableStateOf(snappedMinutes(KEY_INACTIVITY_MINS)) }

        DisposableEffect(activity) {
            val mapCallback = object : ObservableMap.OnMapChangedCallback<ObservableMap<String, Boolean>, String, Boolean>() {
                override fun onMapChanged(sender: ObservableMap<String, Boolean>?, key: String?) {
                    when (key) {
                        EmergencyAssist.EMERGENCY_ASSIST_PREF -> enabled = prefs.getbool(key)
                        KEY_LOW_ALERT -> lowAlert = prefs.getbool(key)
                        KEY_LOWEST_ALERT -> lowestAlert = prefs.getbool(key)
                        KEY_HIGH_ALERT -> highAlert = prefs.getbool(key)
                        KEY_INACTIVITY -> inactivity = prefs.getbool(key)
                    }
                }
            }
            prefs.addOnMapChangedCallback(mapCallback)

            val listCallback = object : ObservableList.OnListChangedCallback<ObservableList<EmergencyContact>>() {
                override fun onChanged(sender: ObservableList<EmergencyContact>) { contacts = contactsObservable.toList() }
                override fun onItemRangeChanged(sender: ObservableList<EmergencyContact>, positionStart: Int, itemCount: Int) { contacts = contactsObservable.toList() }
                override fun onItemRangeInserted(sender: ObservableList<EmergencyContact>, positionStart: Int, itemCount: Int) { contacts = contactsObservable.toList() }
                override fun onItemRangeRemoved(sender: ObservableList<EmergencyContact>, positionStart: Int, itemCount: Int) { contacts = contactsObservable.toList() }
                override fun onItemRangeMoved(sender: ObservableList<EmergencyContact>, fromPosition: Int, toPosition: Int, itemCount: Int) { contacts = contactsObservable.toList() }
            }
            contactsObservable.addOnListChangedCallback(listCallback)

            val previewCallback = object : Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: Observable, propertyId: Int) {
                    previewText = model.lastExtendedText.get() ?: "Preview Text"
                }
            }
            model.lastExtendedText.addOnPropertyChangedCallback(previewCallback)

            onDispose {
                prefs.removeOnMapChangedCallback(mapCallback)
                contactsObservable.removeOnListChangedCallback(listCallback)
                model.lastExtendedText.removeOnPropertyChangedCallback(previewCallback)
            }
        }

        EmergencyAssistScreen(
            enabled = enabled,
            onEnabledChange = { checked ->
                // Legacy order: the listener (masterEnable) fires before the two-way binding
                // writes the preference; validation runs 100ms later either way.
                activity.masterEnable()
                prefs.setbool(EmergencyAssist.EMERGENCY_ASSIST_PREF, checked)
            },
            username = username,
            onUsernameChange = {
                username = it
                model.username = it
            },
            previewText = previewText,
            contacts = contacts,
            onAddContact = { activity.chooseContact() },
            onRemoveContact = { activity.removeContact(it) },
            reasons = listOf(
                AssistReasonState(
                    tag = "low",
                    title = context.getString(R.string.low_alert_not_acknowledged),
                    checked = lowAlert,
                    minutes = lowMinutes.toIntOrNull() ?: 0,
                    minutesText = activity.prettyMinutes(lowMinutes),
                    maxMinutes = 360,
                    onCheckedChange = { prefs.setbool(KEY_LOW_ALERT, it) },
                    onMinutesChange = { lowMinutes = updateMinutes(KEY_LOW_MINS, it) },
                ),
                AssistReasonState(
                    tag = "lowest",
                    title = context.getString(R.string.lowest_alert_not_acknowledged),
                    checked = lowestAlert,
                    minutes = lowestMinutes.toIntOrNull() ?: 0,
                    minutesText = activity.prettyMinutes(lowestMinutes),
                    maxMinutes = 360,
                    onCheckedChange = { prefs.setbool(KEY_LOWEST_ALERT, it) },
                    onMinutesChange = { lowestMinutes = updateMinutes(KEY_LOWEST_MINS, it) },
                ),
                AssistReasonState(
                    tag = "high",
                    title = context.getString(R.string.high_alert_not_acknowledged),
                    checked = highAlert,
                    minutes = highMinutes.toIntOrNull() ?: 0,
                    minutesText = activity.prettyMinutes(highMinutes),
                    maxMinutes = 720,
                    onCheckedChange = { prefs.setbool(KEY_HIGH_ALERT, it) },
                    onMinutesChange = { highMinutes = updateMinutes(KEY_HIGH_MINS, it) },
                ),
                AssistReasonState(
                    tag = "inactivity",
                    title = context.getString(R.string.device_inactivity),
                    checked = inactivity,
                    minutes = inactivityMinutes.toIntOrNull() ?: 0,
                    minutesText = activity.prettyMinutes(inactivityMinutes),
                    maxMinutes = 2880,
                    onCheckedChange = { prefs.setbool(KEY_INACTIVITY, it) },
                    onMinutesChange = { inactivityMinutes = updateMinutes(KEY_INACTIVITY_MINS, it) },
                ),
            ),
            onTest = { activity.testButton() },
            onBack = { activity.finish() },
        )
    }
}

/** One "send messages when ..." switch with its threshold slider. */
internal data class AssistReasonState(
    val tag: String,
    val title: String,
    val checked: Boolean,
    val minutes: Int,
    val minutesText: String,
    val maxMinutes: Int,
    val onCheckedChange: (Boolean) -> Unit,
    val onMinutesChange: (Int) -> Unit,
)

@Composable
internal fun EmergencyAssistScreen(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    username: String,
    onUsernameChange: (String) -> Unit,
    previewText: String,
    contacts: List<EmergencyContact>,
    onAddContact: () -> Unit,
    onRemoveContact: (EmergencyContact) -> Unit,
    reasons: List<AssistReasonState>,
    onTest: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var pendingRemoval by remember { mutableStateOf<EmergencyContact?>(null) }

    SecondaryScreen(title = context.getString(R.string.emergency_messages), onBack = onBack) {
        AssistSwitchRow(
            title = context.getString(R.string.emergency_message_feature),
            checked = enabled,
            boldWhenChecked = false,
            onCheckedChange = onEnabledChange,
            tag = "ea_enable",
        )
        Text(
            text = context.getString(R.string.text_messages_can_be_sent_with_your_location_if_you_do_not_respond_to_alarms_this_feature_is_experimental_and_may_not_be_reliable),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
        )
        OutlinedTextField(
            value = username,
            onValueChange = onUsernameChange,
            singleLine = true,
            label = { Text(context.getString(R.string.your_name)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .testTag("ea_username"),
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                text = previewText,
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(10.dp)
                    .testTag("ea_preview"),
            )
        }
        HorizontalDivider()
        Text(
            text = context.getString(R.string.d_selected_contacts_for_text_messages_format, contacts.size),
            color = if (contacts.isNotEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, top = 5.dp)
                .testTag("ea_contacts_count"),
        )
        contacts.forEach { contact ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("ea_contact_${contact.name}"),
            ) {
                Icon(painter = painterResource(R.drawable.ic_add_alert_grey600_48dp), contentDescription = null)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                ) {
                    Text(contact.name, style = MaterialTheme.typography.titleLarge)
                    Text(contact.number, style = MaterialTheme.typography.bodySmall)
                }
                IconButton(
                    onClick = { pendingRemoval = contact },
                    modifier = Modifier.testTag("ea_contact_delete_${contact.name}"),
                ) {
                    Icon(painter = painterResource(R.drawable.ic_delete_forever_grey_600_24dp), contentDescription = null)
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.End,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            IconButton(onClick = onAddContact, modifier = Modifier.testTag("ea_add_contact")) {
                Icon(painter = painterResource(R.drawable.ic_group_add_grey_500_24dp), contentDescription = null)
            }
        }
        HorizontalDivider()
        Text(
            text = context.getString(R.string.choose_when_to_send_messages),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, top = 8.dp),
        )
        reasons.forEach { reason ->
            AssistSwitchRow(
                title = reason.title,
                checked = reason.checked,
                boldWhenChecked = true,
                onCheckedChange = reason.onCheckedChange,
                tag = "ea_${reason.tag}_switch",
            )
            if (reason.checked) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                ) {
                    Slider(
                        value = reason.minutes.toFloat(),
                        onValueChange = { reason.onMinutesChange(it.roundToInt()) },
                        valueRange = 0f..reason.maxMinutes.toFloat(),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ea_${reason.tag}_slider"),
                    )
                    Text(
                        text = reason.minutesText,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .widthIn(min = 100.dp),
                    )
                }
            }
        }
        HorizontalDivider()
        if (contacts.isNotEmpty()) {
            Button(
                onClick = onTest,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp)
                    .testTag("ea_test"),
            ) {
                Text(context.getString(R.string.test_message_sending))
            }
        }
    }

    pendingRemoval?.let { contact ->
        AlertDialog(
            onDismissRequest = { pendingRemoval = null },
            title = { Text("Remove?") },
            text = { Text("Remove ${contact.name} from emergency text message receivers list?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingRemoval = null
                        onRemoveContact(contact)
                    },
                ) {
                    Text(context.getString(R.string.yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemoval = null }) {
                    Text(context.getString(R.string.no))
                }
            },
        )
    }
}

@Composable
private fun AssistSwitchRow(
    title: String,
    checked: Boolean,
    boldWhenChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (boldWhenChecked && checked) FontWeight.Bold else null,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(tag),
        )
    }
}

/**
 * Reproduces `PrefsViewStringSnapDefaults.get`: an empty or "0" value is treated as the default
 * (and written back), except for the lowest-alert threshold which has no default in the legacy
 * screen and therefore stays at zero.
 */
internal fun snapMinutesValue(key: String, value: String): String =
    if (value.isEmpty() || value == "0") {
        when (key) {
            KEY_LOW_MINS -> "60"
            KEY_HIGH_MINS -> "240"
            KEY_INACTIVITY_MINS -> "1440"
            else -> value
        }
    } else {
        value
    }

private fun snappedMinutes(key: String): String {
    val raw = Pref.getString(key, "")
    val snapped = snapMinutesValue(key, raw)
    if (snapped != raw) {
        Pref.setString(key, snapped)
    }
    return snapped
}

private fun updateMinutes(key: String, minutes: Int): String {
    val effective = snapMinutesValue(key, minutes.toString())
    Pref.setString(key, effective)
    return effective
}

// region Previews

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun EmergencyAssistScreenPreview() {
    XdripPreview {
        EmergencyAssistScreen(
            enabled = true,
            onEnabledChange = {},
            username = "Alex",
            onUsernameChange = {},
            previewText = "Emergency message preview",
            contacts = listOf(EmergencyContact("Sam", "+15550100")),
            onAddContact = {},
            onRemoveContact = {},
            reasons = listOf(
                AssistReasonState(
                    tag = "no_signal",
                    title = "No signal",
                    checked = false,
                    minutes = 20,
                    minutesText = "20 minutes",
                    maxMinutes = 120,
                    onCheckedChange = {},
                    onMinutesChange = {},
                ),
            ),
            onTest = {},
            onBack = {},
        )
    }
}

// endregion
