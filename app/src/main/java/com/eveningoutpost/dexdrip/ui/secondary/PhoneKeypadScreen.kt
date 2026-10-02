@file:JvmName("PhoneKeypadScreen")

package com.eveningoutpost.dexdrip.ui.secondary

import android.content.res.Configuration
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.databinding.Observable
import com.eveningoutpost.dexdrip.PhoneKeypadInputActivity
import com.eveningoutpost.dexdrip.R
import com.eveningoutpost.dexdrip.ui.theme.XdripPreview

private val TABS = listOf(
    "insulin-1" to "Insulin",
    "carbs" to "Carbs",
    "bloodtest" to "Blood test",
    "time" to "Time",
)

/**
 * Track V — `PhoneKeypadInputActivity`: system-keyboard treatment entry form (insulin/carbs/
 * blood-test/time tabs, up to three insulin profiles). Preserves the `WEARABLE_VOICE_PAYLOAD`
 * submit contract and the persisted last tab. Java entry point: [installPhoneKeypad].
 */
fun installPhoneKeypad(activity: PhoneKeypadInputActivity) {
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

        val tab = remember(tick) { activity.currentTab }
        var text by remember(tab) { mutableStateOf(activity.currentValue) }
        val hasValue = remember(tick) { activity.hasAnyValue() }

        PhoneKeypadScreen(
            currentTab = tab,
            text = text,
            suffix = remember(tick) { activity.suffix },
            multipleInsulins = activity.isMultipleInsulins(),
            insulinProfileNames = listOf(
                activity.getInsulinProfileName(1),
                activity.getInsulinProfileName(2),
                activity.getInsulinProfileName(3),
            ),
            activeInsulinProfile = remember(tick) { activity.activeInsulinProfile },
            hasValue = hasValue,
            invalidTime = { activity.isInvalidTime() },
            onTabSelected = { activity.currentTab = it },
            onInsulinProfileSelected = { activity.selectInsulinProfile(it) },
            onTextChange = {
                activity.setCurrentValue(it)
                text = activity.currentValue
            },
            onClear = {
                activity.clearCurrentValue()
                text = activity.currentValue
            },
            onSubmit = { activity.submitAll() },
            onSpeech = { activity.startSpeechRecognition() },
            onTextRecognition = { activity.startTextRecognition() },
            onBack = { activity.finish() },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PhoneKeypadScreen(
    currentTab: String,
    text: String,
    suffix: String,
    multipleInsulins: Boolean,
    insulinProfileNames: List<String?>,
    activeInsulinProfile: Int,
    hasValue: Boolean,
    invalidTime: () -> Boolean,
    onTabSelected: (String) -> Unit,
    onInsulinProfileSelected: (Int) -> Unit,
    onTextChange: (String) -> Unit,
    onClear: () -> Unit,
    onSubmit: () -> Unit,
    onSpeech: () -> Unit,
    onTextRecognition: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var showInvalidTime by remember { mutableStateOf(false) }
    val selectedBase = currentTab.split("-")[0]

    SecondaryScreen(title = "Keypad Input", onBack = onBack) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            TABS.forEach { (key, label) ->
                FilterChip(
                    selected = selectedBase == key.split("-")[0],
                    onClick = { onTabSelected(key) },
                    label = { Text(label) },
                    modifier = Modifier.testTag("keypad_tab_${key.split("-")[0]}"),
                )
            }
        }
        if (multipleInsulins) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
            ) {
                insulinProfileNames.forEachIndexed { index, name ->
                    if (name != null) {
                        FilterChip(
                            selected = selectedBase == "insulin" && activeInsulinProfile == index + 1,
                            onClick = { onInsulinProfileSelected(index + 1) },
                            label = { Text(name) },
                            modifier = Modifier.testTag("keypad_insulin_${index + 1}"),
                        )
                    }
                }
            }
        }
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            singleLine = true,
            suffix = { Text(suffix) },
            keyboardOptions = KeyboardOptions(
                keyboardType = if (selectedBase == "time") KeyboardType.Number else KeyboardType.Decimal,
            ),
            textStyle = MaterialTheme.typography.headlineMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag("keypad_value"),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Button(
                onClick = {
                    if (invalidTime()) showInvalidTime = true else onSubmit()
                },
                enabled = hasValue,
                modifier = Modifier
                    .weight(1f)
                    .testTag("keypad_submit"),
            ) {
                Text(context.getString(R.string.done))
            }
            OutlinedButton(
                onClick = onClear,
                modifier = Modifier
                    .weight(1f)
                    .testTag("keypad_clear"),
            ) {
                Text("Clear")
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            OutlinedButton(
                onClick = onSpeech,
                modifier = Modifier
                    .weight(1f)
                    .testTag("keypad_speak"),
            ) {
                Text(context.getString(R.string.speak_your_treatment))
            }
            OutlinedButton(
                onClick = onTextRecognition,
                modifier = Modifier
                    .weight(1f)
                    .testTag("keypad_type"),
            ) {
                Text("Type instead")
            }
        }
    }

    if (showInvalidTime) {
        AlertDialog(
            onDismissRequest = { showInvalidTime = false },
            title = { Text("Invalid time") },
            text = { Text("Please enter a valid time or clear the time tab.") },
            confirmButton = {
                TextButton(onClick = { showInvalidTime = false }) {
                    Text(context.getString(android.R.string.ok))
                }
            },
        )
    }
}

// region Previews

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun PhoneKeypadScreenPreview() {
    XdripPreview {
        PhoneKeypadScreen(
            currentTab = "insulin-1",
            text = "4.5",
            suffix = " units",
            multipleInsulins = false,
            insulinProfileNames = listOf("Novorapid", null, null),
            activeInsulinProfile = 1,
            hasValue = true,
            invalidTime = { false },
            onTabSelected = {},
            onInsulinProfileSelected = {},
            onTextChange = {},
            onClear = {},
            onSubmit = {},
            onSpeech = {},
            onTextRecognition = {},
            onBack = {},
        )
    }
}

// endregion
