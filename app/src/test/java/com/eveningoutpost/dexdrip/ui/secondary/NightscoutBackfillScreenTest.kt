package com.eveningoutpost.dexdrip.ui.secondary

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.eveningoutpost.dexdrip.BuildConfig
import com.eveningoutpost.dexdrip.TestingApplication
import com.eveningoutpost.dexdrip.utilitymodels.Constants
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [BuildConfig.targetSDK], application = TestingApplication::class)
class NightscoutBackfillScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun guardBlocksWithinHourAndAllowsAfter() {
        BackfillGuard.clear()
        BackfillGuard.markRun(1_000L)

        assertThat(BackfillGuard.isLocked(2_000L)).isTrue()
        assertThat(BackfillGuard.isLocked(1_000L + Constants.HOUR_IN_MS - 1)).isTrue()
        assertThat(BackfillGuard.isLocked(1_000L + Constants.HOUR_IN_MS + 1)).isFalse()

        BackfillGuard.clear()
        assertThat(BackfillGuard.isLocked(1_000L)).isFalse()
    }

    @Test
    fun runPassesSelectedDate() {
        var runMillis = -1L

        composeRule.setContent { NightscoutBackfillScreen(onCancel = {}, onRun = { runMillis = it }) }
        composeRule.onNodeWithTag("backfill_run").performClick()

        assertThat(runMillis).isGreaterThan(0L)
    }

    @Test
    fun dateButtonOpensPicker() {
        composeRule.setContent { NightscoutBackfillScreen(onCancel = {}, onRun = {}) }

        composeRule.onNodeWithTag("backfill_date").performClick()

        composeRule.onNodeWithText("OK").assertExists()
    }
}
