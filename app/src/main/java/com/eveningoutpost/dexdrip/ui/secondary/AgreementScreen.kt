@file:JvmName("AgreementScreen")

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.eveningoutpost.dexdrip.Agreement
import com.eveningoutpost.dexdrip.Home
import com.eveningoutpost.dexdrip.R
import com.eveningoutpost.dexdrip.ui.theme.XdripPreview
import com.eveningoutpost.dexdrip.utilitymodels.Pref

/**
 * Track V pass 2 — `Agreement`: the first-run important warning gate
 * (`warning_agreed_to`). Saving with the box ticked continues to [Home]. Java entry point:
 * [installAgreement].
 */
fun installAgreement(activity: ComponentActivity) {
    activity.setContent {
        AgreementScreen(
            onBack = { activity.finish() },
            onAgree = { agreed ->
                Pref.setBoolean(Agreement.prefmarker, agreed)
                if (agreed) {
                    activity.startActivity(Intent(activity, Home::class.java))
                    activity.finish()
                }
            },
        )
    }
}

@Composable
internal fun AgreementScreen(
    onBack: () -> Unit,
    onAgree: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    var agreed by remember { mutableStateOf(Pref.getBoolean(Agreement.prefmarker, false)) }

    SecondaryScreen(title = context.getString(R.string.important_warning), onBack = onBack) {
        Text(
            text = context.getString(R.string.important_warning),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        )
        Text(
            text = context.getString(R.string.importanttext),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(vertical = 16.dp),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { agreed = !agreed }
                .padding(vertical = 8.dp),
        ) {
            Checkbox(
                checked = agreed,
                onCheckedChange = { agreed = it },
                modifier = Modifier.testTag("agreement_checkbox"),
            )
            Text(context.getString(R.string.pref_I_understand_title))
        }
        Button(
            onClick = { onAgree(agreed) },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 20.dp)
                .testTag("agreement_save"),
        ) {
            Text(context.getString(R.string.i_agree))
        }
    }
}

// region Previews

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun AgreementScreenPreview() {
    XdripPreview {
        AgreementScreen(onBack = {}, onAgree = {})
    }
}

// endregion
