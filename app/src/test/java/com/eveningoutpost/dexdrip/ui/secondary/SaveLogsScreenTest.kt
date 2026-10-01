package com.eveningoutpost.dexdrip.ui.secondary

import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
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
class SaveLogsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun shortLogDataIsShownVerbatim() {
        composeRule.setContent { SaveLogsScreen(logData = "short log", onSave = {}, onClose = {}) }

        composeRule.onNodeWithTag("savelogs_text").assertTextEquals("short log")
    }

    @Test
    fun longLogDataIsHiddenButSized() {
        val data = "x".repeat(400)
        composeRule.setContent { SaveLogsScreen(logData = data, onSave = {}, onClose = {}) }

        composeRule.onNodeWithTag("savelogs_text")
            .assertTextContains("Attached 400 characters of log data. (hidden)", substring = true)
    }

    @Test
    fun saveAndCloseDispatch() {
        var saved = false
        var closed = false
        composeRule.setContent { SaveLogsScreen(logData = "log", onSave = { saved = true }, onClose = { closed = true }) }

        composeRule.onNodeWithTag("savelogs_save").performScrollTo().performClick()
        composeRule.onNodeWithTag("savelogs_close").performScrollTo().performClick()

        assertThat(saved).isTrue()
        assertThat(closed).isTrue()
    }

    @Test
    fun displayTextMatchesLegacyThreshold() {
        assertThat(displayText("a".repeat(300))).isEqualTo("a".repeat(300))
        assertThat(displayText("a".repeat(301))).contains("Attached 301 characters of log data. (hidden)")
    }
}
