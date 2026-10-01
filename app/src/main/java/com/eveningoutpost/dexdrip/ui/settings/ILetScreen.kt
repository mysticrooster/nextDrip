package com.eveningoutpost.dexdrip.ui.settings

import android.content.Intent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.eveningoutpost.dexdrip.cgm.ilet.ILetEntry
import com.eveningoutpost.dexdrip.cgm.ilet.ILetLoginActivity
import com.eveningoutpost.dexdrip.cgm.ilet.IletPrefs

/**
 * S6 — iLet settings (pump data source). Read-only notice, master enable,
 * download toggles, the pump-IOB graph line and a link to the account screen.
 */
@Composable
internal fun ILetScreen() {
    val context = LocalContext.current
    val state = rememberSettingsState()

    SettingsCategory("iLet") {
        Text(
            "Read-only. This is not medical software and cannot deliver insulin or change " +
                "therapy. Credentials are stored encrypted on this device and never logged.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        SwitchPref(
            state = state,
            key = IletPrefs.ENABLED,
            title = "Enable iLet",
            subtitle = "Read pump data; off by default",
            onCheckedChange = { enabled ->
                state.setBool(IletPrefs.ENABLED, enabled)
                if (enabled) ILetEntry.startIfEnabled(context) else ILetEntry.stop(context)
            },
            tag = "setting_ilet_enabled",
        )

        EditPref(
            state = state,
            key = IletPrefs.LAST_ADDRESS,
            title = "Pump Bluetooth address",
            default = "",
            subtitle = "Optional. Leave blank to scan for the iLet on first connection.",
            tag = "setting_ilet_address",
        )

        val serial = IletPrefs.deviceSerial()
        val address = state.string(IletPrefs.LAST_ADDRESS, "")
        Text(
            buildString {
                append("Pump serial: ")
                append(serial.ifEmpty { "detected on first connection" })
                append("\nBluetooth address: ")
                append(address.ifEmpty { "scan on first connection" })
            },
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        SwitchPref(state, IletPrefs.DOWNLOAD_GLUCOSE, "Use iLet as glucose source", default = true, tag = "setting_ilet_glucose")
        SwitchPref(state, IletPrefs.DOWNLOAD_BOLUSES, "Download boluses", default = true, tag = "setting_ilet_boluses")
        SwitchPref(state, IletPrefs.DOWNLOAD_MEALS, "Download meals", default = true, tag = "setting_ilet_meals")
        SwitchPref(state, IletPrefs.DOWNLOAD_BASAL, "Download basal", default = true, tag = "setting_ilet_basal")
        SwitchPref(
            state,
            IletPrefs.SHOW_PUMP_IOB_LINE,
            "Show pump IOB line",
            default = false,
            subtitle = "Separate from xDrip's treatment-derived IOB",
            tag = "setting_ilet_iob_line",
        )

        SettingsActionRow(
            title = "iLet account",
            subtitle = "Sign in, show status, reset credentials",
            onClick = { context.startActivity(Intent(context, ILetLoginActivity::class.java)) },
            modifier = Modifier.testTag("setting_ilet_account"),
        )
    }
}
