package com.eveningoutpost.dexdrip.ui.secondary

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.eveningoutpost.dexdrip.BuildConfig
import com.eveningoutpost.dexdrip.LicenseAgreementActivity
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
class LicenseAgreementScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<LicenseAgreementActivity>()

    @Test
    fun saveWritesUnderstandAndStartsHome() {
        Pref.setBoolean("I_understand", false)

        composeRule.onNodeWithTag("license_checkbox").performClick()
        composeRule.onNodeWithTag("license_save").performClick()

        assertThat(Pref.getBoolean("I_understand", false)).isTrue()
        assertThat(Shadows.shadowOf(composeRule.activity).nextStartedActivity?.component?.className)
            .isEqualTo("com.eveningoutpost.dexdrip.Home")
    }
}
