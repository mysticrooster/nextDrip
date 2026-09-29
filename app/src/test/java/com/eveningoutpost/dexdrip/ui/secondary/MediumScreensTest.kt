package com.eveningoutpost.dexdrip.ui.secondary

import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.eveningoutpost.dexdrip.BuildConfig
import com.eveningoutpost.dexdrip.TestingApplication
import com.eveningoutpost.dexdrip.cgm.glupro.ViewModel
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Track V pass 3 (Medium) — small Data-Binding screens. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [BuildConfig.targetSDK], application = TestingApplication::class)
class MediumScreensTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun databaseAdminRendersConsoleAndDispatchesActions() {
        var quick = false
        var long = false
        var statistics = false
        var compact = false

        composeRule.setContent {
            DatabaseAdminScreen(
                console = "Ready",
                onQuickCheck = { quick = true },
                onLongCheck = { long = true },
                onStatistics = { statistics = true },
                onCompact = { compact = true },
                onBack = {},
            )
        }

        composeRule.onNodeWithTag("db_console").assertTextContains("Ready")
        composeRule.onNodeWithTag("db_quick").performClick()
        composeRule.onNodeWithTag("db_long").performClick()
        composeRule.onNodeWithTag("db_statistics").performClick()
        composeRule.onNodeWithTag("db_compact").performClick()

        assertThat(quick).isTrue()
        assertThat(long).isTrue()
        assertThat(statistics).isTrue()
        assertThat(compact).isTrue()
    }

    @Test
    fun mtpConfigureShowsProvidedStatus() {
        composeRule.setContent {
            MtpConfigureScreen(
                onBack = {},
                statusProvider = { "Not active" },
                colorProvider = { null },
                usbAttempt = {},
            )
        }

        composeRule.onNodeWithTag("mtp_status").assertTextContains("Not active")
    }

    @Test
    fun gluProRendersScanAgain() {
        composeRule.setContent {
            GluProScreen(viewModel = ViewModel(), onBack = {})
        }

        composeRule.onNodeWithTag("glupro_scan_again").assertExists()
    }
}
