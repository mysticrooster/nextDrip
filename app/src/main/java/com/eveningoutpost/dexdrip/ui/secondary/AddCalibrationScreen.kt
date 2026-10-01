@file:JvmName("AddCalibrationScreen")

package com.eveningoutpost.dexdrip.ui.secondary

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.eveningoutpost.dexdrip.AddCalibration
import com.eveningoutpost.dexdrip.R

/**
 * Track V pass 5 — `AddCalibration`: single blood glucose entry. The activity keeps the
 * calibration/blood-test side effects and returns a validation error (or null) from
 * `saveCalibration`. Java entry point: [installAddCalibration].
 */
fun installAddCalibration(activity: AddCalibration) {
    activity.setContent {
        AddCalibrationScreen(
            onDone = { activity.saveCalibration(it) },
            onBack = { activity.finish() },
        )
    }
}

@Composable
internal fun AddCalibrationScreen(
    onDone: (String) -> String?,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var value by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    SecondaryScreen(title = context.getString(R.string.add_calibration), onBack = onBack) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                value = it
                error = null
            },
            singleLine = true,
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            placeholder = { Text(context.getString(R.string.enter_blood_glucose_value)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .testTag("addcal_value"),
        )
        Button(
            onClick = { error = onDone(value) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("addcal_done"),
        ) {
            Text(context.getString(R.string.done))
        }
    }
}
