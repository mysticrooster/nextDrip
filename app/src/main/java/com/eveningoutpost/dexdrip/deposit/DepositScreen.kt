@file:JvmName("DepositScreen")

package com.eveningoutpost.dexdrip.deposit

import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.eveningoutpost.dexdrip.R
import com.eveningoutpost.dexdrip.models.JoH
import com.eveningoutpost.dexdrip.ui.secondary.SecondaryScreen
import com.eveningoutpost.dexdrip.ui.settings.rememberSettingsState
import com.eveningoutpost.dexdrip.ui.theme.XdripPreview
import com.eveningoutpost.dexdrip.utilitymodels.Constants

/**
 * Track V — `DepositActivity`: Web Deposit status and upload controls. The Data-Binding
 * `ObservableField` view model is replaced with Compose state; the async `WebDeposit` callbacks are
 * wired through [F]. Java entry point: [installDeposit].
 */
fun installDeposit(activity: ComponentActivity) {
    activity.setContent {
        DepositScreen(onBack = { activity.finish() })
    }
}

@Composable
internal fun DepositScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val settings = rememberSettingsState()
    var status by remember { mutableStateOf("Ready") }
    var serialInfo by remember { mutableStateOf("") }
    var startTime by remember { mutableLongStateOf(0L) }
    var endTime by remember { mutableLongStateOf(0L) }
    var showButton by remember { mutableStateOf(true) }
    var showReset by remember { mutableStateOf(false) }

    fun updateBlock() {
        startTime = WebDeposit.getNextTime()
        endTime = minOf(startTime + Constants.MONTH_IN_MS, JoH.tsl() - Constants.HOUR_IN_MS * 8)
    }

    LaunchedEffect(Unit) {
        updateBlock()
        serialInfo = WebDeposit.getSerialInfo()
    }

    val successG = F { s ->
        showButton = true
        if (s == "OK") {
            status = "Succeeded Ok!"
            WebDeposit.setNextTime(endTime + (Constants.HOUR_IN_MS * 3.9).toLong())
            updateBlock()
        } else {
            status = "Failed with message: $s"
        }
    }
    val successT = F { s ->
        showButton = true
        status = if (s == "OK") "Succeeded Ok!" else "Failed with message: $s"
    }
    val failure = F { s ->
        status = "Failure: $s"
        showButton = true
    }
    val statusCallback = F { s -> status = s }

    val depositG: () -> Unit = {
        showButton = false
        Thread { WebDeposit.doUploadByType("G", startTime, endTime, successG, failure, statusCallback) }.start()
    }
    val depositT: () -> Unit = {
        showButton = false
        Thread {
            WebDeposit.doUploadByType(
                "T",
                JoH.tsl() - Constants.MONTH_IN_MS * 3,
                JoH.tsl(),
                successT,
                failure,
                statusCallback,
            )
        }.start()
    }

    SecondaryScreen(title = "Web Deposit", onBack = onBack) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = status,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .testTag("deposit_status"),
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(8.dp),
        ) {
            Text(JoH.dateTimeText(startTime), fontFamily = FontFamily.Monospace)
            Text("to", modifier = Modifier.padding(vertical = 4.dp))
            Text(JoH.dateTimeText(endTime), fontFamily = FontFamily.Monospace)
        }
        Text(
            text = serialInfo,
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("deposit_serial"),
        )
        if (!showButton) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Button(
                    onClick = depositG,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("deposit_g"),
                ) {
                    Text("Deposit Glucose")
                }
                Button(
                    onClick = depositT,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                        .testTag("deposit_t"),
                ) {
                    Text("Deposit Treatment")
                }
            }
        }
        if (showButton && settings.bool("engineering_mode", false)) {
            OutlinedButton(
                onClick = { showReset = true },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 24.dp)
                    .testTag("deposit_reset"),
            ) {
                Text("!!! RESET !!!")
            }
        }
    }

    if (showReset) {
        AlertDialog(
            onDismissRequest = { showReset = false },
            title = { Text("Confirm Reset") },
            text = { Text("Resetting could cause data overlap or other problems, you must be absolutely sure before using it") },
            confirmButton = {
                TextButton(onClick = {
                    WebDeposit.setNextTime(0L)
                    updateBlock()
                    status = "Reset data sequence!"
                    showReset = false
                }) {
                    Text(context.getString(R.string.yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { showReset = false }) {
                    Text(context.getString(R.string.no))
                }
            },
        )
    }
}

// region Previews

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun DepositScreenPreview() {
    XdripPreview {
        DepositScreen(onBack = {})
    }
}

// endregion
