@file:JvmName("GluProScreen")

package com.eveningoutpost.dexdrip.ui.secondary

import android.content.res.Configuration
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.databinding.Observable
import androidx.databinding.ObservableList
import com.eveningoutpost.dexdrip.R
import com.eveningoutpost.dexdrip.cgm.glupro.GluProActivity
import com.eveningoutpost.dexdrip.cgm.glupro.ViewModel
import com.eveningoutpost.dexdrip.ui.theme.XdripPreview
import lwld.glucose.profile.iface.Device

/**
 * Track V pass 3 (Medium) — `GluProActivity`: scanned Glucose Profile device selection. Keeps the
 * shared [ViewModel] (also driven by the service) and bridges its observable list/boolean into
 * Compose. Java entry point: [installGluPro].
 */
fun installGluPro(activity: GluProActivity) {
    activity.setContent {
        GluProScreen(viewModel = activity.viewModel, onBack = { activity.finish() })
    }
}

@Composable
internal fun GluProScreen(viewModel: ViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    var devices by remember { mutableStateOf(viewModel.scannedDevices.toList()) }
    var scanning by remember { mutableStateOf(viewModel.scanning.get()) }

    DisposableEffect(viewModel) {
        val listCallback = object : ObservableList.OnListChangedCallback<ObservableList<Device>>() {
            override fun onChanged(sender: ObservableList<Device>) { devices = viewModel.scannedDevices.toList() }
            override fun onItemRangeChanged(sender: ObservableList<Device>, positionStart: Int, itemCount: Int) { devices = viewModel.scannedDevices.toList() }
            override fun onItemRangeInserted(sender: ObservableList<Device>, positionStart: Int, itemCount: Int) { devices = viewModel.scannedDevices.toList() }
            override fun onItemRangeRemoved(sender: ObservableList<Device>, positionStart: Int, itemCount: Int) { devices = viewModel.scannedDevices.toList() }
            override fun onItemRangeMoved(sender: ObservableList<Device>, fromPosition: Int, toPosition: Int, itemCount: Int) { devices = viewModel.scannedDevices.toList() }
        }
        viewModel.scannedDevices.addOnListChangedCallback(listCallback)
        val scanningCallback = object : Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable, propertyId: Int) { scanning = viewModel.scanning.get() }
        }
        viewModel.scanning.addOnPropertyChangedCallback(scanningCallback)
        onDispose {
            viewModel.scannedDevices.removeOnListChangedCallback(listCallback)
            viewModel.scanning.removeOnPropertyChangedCallback(scanningCallback)
        }
    }

    SecondaryScreen(title = context.getString(R.string.glupro_activity_title), onBack = onBack) {
        Text(
            text = context.getString(R.string.smartguide_pairing),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.fillMaxWidth().padding(10.dp),
        )
        devices.forEach { device ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().testTag("glupro_device_${device.address}"),
            ) {
                Text(
                    text = device.name,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f).padding(start = 4.dp),
                )
                Button(onClick = { viewModel.select(device.name, device.address) }) {
                    Text("Connect")
                }
            }
        }
        Image(
            painter = painterResource(R.drawable.smartguide_pin_help),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 16.dp),
        )
        if (scanning) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
            Button(
                onClick = { viewModel.select(null, null) },
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .testTag("glupro_scan_again"),
            ) {
                Text(context.getString(R.string.glupro_scan_again))
            }
        }
        Spacer(Modifier.height(16.dp))
        Spacer(Modifier.width(0.dp))
    }
}

// region Previews

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun GluProScreenPreview() {
    XdripPreview {
        GluProScreen(viewModel = ViewModel(), onBack = {})
    }
}

// endregion
