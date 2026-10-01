@file:JvmName("SelectAudioDeviceScreen")

package com.eveningoutpost.dexdrip.ui.secondary

import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.eveningoutpost.dexdrip.R
import com.eveningoutpost.dexdrip.models.JoH
import com.eveningoutpost.dexdrip.ui.activities.SelectAudioDevice
import com.eveningoutpost.dexdrip.ui.theme.XdripPreview
import com.eveningoutpost.dexdrip.utils.HeadsetStateReceiver
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Track V — `SelectAudioDevice`: live selection of the current Bluetooth audio device for vehicle
 * mode. Polls [HeadsetStateReceiver] while resumed. The static [SelectAudioDevice.getAudioMac] /
 * `setAudioMac` helpers are retained for the receiver. Java entry point: [install].
 */
fun installSelectAudioDevice(activity: ComponentActivity) {
    if (HeadsetStateReceiver.getLastConnectedMac().isEmpty()) {
        JoH.static_toast_long(activity.getString(R.string.no_bluetooth_audio_found))
        activity.finish()
        return
    }
    activity.setContent {
        SelectAudioDeviceScreen(
            onCancel = { activity.finish() },
            onSave = { mac, name ->
                SelectAudioDevice.setAudioMac(mac)
                JoH.static_toast_long("Set to: $name $mac")
                HeadsetStateReceiver.reprocessConnectionIfAlreadyConnected(mac)
                activity.finish()
            },
        )
    }
}

@Composable
internal fun SelectAudioDeviceScreen(
    onSave: (mac: String, name: String) -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var name by remember { mutableStateOf(HeadsetStateReceiver.getLastConnectedName()) }
    var mac by remember { mutableStateOf(HeadsetStateReceiver.getLastConnectedMac()) }
    var resumed by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> resumed = true
                Lifecycle.Event.ON_PAUSE -> resumed = false
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(resumed) {
        while (resumed && isActive) {
            name = HeadsetStateReceiver.getLastConnectedName()
            mac = HeadsetStateReceiver.getLastConnectedMac()
            delay(500)
        }
    }

    SecondaryScreen(title = context.getString(R.string.learn_current_car_audio), onBack = onCancel) {
        Image(
            painter = painterResource(R.drawable.ic_car_connected_grey600_48dp),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 24.dp),
        )
        Text(
            text = context.getString(R.string.when_connected_to_the_audio_device_shown_below_switch_to_vehicle_mode),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
        )
        Text(
            text = name,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 8.dp)
                .testTag("audio_name"),
        )
        Text(
            text = mac,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .testTag("audio_mac"),
        )
        Row(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 24.dp),
        ) {
            Button(
                onClick = { onSave(mac, name) },
                modifier = Modifier.testTag("audio_save"),
            ) {
                Text(context.getString(R.string.yes))
            }
            Spacer(Modifier.width(16.dp))
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.testTag("audio_cancel"),
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
private fun SelectAudioDeviceScreenPreview() {
    XdripPreview {
        SelectAudioDeviceScreen(onSave = { _, _ -> }, onCancel = {})
    }
}

// endregion
