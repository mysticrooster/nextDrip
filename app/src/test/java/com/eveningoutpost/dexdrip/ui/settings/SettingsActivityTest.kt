package com.eveningoutpost.dexdrip.ui.settings

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.eveningoutpost.dexdrip.BuildConfig
import com.eveningoutpost.dexdrip.TestingApplication
import com.eveningoutpost.dexdrip.utilitymodels.Pref
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [BuildConfig.targetSDK], application = TestingApplication::class)
class SettingsActivityTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<SettingsActivity>()

    @Test
    fun unitsSubScreenWritesPref() {
        Pref.setString("units", "mgdl")

        // Root -> units sub-screen -> units list dialog.
        composeRule.onNodeWithTag("setting_glucose_units").performClick()
        composeRule.onNodeWithTag("setting_units").performClick()
        composeRule.onNodeWithText("mmol/L").performClick()

        assertThat(Pref.getString("units", "mgdl")).isEqualTo("mmol")
    }

    @Test
    fun editTextRowOpensDialog() {
        composeRule.onNodeWithTag("setting_glucose_units").performClick()
        composeRule.onNodeWithTag("setting_highValue").performClick()

        // The edit dialog (with its OK button) is shown.
        composeRule.onNodeWithText("OK").assertExists()
    }

    @Test
    fun notificationSwitchWritesPref() {
        Pref.setBoolean("smart_snoozing", true)

        composeRule.onNodeWithTag("setting_notifications").performClick()
        composeRule.onNodeWithText("Glucose Alerts Settings").performClick()
        composeRule.onNodeWithText("Smart Snoozing").performClick()

        assertThat(Pref.getBoolean("smart_snoozing", true)).isFalse()
    }

    @Test
    fun dependentRowDisabledUntilMasterEnabled() {
        Pref.setBoolean("disable_alerts_stale_data", false)

        composeRule.onNodeWithTag("setting_notifications").performClick()
        composeRule.onNodeWithText("Glucose Alerts Settings").performClick()
        composeRule.onNodeWithText("Suppress Alerts if missed readings").performScrollTo().performClick()

        // Master off -> dependent numeric row does not react.
        composeRule.onNodeWithTag("setting_stale_minutes").performClick()
        composeRule.onNodeWithText("OK").assertDoesNotExist()

        // Master on -> dependent row opens its dialog.
        composeRule.onNodeWithTag("setting_stale_enabled").performClick()
        composeRule.onNodeWithTag("setting_stale_minutes").performClick()
        composeRule.onNodeWithText("OK").assertExists()
    }

    @Test
    fun searchNavigatesToMigratedSetting() {
        composeRule.onNodeWithTag("setting_search").performTextInput("calibration")
        composeRule.onNodeWithText("Calibration Alerts").performClick()

        composeRule.onNodeWithTag("setting_calibration_notifications").assertExists()
    }
}
