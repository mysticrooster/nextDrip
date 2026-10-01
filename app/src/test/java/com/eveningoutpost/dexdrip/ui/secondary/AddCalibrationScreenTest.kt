package com.eveningoutpost.dexdrip.ui.secondary

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
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
class AddCalibrationScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun blankValueShowsError() {
        composeRule.setContent { AddCalibrationScreen(onDone = { "Calibration Can Not be Blank" }, onBack = {}) }

        composeRule.onNodeWithTag("addcal_done").performClick()

        composeRule.onNodeWithText("Calibration Can Not be Blank").assertExists()
    }

    @Test
    fun enteredValueIsPassedToActivity() {
        var submitted: String? = null
        composeRule.setContent { AddCalibrationScreen(onDone = { submitted = it; null }, onBack = {}) }

        composeRule.onNodeWithTag("addcal_value").performTextInput("123")
        composeRule.onNodeWithTag("addcal_done").performClick()

        assertThat(submitted).isEqualTo("123")
    }

    @Test
    fun errorClearsOnTyping() {
        composeRule.setContent { AddCalibrationScreen(onDone = { "Calibration Can Not be Blank" }, onBack = {}) }

        composeRule.onNodeWithTag("addcal_done").performClick()
        composeRule.onNodeWithText("Calibration Can Not be Blank").assertExists()

        composeRule.onNodeWithTag("addcal_value").performTextReplacement("120")
        composeRule.onNodeWithText("Calibration Can Not be Blank").assertDoesNotExist()
    }
}
