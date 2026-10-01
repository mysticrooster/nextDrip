@file:JvmName("InsulinProfileScreen")

package com.eveningoutpost.dexdrip.ui.secondary

import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.eveningoutpost.dexdrip.R
import com.eveningoutpost.dexdrip.insulin.InsulinManager
import com.eveningoutpost.dexdrip.models.JoH
import com.eveningoutpost.dexdrip.ui.settings.SettingsListRow
import com.eveningoutpost.dexdrip.ui.theme.XdripPreview

/**
 * Track V — `InsulinProfileEditor`: enable/disable insulin profiles and pick the basal/bolus
 * profile. Java entry point: [installInsulinProfileEditor].
 */
fun installInsulinProfileEditor(activity: ComponentActivity) {
    if (InsulinManager.getAllProfiles() == null) {
        JoH.static_toast_long("Can't initialize insulin profiles")
        activity.finish()
        return
    }
    activity.setContent {
        InsulinProfileScreen(
            onSave = { activity.finish() },
            onCancel = { activity.finish() },
        )
    }
}

@Composable
internal fun InsulinProfileScreen(
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    var revision by remember { mutableIntStateOf(0) }
    val profiles = remember(revision) { InsulinManager.getAllProfiles()?.toList().orEmpty() }
    val byName = remember(revision) { profiles.associateBy { it.displayName } }
    val names = remember(revision) { profiles.map { it.displayName } }
    var enabledNames by remember(revision) {
        mutableStateOf(profiles.filter { InsulinManager.isProfileEnabled(it) }.map { it.displayName }.toSet())
    }
    var basalName by remember(revision) { mutableStateOf(InsulinManager.getBasalProfile()?.displayName ?: names.firstOrNull() ?: "") }
    var bolusName by remember(revision) { mutableStateOf(InsulinManager.getBolusProfile()?.displayName ?: names.firstOrNull() ?: "") }

    SecondaryScreen(title = context.getString(R.string.enable_dedicated_insulin_profiles), onBack = onCancel) {
        Text(
            text = context.getString(R.string.pref_enable_insulinprofiles),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp),
        )
        profiles.forEach { insulin ->
            val name = insulin.displayName
            val checked = name in enabledNames
            val toggle = {
                if (checked) {
                    InsulinManager.disableProfile(insulin)
                    enabledNames = enabledNames - name
                } else {
                    InsulinManager.enableProfile(insulin)
                    enabledNames = enabledNames + name
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = { toggle() },
                    modifier = Modifier.testTag("insulin_$name"),
                )
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.clickable { toggle() },
                )
            }
        }
        SettingsListRow(
            title = context.getString(R.string.pref_select_basal_insulinprofiles),
            entries = names,
            values = names,
            selectedValue = basalName,
            onSelected = { name ->
                byName[name]?.let { InsulinManager.setBasalProfile(it) }
                basalName = name
            },
            modifier = Modifier.testTag("insulin_basal"),
        )
        SettingsListRow(
            title = context.getString(R.string.pref_select_bolus_insulinprofiles),
            entries = names,
            values = names,
            selectedValue = bolusName,
            onSelected = { name ->
                byName[name]?.let { InsulinManager.setBolusProfile(it) }
                bolusName = name
            },
            modifier = Modifier.testTag("insulin_bolus"),
        )
        Row(modifier = Modifier.padding(16.dp)) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.testTag("insulin_cancel"),
            ) {
                Text(context.getString(R.string.cancel))
            }
            OutlinedButton(
                onClick = {
                    InsulinManager.LoadDisabledProfilesFromPrefs()
                    revision++
                },
                modifier = Modifier
                    .padding(start = 8.dp)
                    .testTag("insulin_undo"),
            ) {
                Text(context.getString(R.string.reset))
            }
            Button(
                onClick = {
                    InsulinManager.saveDisabledProfilesToPrefs()
                    onSave()
                },
                modifier = Modifier
                    .padding(start = 8.dp)
                    .testTag("insulin_save"),
            ) {
                Text(context.getString(R.string.save))
            }
        }
    }
}

// region Previews

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun InsulinProfileScreenPreview() {
    XdripPreview {
        InsulinProfileScreen(onSave = {}, onCancel = {})
    }
}

// endregion
