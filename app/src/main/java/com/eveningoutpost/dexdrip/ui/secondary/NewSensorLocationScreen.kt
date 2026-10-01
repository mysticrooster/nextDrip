@file:JvmName("NewSensorLocationScreen")

package com.eveningoutpost.dexdrip.ui.secondary

import android.content.res.Configuration
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.eveningoutpost.dexdrip.NewSensorLocation
import com.eveningoutpost.dexdrip.R
import com.eveningoutpost.dexdrip.ui.theme.XdripPreview

internal const val LOCATION_PRIVATE_ID = 200
internal const val LOCATION_OTHER_ID = 201

/** The legacy (hardcoded, untranslated) sensor location options. */
internal data class SensorLocationOption(val label: String, val id: Int)

internal val SENSOR_LOCATIONS = listOf(
    SensorLocationOption("I don't wish to share", LOCATION_PRIVATE_ID),
    SensorLocationOption("Upper arm", 1),
    SensorLocationOption("Thigh", 2),
    SensorLocationOption("Belly (abdomen)", 3),
    SensorLocationOption("Lower back", 4),
    SensorLocationOption("Buttocks", 5),
    SensorLocationOption("Other", LOCATION_OTHER_ID),
)

/**
 * Track V pass 5 — `NewSensorLocation`: radio list of sensor placement options with an "Other"
 * free-text field. Java entry point: [installNewSensorLocation].
 */
fun installNewSensorLocation(activity: NewSensorLocation) {
    activity.setContent {
        NewSensorLocationScreen(
            onSave = { activity.saveLocation(it) },
            onCancel = { activity.cancelLocation() },
        )
    }
}

@Composable
internal fun NewSensorLocationScreen(
    onSave: (String) -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    var selectedId by remember { mutableStateOf(LOCATION_PRIVATE_ID) }
    var otherText by remember { mutableStateOf("") }
    var dontAskAgain by remember { mutableStateOf(false) }

    SecondaryScreen(title = context.getString(R.string.title_activity_new_sensor_location), onBack = onCancel) {
        Text(
            text = context.getString(R.string.description_sensor_location),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        SENSOR_LOCATIONS.forEach { option ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedId = option.id }
                    .padding(horizontal = 8.dp),
            ) {
                RadioButton(
                    selected = selectedId == option.id,
                    onClick = { selectedId = option.id },
                    modifier = Modifier.testTag("nsl_radio_${option.id}"),
                )
                Text(option.label, style = MaterialTheme.typography.bodyLarge)
            }
        }
        OutlinedTextField(
            value = otherText,
            onValueChange = { otherText = it },
            enabled = selectedId == LOCATION_OTHER_ID,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("nsl_other"),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { dontAskAgain = !dontAskAgain }
                .padding(horizontal = 8.dp),
        ) {
            Checkbox(
                checked = dontAskAgain,
                onCheckedChange = { dontAskAgain = it },
                modifier = Modifier.testTag("nsl_dont_ask"),
            )
            Text(context.getString(R.string.pref_sensor_location_dont_ask_again))
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Button(
                onClick = {
                    val location = if (selectedId == LOCATION_OTHER_ID) {
                        otherText
                    } else {
                        SENSOR_LOCATIONS.first { it.id == selectedId }.label
                    }
                    onSave(location)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("nsl_save"),
            ) {
                Text(context.getString(R.string.save_sensor_location))
            }
            Button(
                onClick = onCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .testTag("nsl_cancel"),
            ) {
                Text(context.getString(R.string.cancel))
            }
        }
    }
}

// region Previews

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun NewSensorLocationScreenPreview() {
    XdripPreview {
        NewSensorLocationScreen(onSave = {}, onCancel = {})
    }
}

// endregion
