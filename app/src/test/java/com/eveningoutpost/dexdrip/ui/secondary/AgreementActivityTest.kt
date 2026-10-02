package com.eveningoutpost.dexdrip.ui.secondary

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.eveningoutpost.dexdrip.Agreement
import com.eveningoutpost.dexdrip.BuildConfig
import com.eveningoutpost.dexdrip.TestingApplication
import com.eveningoutpost.dexdrip.utilitymodels.Pref
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [BuildConfig.targetSDK], application = TestingApplication::class)
class AgreementActivityTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<Agreement>()

    @Test
    fun legacyActionBarIsHiddenByHostScaffold() {
        assertThat(composeRule.activity.supportActionBar?.isShowing).isNotEqualTo(true)
    }

    @Test
    fun saveWritesPrefAndStartsHome() {
        Pref.setBoolean(Agreement.prefmarker, false)

        composeRule.onNodeWithTag("agreement_checkbox").performScrollTo().performClick()
        composeRule.onNodeWithTag("agreement_save").performScrollTo().performClick()

        assertThat(Pref.getBoolean(Agreement.prefmarker, false)).isTrue()
        assertThat(Shadows.shadowOf(composeRule.activity).nextStartedActivity?.component?.className)
            .isEqualTo("com.eveningoutpost.dexdrip.Home")
    }
}
