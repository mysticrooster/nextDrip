package com.eveningoutpost.dexdrip.ui.settings

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.eveningoutpost.dexdrip.LicenseAgreementActivity
import com.eveningoutpost.dexdrip.R
import com.eveningoutpost.dexdrip.ui.theme.XdripTheme
import com.eveningoutpost.dexdrip.utilitymodels.Pref
import com.eveningoutpost.dexdrip.utils.Preferences

/**
 * Compose settings host.
 *
 * This is the first step of the settings migration (Phase 4): a self-contained Compose screen
 * that renders the migrated categories and links to the legacy [Preferences] activity for the
 * rest. Categories are ported onto [SettingsComponents] one at a time; the pref keys are
 * unchanged so user data/backups are unaffected.
 */
class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            XdripTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    SettingsRoot(
                        onOpenClassic = {
                            startActivity(Intent(this, Preferences::class.java))
                        },
                    )
                }
            }
        }
    }
}

private sealed interface SettingsScreen {
    data object Root : SettingsScreen
    data object Units : SettingsScreen
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsRoot(onOpenClassic: () -> Unit) {
    val stack = remember { mutableStateListOf<SettingsScreen>(SettingsScreen.Root) }
    BackHandler(enabled = stack.size > 1) { stack.removeAt(stack.lastIndex) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titleFor(stack.last()), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    if (stack.size > 1) {
                        TextButton(onClick = { stack.removeAt(stack.lastIndex) }) { Text("\u2039") }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            when (stack.last()) {
                SettingsScreen.Root -> RootScreen(
                    onOpenUnits = { stack.add(SettingsScreen.Units) },
                    onOpenClassic = onOpenClassic,
                )
                SettingsScreen.Units -> UnitsScreen()
            }
        }
    }
}

private fun titleFor(screen: SettingsScreen): String = when (screen) {
    SettingsScreen.Root -> "Settings"
    SettingsScreen.Units -> "Glucose Units"
}

@Composable
private fun RootScreen(onOpenUnits: () -> Unit, onOpenClassic: () -> Unit) {
    val context = LocalContext.current
    SettingsCategory(context.getString(R.string.general_settings)) {
        SettingsActionRow(
            title = context.getString(R.string.glucose_units),
            subtitle = context.getString(R.string.mmol_or_mgdl_high_and_low),
            onClick = onOpenUnits,
            modifier = Modifier.testTag("setting_glucose_units"),
        )
    }
    SettingsCategory("About") {
        SettingsActionRow(
            title = context.getString(R.string.end_user_license_agreement),
            subtitle = context.getString(R.string.not_for_medical_use),
            onClick = { context.startActivity(Intent(context, LicenseAgreementActivity::class.java)) },
            modifier = Modifier.testTag("setting_license"),
        )
        SettingsActionRow(
            title = "Classic settings",
            subtitle = "Screens not yet migrated to the new UI",
            onClick = onOpenClassic,
            modifier = Modifier.testTag("setting_classic"),
        )
    }
}

@Composable
private fun UnitsScreen() {
    val context = LocalContext.current
    var units by remember { mutableStateOf(Pref.getString("units", "mgdl")) }
    var high by remember { mutableStateOf(Pref.getString("highValue", "170")) }
    var low by remember { mutableStateOf(Pref.getString("lowValue", "70")) }

    SettingsCategory(context.getString(R.string.general_settings)) {
        SettingsListRow(
            title = context.getString(R.string.glucose_units),
            entries = listOf("mg/dL", "mmol/L"),
            values = listOf("mgdl", "mmol"),
            selectedValue = units,
            onSelected = {
                units = it
                Pref.setString("units", it)
            },
            modifier = Modifier.testTag("setting_units"),
        )
        SettingsEditTextRow(
            title = context.getString(R.string.high_value),
            subtitle = context.getString(R.string.maximum_value),
            value = high,
            numeric = true,
            onValueChange = {
                high = it
                Pref.setString("highValue", it)
            },
            modifier = Modifier.testTag("setting_highValue"),
        )
        SettingsEditTextRow(
            title = context.getString(R.string.low_value),
            subtitle = context.getString(R.string.minimum_value),
            value = low,
            numeric = true,
            onValueChange = {
                low = it
                Pref.setString("lowValue", it)
            },
            modifier = Modifier.testTag("setting_lowValue"),
        )
    }
}
