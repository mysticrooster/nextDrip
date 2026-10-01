@file:JvmName("CalibrationOverrideScreen")

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
import com.eveningoutpost.dexdrip.GcmActivity
import com.eveningoutpost.dexdrip.Home
import com.eveningoutpost.dexdrip.R
import com.eveningoutpost.dexdrip.calibrations.NativeCalibrationPipe
import com.eveningoutpost.dexdrip.models.Calibration
import com.eveningoutpost.dexdrip.models.JoH
import com.eveningoutpost.dexdrip.models.Sensor
import com.eveningoutpost.dexdrip.models.UserError.Log
import com.eveningoutpost.dexdrip.ui.theme.XdripPreview
import com.eveningoutpost.dexdrip.utilitymodels.CalibrationSendQueue
import com.eveningoutpost.dexdrip.utilitymodels.CollectionServiceStarter
import com.eveningoutpost.dexdrip.utilitymodels.UndoRedo

/**
 * Track V pass 2 — `CalibrationOverride`: overriding the previous calibration with a new value.
 * Java entry point: [installCalibrationOverride].
 */
fun installCalibrationOverride(activity: ComponentActivity) {
    if (CollectionServiceStarter.isBTShare(activity)) {
        activity.startActivity(Intent(activity, Home::class.java))
        activity.finish()
        return
    }
    activity.setContent {
        CalibrationOverrideScreen(
            onBack = { activity.finish() },
            onSubmit = { raw -> handleOverride(activity, raw) },
        )
    }
}

private fun handleOverride(activity: ComponentActivity, raw: String): String? {
    if (raw.isEmpty()) return "Calibration Can Not be blank"
    if (!Sensor.isActive()) {
        Log.w("Calibration", "ERROR, no active sensor")
        return null
    }
    return try {
        val calValue = JoH.tolerantParseDouble(raw)
        val lastCalibration = Calibration.lastValid()
        if (lastCalibration == null) {
            Log.wtf("OverrideCalib", "Last valid calibration is null when trying to cancel it in override!")
        } else {
            lastCalibration.sensor_confidence = 0.0
            lastCalibration.slope_confidence = 0.0
            lastCalibration.save()
            CalibrationSendQueue.addToQueue(lastCalibration, activity)
        }
        val calibration = Calibration.create(calValue, activity)
        if (calibration != null) {
            UndoRedo.addUndoCalibration(calibration.uuid)
            GcmActivity.pushCalibration(raw, "0")
            NativeCalibrationPipe.addCalibration(calibration.bg.toInt(), calibration.timestamp)
        } else {
            JoH.static_toast_long("Could not create calibration!")
        }
        activity.startActivity(Intent(activity, Home::class.java))
        activity.finish()
        null
    } catch (e: NumberFormatException) {
        activity.getString(R.string.number_error_) + e
    }
}

@Composable
internal fun CalibrationOverrideScreen(
    onBack: () -> Unit,
    onSubmit: (String) -> String?,
) {
    val context = LocalContext.current
    var text by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    SecondaryScreen(title = context.getString(R.string.title_activity_calibration_override), onBack = onBack) {
        Text(
            text = context.getString(R.string.entering_a_calibration_now_will_override_your_previously_entered_calibration),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
        )
        OutlinedTextField(
            value = text,
            onValueChange = { text = it; error = null },
            singleLine = true,
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            placeholder = { Text(context.getString(R.string.enter_blood_glucose_value)) },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .testTag("override_bg_value"),
        )
        Button(
            onClick = { error = onSubmit(text) },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 24.dp)
                .testTag("override_save"),
        ) {
            Text(context.getString(R.string.done))
        }
    }
}

// region Previews

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun CalibrationOverrideScreenPreview() {
    XdripPreview {
        CalibrationOverrideScreen(onBack = {}, onSubmit = { null })
    }
}

// endregion
