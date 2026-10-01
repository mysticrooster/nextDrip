package com.eveningoutpost.dexdrip.ui.settings

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.eveningoutpost.dexdrip.R
import com.eveningoutpost.dexdrip.cgm.ilet.ILetEntry
import com.eveningoutpost.dexdrip.insulin.InsulinPumps
import com.eveningoutpost.dexdrip.utilitymodels.Pref
import com.eveningoutpost.dexdrip.utils.DexCollectionType

/**
 * `Devices → Insulin Pumps`: pick the active pump driver and, for a CGM-capable
 * pump, make it the primary glucose source.
 *
 * The selector is general and lists pumps regardless of CGM support. Choosing a
 * pump only activates its driver (pump-only mode is fine); the "Use … as glucose
 * source" toggle is what sets `dex_collection_method`, remembering the previous
 * collector so it can be restored on toggle-off.
 */
@Composable
internal fun InsulinPumpsScreen(onNavigate: (SettingsScreen) -> Unit) {
    val context = LocalContext.current
    val state = rememberSettingsState()
    val entries = context.resources.getStringArray(R.array.InsulinPumpEntries).toList()
    val values = context.resources.getStringArray(R.array.InsulinPumpValues).toList()
    val selected = state.string(InsulinPumps.PREF_SELECTED, InsulinPumps.NONE)
    val pump = InsulinPumps.pumpFor(selected)

    SettingsCategory("Insulin Pumps") {
        SettingsListRow(
            title = "Insulin pump",
            subtitle = "Pick the pump xDrip should read",
            entries = entries,
            values = values,
            selectedValue = selected,
            onSelected = { newValue ->
                // Switching pump: if the previous one was the glucose source, hand
                // the collector back before the change.
                InsulinPumps.pumpFor(state.string(InsulinPumps.PREF_SELECTED, InsulinPumps.NONE))
                    ?.let { stopGlucoseSource(state, it) }
                state.setString(InsulinPumps.PREF_SELECTED, newValue)
                applyPumpSelection(context, newValue)
            },
            modifier = Modifier.testTag("setting_insulin_pump"),
        )

        if (pump != null) {
            if (pump.canBeGlucoseSource) {
                PumpGlucoseSourceRow(pump = pump, state = state)
            }
            SettingsActionRow(
                title = "${pump.value} settings",
                subtitle = "Account, data options and read-only notice",
                icon = Icons.Outlined.MonitorHeart,
                onClick = { onNavigate(pumpSettingsScreen(pump)) },
                modifier = Modifier.testTag("setting_pump_settings"),
            )
        }
    }
}

@Composable
private fun PumpGlucoseSourceRow(pump: InsulinPumps.Pump, state: SettingsState) {
    val collectorName = pump.collectorName ?: return
    val current = state.string(DexCollectionType.DEX_COLLECTION_METHOD, DexCollectionType.Disabled.internalName)
    val checked = current == collectorName
    var pending by remember { mutableStateOf<Boolean?>(null) }

    ListItem(
        headlineContent = { Text("Use ${pump.value} as glucose source") },
        supportingContent = {
            Text(
                if (checked) "Hardware Data Source is ${pump.value}"
                else "Pump-only: another collector supplies glucose"
            )
        },
        trailingContent = { Switch(checked = checked, onCheckedChange = { pending = it }) },
        modifier = Modifier
            .fillMaxWidth()
            .clickable { pending = !checked }
            .testTag("setting_pump_glucose_source"),
    )
    HorizontalDivider()

    pending?.let { desired ->
        // Changing the collector restarts the collection service and can alter
        // calibration/follower behaviour, so confirm before applying.
        AlertDialog(
            onDismissRequest = { pending = null },
            title = {
                Text(if (desired) "Use ${pump.value} as glucose source?" else "Stop using ${pump.value} for glucose?")
            },
            text = {
                Text(
                    if (desired) {
                        "The Hardware Data Source will be set to ${pump.value}; the current collector may stop."
                    } else {
                        "The Hardware Data Source will return to its previous collector."
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pending = null
                        setGlucoseSource(pump, desired, state)
                    },
                ) { Text("Confirm") }
            },
            dismissButton = { TextButton(onClick = { pending = null }) { Text("Cancel") } },
            modifier = Modifier.testTag("setting_pump_glucose_confirm"),
        )
    }
}

private fun pumpSettingsScreen(pump: InsulinPumps.Pump): SettingsScreen = when (pump.value) {
    "iLet" -> SettingsScreen.ILetDevice
    else -> SettingsScreen.InsulinPumps
}

/** Activate the chosen driver; only iLet has a driver today. */
private fun applyPumpSelection(context: Context, value: String) {
    val pump = InsulinPumps.pumpFor(value)
    InsulinPumps.setDriverEnabled(pump, pump != null)
    if (pump?.value == "iLet") {
        ILetEntry.startIfEnabled(context)
    } else {
        ILetEntry.stop(context)
    }
}

/** Give the collector back if this pump currently owns it. */
private fun stopGlucoseSource(state: SettingsState, pump: InsulinPumps.Pump) {
    val collectorName = pump.collectorName ?: return
    val current = Pref.getString(DexCollectionType.DEX_COLLECTION_METHOD, DexCollectionType.Disabled.internalName)
    if (current != collectorName) return
    val restore = InsulinPumps.previousCollector()
    applyCollectionMethodChange(restore)
    state.setString(DexCollectionType.DEX_COLLECTION_METHOD, restore)
}

private fun setGlucoseSource(pump: InsulinPumps.Pump, enabled: Boolean, state: SettingsState) {
    val collectorName = pump.collectorName ?: return
    val current = Pref.getString(DexCollectionType.DEX_COLLECTION_METHOD, DexCollectionType.Disabled.internalName)
    if (enabled) {
        if (current != collectorName) {
            InsulinPumps.rememberCollector(current)
            applyCollectionMethodChange(collectorName)
            state.setString(DexCollectionType.DEX_COLLECTION_METHOD, collectorName)
        }
        // Being the glucose source implies the pump is the active one.
        state.setString(InsulinPumps.PREF_SELECTED, pump.value)
        InsulinPumps.setDriverEnabled(pump, true)
    } else if (current == collectorName) {
        val restore = InsulinPumps.previousCollector()
        applyCollectionMethodChange(restore)
        state.setString(DexCollectionType.DEX_COLLECTION_METHOD, restore)
    }
}
