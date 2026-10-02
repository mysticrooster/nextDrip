@file:JvmName("BTGlucoseMeterScreen")

package com.eveningoutpost.dexdrip.ui.secondary

import android.content.res.Configuration
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.databinding.Observable
import com.eveningoutpost.dexdrip.R
import com.eveningoutpost.dexdrip.glucosemeter.BTGlucoseMeterActivity
import com.eveningoutpost.dexdrip.glucosemeter.BTGlucoseMeterActivity.MyBluetoothDevice
import com.eveningoutpost.dexdrip.ui.theme.XdripPreview

/**
 * Track V (Data & admin) — `BTGlucoseMeterActivity`: scan status + scanned BLE meter list with
 * tap-to-connect and long-press disconnect/forget. The activity keeps the Bluetooth + broadcast
 * state machine; the screen observes [BTGlucoseMeterActivity.tick]. Java entry point:
 * [installBTGlucoseMeter].
 */
fun installBTGlucoseMeter(activity: BTGlucoseMeterActivity) {
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

        val status = remember(tick) { activity.status.get().orEmpty() }
        val devices = remember(tick) { activity.deviceSnapshot }
        val selectedAddress = remember(tick) { activity.selectedAddress }

        BTGlucoseMeterScreen(
            status = status,
            devices = devices,
            selectedAddress = selectedAddress,
            onScan = { activity.startScan() },
            onDeviceClick = { activity.onDeviceClick(it) },
            onDisconnect = { activity.disconnectDevice(it) },
            onForget = { activity.forgetDevice(it) },
            onBack = { activity.finish() },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun BTGlucoseMeterScreen(
    status: String,
    devices: List<MyBluetoothDevice>,
    selectedAddress: String,
    onScan: () -> Unit,
    onDeviceClick: (MyBluetoothDevice) -> Unit,
    onDisconnect: (MyBluetoothDevice) -> Unit,
    onForget: (MyBluetoothDevice) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var actionDevice by remember { mutableStateOf<MyBluetoothDevice?>(null) }

    SecondaryScreen(title = "Meter Scan", onBack = onBack) {
        Text(
            text = status,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("btg_status"),
        )
        Button(
            onClick = onScan,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .testTag("btg_scan"),
        ) {
            Text("Scan")
        }
        devices.forEachIndexed { index, device ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { onDeviceClick(device) },
                        onLongClick = { actionDevice = device },
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .testTag("btg_device_$index"),
            ) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (device.address == selectedAddress) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                Text(
                    text = device.address + if (device.isBonded) "   Paired" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (device.isBonded) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            HorizontalDivider()
        }
    }

    actionDevice?.let { device ->
        AlertDialog(
            onDismissRequest = { actionDevice = null },
            title = { Text("Choose Action") },
            text = { Text("You can disconnect from this device or forget its pairing here") },
            confirmButton = {
                TextButton(
                    onClick = {
                        actionDevice = null
                        onDisconnect(device)
                    },
                ) {
                    Text("Disconnect")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        actionDevice = null
                        onForget(device)
                    },
                ) {
                    Text("Forget Pair")
                }
            },
        )
    }
}

// region Previews

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun BTGlucoseMeterScreenPreview() {
    XdripPreview {
        BTGlucoseMeterScreen(
            status = "Scanning",
            devices = listOf(MyBluetoothDevice("AA:BB^10^Meter One")),
            selectedAddress = "AA:BB",
            onScan = {},
            onDeviceClick = {},
            onDisconnect = {},
            onForget = {},
            onBack = {},
        )
    }
}

// endregion
