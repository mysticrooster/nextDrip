package com.eveningoutpost.dexdrip.ui.secondary

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.eveningoutpost.dexdrip.BuildConfig
import com.eveningoutpost.dexdrip.TestingApplication
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [BuildConfig.targetSDK], application = TestingApplication::class)
class DisplayQRCodeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Composable
    private fun screen(
        showQr: Boolean = false,
        qrBitmap: Bitmap? = null,
        showGkey: Boolean = false,
        desertSync: Boolean = false,
        narrative: String = "Hello World",
        onSyncSettings: () -> Unit = {},
        onDesertSync: () -> Unit = {},
        onGkey: () -> Unit = {},
        onConnectionSettings: () -> Unit = {},
        onAllSettings: () -> Unit = {},
    ) = DisplayQRCodeScreen(
        showQr = showQr,
        qrBitmap = qrBitmap,
        showGkey = showGkey,
        desertSync = desertSync,
        narrative = narrative,
        onSyncSettings = onSyncSettings,
        onDesertSync = onDesertSync,
        onGkey = onGkey,
        onConnectionSettings = onConnectionSettings,
        onAllSettings = onAllSettings,
        onClose = {},
    )

    @Test
    fun buttonsDispatchWhenNoQrShown() {
        var sync = false
        var connection = false
        var all = false
        composeRule.setContent {
            screen(
                onSyncSettings = { sync = true },
                onConnectionSettings = { connection = true },
                onAllSettings = { all = true },
            )
        }

        composeRule.onNodeWithTag("qr_sync").performScrollTo().performClick()
        composeRule.onNodeWithTag("qr_connection").performScrollTo().performClick()
        composeRule.onNodeWithTag("qr_all").performScrollTo().performClick()

        assertThat(sync).isTrue()
        assertThat(connection).isTrue()
        assertThat(all).isTrue()
    }

    @Test
    fun desertAndGkeyButtonsGatedOnState() {
        composeRule.setContent { screen(showGkey = false, desertSync = false) }

        composeRule.onNodeWithTag("qr_desert").assertDoesNotExist()
        composeRule.onNodeWithTag("qr_gkey").assertDoesNotExist()
    }

    @Test
    fun desertAndGkeyButtonsShownAndDispatch() {
        var desert = false
        var gkey = false
        composeRule.setContent {
            screen(showGkey = true, desertSync = true, onDesertSync = { desert = true }, onGkey = { gkey = true })
        }

        composeRule.onNodeWithTag("qr_desert").performScrollTo().performClick()
        composeRule.onNodeWithTag("qr_gkey").performScrollTo().performClick()

        assertThat(desert).isTrue()
        assertThat(gkey).isTrue()
    }

    @Test
    fun qrAndNarrativeShownAndSourcesHidden() {
        val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        composeRule.setContent { screen(showQr = true, qrBitmap = bitmap, narrative = "Sat 12:00\nGoogle Pixel\nNightscout configuration") }

        composeRule.onNodeWithTag("qr_image").assertExists()
        composeRule.onNodeWithTag("qr_narrative").assertExists()
        composeRule.onNodeWithText("Sat 12:00\nGoogle Pixel\nNightscout configuration").assertExists()
        composeRule.onNodeWithTag("qr_sync").assertDoesNotExist()
    }
}
