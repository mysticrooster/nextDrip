@file:JvmName("HealthPrivacyScreen")

package com.eveningoutpost.dexdrip.ui.secondary

import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.eveningoutpost.dexdrip.R
import com.eveningoutpost.dexdrip.ui.theme.XdripPreview

/**
 * Track V pass 2 — `HealthPrivacy`: static Health Connect privacy explanation.
 * Java entry point: [installHealthPrivacy].
 */
fun installHealthPrivacy(activity: ComponentActivity) {
    activity.setContent {
        HealthPrivacyScreen(onClose = { activity.finish() })
    }
}

@Composable
internal fun HealthPrivacyScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    SecondaryScreen(title = context.getString(R.string.title_health_privacy), onBack = onClose) {
        Text(
            text = context.getString(R.string.health_privacy_text),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth().padding(15.dp),
        )
        Button(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(5.dp)
                .testTag("health_privacy_close"),
        ) {
            Text(context.getString(R.string.close))
        }
    }
}

// region Previews

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun HealthPrivacyScreenPreview() {
    XdripPreview {
        HealthPrivacyScreen(onClose = {})
    }
}

// endregion
