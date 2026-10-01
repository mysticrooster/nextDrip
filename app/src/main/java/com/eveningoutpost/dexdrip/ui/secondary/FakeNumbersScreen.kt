@file:JvmName("FakeNumbersScreen")

package com.eveningoutpost.dexdrip.ui.secondary

import android.content.Intent
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.eveningoutpost.dexdrip.models.ActiveBgAlert
import com.eveningoutpost.dexdrip.models.AlertType
import com.eveningoutpost.dexdrip.models.BgReading
import com.eveningoutpost.dexdrip.ui.theme.XdripPreview
import java.util.Date

/**
 * Track V pass 2 — `FakeNumbers`: developer test screen that injects a BG reading and triggers test
 * alerts. Java entry point: [installFakeNumbers].
 */
fun installFakeNumbers(activity: ComponentActivity) {
    activity.setContent {
        FakeNumbersScreen(
            onBack = { activity.finish() },
            onLog = { raw ->
                val intValue = raw.toIntOrNull()
                if (intValue == null) {
                    activity.getString(R.string.calibration_can_not_be_blank)
                } else {
                    val filteredValue = if (intValue > 200) (intValue * 1.2).toInt() else intValue
                    BgReading.create(
                        (intValue * 1000).toDouble(),
                        (filteredValue * 1000).toDouble(),
                        activity,
                        Date().time,
                    )
                    activity.startActivity(Intent(activity, Home::class.java))
                    activity.finish()
                    null
                }
            },
            onStartTest = {
                ActiveBgAlert.getOnly()
                ActiveBgAlert.ClearData()
                ActiveBgAlert.Create("some string", true, Date().time)
            },
            onStartTestAlerts = {
                AlertType.testAll(activity)
                BgReading.TestgetUnclearTimes()
            },
        )
    }
}

@Composable
internal fun FakeNumbersScreen(
    onBack: () -> Unit,
    onLog: (String) -> String?,
    onStartTest: () -> Unit,
    onStartTestAlerts: () -> Unit,
) {
    val context = LocalContext.current
    var value by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    SecondaryScreen(title = context.getString(R.string.title_activity_fake_numbers), onBack = onBack) {
        OutlinedTextField(
            value = value,
            onValueChange = { value = it; error = null },
            singleLine = true,
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            placeholder = { Text(context.getString(R.string.enter_blood_glucose_value)) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("fake_bg_value"),
        )
        Button(
            onClick = { error = onLog(value) },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 16.dp)
                .testTag("fake_log"),
        ) {
            Text(context.getString(R.string.done))
        }
        Row(modifier = Modifier.padding(top = 16.dp)) {
            OutlinedButton(onClick = onStartTest, modifier = Modifier.testTag("fake_start_test")) {
                Text("StartTest")
            }
            OutlinedButton(
                onClick = onStartTestAlerts,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .testTag("fake_start_test_alerts"),
            ) {
                Text("StartTestAlerts")
            }
        }
    }
}

// region Previews

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun FakeNumbersScreenPreview() {
    XdripPreview {
        FakeNumbersScreen(onBack = {}, onLog = { null }, onStartTest = {}, onStartTestAlerts = {})
    }
}

// endregion
