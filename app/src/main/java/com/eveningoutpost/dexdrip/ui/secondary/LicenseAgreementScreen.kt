@file:JvmName("LicenseAgreementScreen")

package com.eveningoutpost.dexdrip.ui.secondary

import android.content.Intent
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.eveningoutpost.dexdrip.Agreement
import com.eveningoutpost.dexdrip.Home
import com.eveningoutpost.dexdrip.R
import com.eveningoutpost.dexdrip.ui.theme.XdripPreview
import com.eveningoutpost.dexdrip.utilitymodels.Pref
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity

/**
 * Track V — `LicenseAgreementActivity`: Compose EULA gate. Saving writes `I_understand` and starts
 * [Home] (preserving the startup gate in `Home.checkEula`). Java entry point: [install].
 */
fun installLicenseAgreement(activity: ComponentActivity) {
    activity.setContent {
        LicenseAgreementScreen(
            onBack = { activity.finish() },
            onSave = { agreed ->
                Pref.setBoolean("I_understand", agreed)
                activity.startActivity(Intent(activity, Home::class.java))
                activity.finish()
            },
            onGoogleLicenses = { activity.startActivity(Intent(activity, OssLicensesMenuActivity::class.java)) },
            onWarning = { activity.startActivity(Intent(activity, Agreement::class.java)) },
        )
    }
}

@Composable
internal fun LicenseAgreementScreen(
    onBack: () -> Unit,
    onSave: (Boolean) -> Unit,
    onGoogleLicenses: () -> Unit,
    onWarning: () -> Unit,
) {
    val context = LocalContext.current
    var agreed by remember { mutableStateOf(Pref.getBoolean("I_understand", false)) }

    SecondaryScreen(title = context.getString(R.string.end_user_license_agreement), onBack = onBack) {
        Text(
            text = context.getString(R.string.pref_I_understand_summery),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(16.dp),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { agreed = !agreed }
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Checkbox(
                checked = agreed,
                onCheckedChange = { agreed = it },
                modifier = Modifier.testTag("license_checkbox"),
            )
            Text(context.getString(R.string.pref_I_understand_title))
        }
        Button(
            onClick = { onSave(agreed) },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 20.dp)
                .testTag("license_save"),
        ) {
            Text(context.getString(R.string.save))
        }
        OutlinedButton(
            onClick = onWarning,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 20.dp),
        ) {
            Text(context.getString(R.string.view_important_warning))
        }
        TextButton(
            onClick = onGoogleLicenses,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .alpha(0.5f),
        ) {
            Text(context.getString(R.string.view_google_licenses))
        }
    }
}

// region Previews

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun LicenseAgreementScreenPreview() {
    XdripPreview {
        LicenseAgreementScreen(onBack = {}, onSave = {}, onGoogleLicenses = {}, onWarning = {})
    }
}

// endregion
