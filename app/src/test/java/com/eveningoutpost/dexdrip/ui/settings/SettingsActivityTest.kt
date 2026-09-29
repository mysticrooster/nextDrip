package com.eveningoutpost.dexdrip.ui.settings

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
}
