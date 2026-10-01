@file:JvmName("DoubleCalibrationScreen")

package com.eveningoutpost.dexdrip.ui.secondary

import android.content.Intent
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.eveningoutpost.dexdrip.Home
import com.eveningoutpost.dexdrip.R
import com.eveningoutpost.dexdrip.models.Calibration
import com.eveningoutpost.dexdrip.models.JoH
import com.eveningoutpost.dexdrip.models.Sensor
import com.eveningoutpost.dexdrip.models.UserError.Log
import com.eveningoutpost.dexdrip.ui.theme.XdripPreview
import com.eveningoutpost.dexdrip.utilitymodels.CollectionServiceStarter
import com.eveningoutpost.dexdrip.utilitymodels.Constants
import com.eveningoutpost.dexdrip.utilitymodels.Pref

/**
 * Track V pass 2 — `DoubleCalibrationActivity`: initial one/two finger-prick calibration. The second
 * field is only shown when `use_double_calibrations` is enabled. Java entry point:
 * [installDoubleCalibration].
 */
fun installDoubleCalibration(activity: ComponentActivity) {
    if (CollectionServiceStarter.isBTShare(activity)) {
        activity.startActivity(Intent(activity, Home::class.java))
        activity.finish()
        return
    }
    activity.setContent {
        DoubleCalibrationScreen(
            onBack = { activity.finish() },
            onSubmit = { v1, v2 -> handleDoubleCalibration(activity, v1, v2) },
        )
    }
}

private fun handleDoubleCalibration(activity: ComponentActivity, raw1: String, raw2: String): String? {
    if (raw1.isEmpty()) return activity.getString(R.string.calibration_can_not_be_blank)
    if (!Sensor.isActive()) {
        Log.w("DoubleCalibration", "ERROR, sensor is not active")
        return null
    }
    val effective2 = raw2.ifEmpty { raw1 }
    return try {
        val calValue1 = raw1.toDouble()
        val calValue2 = effective2.toDouble()
        val multiplier = if (Pref.getString("units", "mgdl") == "mgdl") 1.0 else Constants.MMOLL_TO_MGDL
        if (calValue1 * multiplier < 40 || calValue1 * multiplier > 400 ||
            calValue2 * multiplier < 40 || calValue2 * multiplier > 400
        ) {
            JoH.static_toast_long(activity.getString(R.string.calibration_out_of_range))
        } else {
            Calibration.initialCalibration(calValue1, calValue2, activity)
            activity.startActivity(Intent(activity, Home::class.java))
            activity.finish()
        }
        null
    } catch (e: NumberFormatException) {
        JoH.static_toast_long(activity.getString(R.string.invalid_calibration_number))
        null
    }
}

@Composable
internal fun DoubleCalibrationScreen(
    onBack: () -> Unit,
    onSubmit: (String, String) -> String?,
) {
    val context = LocalContext.current
    val doubleCalibrations = remember { Pref.getBooleanDefaultFalse("use_double_calibrations") }
    var value1 by remember { mutableStateOf("") }
    var value2 by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    SecondaryScreen(title = context.getString(R.string.title_activity_double_calibration), onBack = onBack) {
        OutlinedTextField(
            value = value1,
            onValueChange = { value1 = it; error = null },
            singleLine = true,
            isError = error != null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            placeholder = { Text(context.getString(R.string.enter_first_bg_value)) },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 16.dp)
                .testTag("double_bg_value_1"),
        )
        if (doubleCalibrations) {
            OutlinedTextField(
                value = value2,
                onValueChange = { value2 = it; error = null },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                placeholder = { Text(context.getString(R.string.enter_second_bg_value)) },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 16.dp)
                    .testTag("double_bg_value_2"),
            )
        }
        Text(
            text = context.getString(
                if (doubleCalibrations) {
                    R.string.in_order_to_get_started_please_perform_two_finger
                } else {
                    R.string.enter_blood_glucose_value
                },
            ),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        )
        error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        Button(
            onClick = { error = onSubmit(value1, value2) },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .testTag("double_save"),
        ) {
            Text(context.getString(R.string.done))
        }
    }
}

// region Previews

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun DoubleCalibrationScreenPreview() {
    XdripPreview {
        DoubleCalibrationScreen(onBack = {}, onSubmit = { _, _ -> null })
    }
}

// endregion
