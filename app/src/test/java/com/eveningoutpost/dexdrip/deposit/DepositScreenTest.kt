package com.eveningoutpost.dexdrip.deposit

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.eveningoutpost.dexdrip.BuildConfig
import com.eveningoutpost.dexdrip.TestingApplication
import com.eveningoutpost.dexdrip.utilitymodels.Pref
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [BuildConfig.targetSDK], application = TestingApplication::class)
class DepositScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun resetHiddenOutsideEngineeringMode() {
        Pref.setBoolean("engineering_mode", false)

        composeRule.setContent { DepositScreen(onBack = {}) }

        composeRule.onNodeWithTag("deposit_reset").assertDoesNotExist()
    }

    @Test
    fun resetConfirmationRunsReset() {
        Pref.setBoolean("engineering_mode", true)

        composeRule.setContent { DepositScreen(onBack = {}) }
        composeRule.onNodeWithTag("deposit_reset").performScrollTo().performClick()

        composeRule.onNodeWithText("Confirm Reset").assertExists()
        composeRule.onNodeWithText("Yes").performClick()

        composeRule.onNodeWithText("Reset data sequence!").assertExists()
    }

    @Test
    fun depositButtonsAndStatusRender() {
        composeRule.setContent { DepositScreen(onBack = {}) }

        composeRule.onNodeWithText("Ready").assertExists()
        composeRule.onNodeWithTag("deposit_g").assertExists()
        composeRule.onNodeWithTag("deposit_t").assertExists()
    }
}
