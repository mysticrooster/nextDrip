@file:JvmName("CalibrationCheckInScreen")

package com.eveningoutpost.dexdrip.ui.secondary

import android.content.Intent
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.eveningoutpost.dexdrip.Home
import com.eveningoutpost.dexdrip.R
import com.eveningoutpost.dexdrip.importedlibraries.dexcom.SyncingService
import com.eveningoutpost.dexdrip.models.JoH
import com.eveningoutpost.dexdrip.models.Sensor
import com.eveningoutpost.dexdrip.models.UserError.Log
import com.eveningoutpost.dexdrip.ui.theme.XdripPreview

/**
 * Track V pass 2 — `CalibrationCheckInActivity`: request a Dexcom receiver calibration check-in.
 * Java entry point: [installCalibrationCheckIn].
 */
fun installCalibrationCheckIn(activity: ComponentActivity) {
    activity.setContent {
        CalibrationCheckInScreen(
            onBack = { activity.finish() },
            onCheckIn = {
                if (Sensor.isActive()) {
                    SyncingService.startActionCalibrationCheckin(activity)
                    JoH.static_toast_long("Checked in all calibrations")
                    activity.startActivity(Intent(activity, Home::class.java))
                    activity.finish()
                } else {
                    Log.d("CALIBRATION", "ERROR, sensor not active")
                }
            },
        )
    }
}

@Composable
internal fun CalibrationCheckInScreen(
    onBack: () -> Unit,
    onCheckIn: () -> Unit,
) {
    val context = LocalContext.current
    SecondaryScreen(title = context.getString(R.string.title_activity_calibration_check_in), onBack = onBack) {
        Text(
            text = context.getString(R.string.plug_in_your_dexcom_reciever),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
        androidx.compose.material3.Button(
            onClick = onCheckIn,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 24.dp)
                .testTag("check_in_calibrations"),
        ) {
            Text(context.getString(R.string.check_in_dexcom_calibrations))
        }
    }
}

// region Previews

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun CalibrationCheckInScreenPreview() {
    XdripPreview {
        CalibrationCheckInScreen(onBack = {}, onCheckIn = {})
    }
}

// endregion
