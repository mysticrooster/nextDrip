package com.eveningoutpost.dexdrip.ui.secondary

import androidx.compose.ui.test.junit4.createComposeRule
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
class TimePickerPrefScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun confirmWritesSecondsOfDayString() {
        Pref.setString("test_time_pref", "5400") // 01:30
        var done = false

        composeRule.setContent { TimePickerPrefScreen("test_time_pref") { done = true } }
        composeRule.onNodeWithText("OK").performClick()

        assertThat(Pref.getString("test_time_pref", "")).isEqualTo("5400")
        assertThat(done).isTrue()
    }

    @Test
    fun cancelLeavesPreferenceUntouched() {
        Pref.setString("test_time_pref", "5400")
        var done = false

        composeRule.setContent { TimePickerPrefScreen("test_time_pref") { done = true } }
        composeRule.onNodeWithText("Cancel").performClick()

        assertThat(Pref.getString("test_time_pref", "")).isEqualTo("5400")
        assertThat(done).isTrue()
    }
}
