package com.eveningoutpost.dexdrip.ui.secondary

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.eveningoutpost.dexdrip.BuildConfig
import com.eveningoutpost.dexdrip.SnoozeActivity
import com.eveningoutpost.dexdrip.TestingApplication
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [BuildConfig.targetSDK], application = TestingApplication::class)
class SnoozeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val labels = listOf("10 minutes", "15 minutes", "20 minutes", "30 minutes")

    private fun state(
        status: String = "No active alert exists",
        showSnooze: Boolean = false,
        showRemoteSnooze: Boolean = false,
        selectedIndex: Int = 0,
        showDisableAll: Boolean = true,
        showEnableAll: Boolean = false,
        showDisableLow: Boolean = true,
        showEnableLow: Boolean = false,
        showDisableHigh: Boolean = true,
        showEnableHigh: Boolean = false,
    ) = SnoozeState(
        status = status,
        showSnooze = showSnooze,
        showRemoteSnooze = showRemoteSnooze,
        snoozeLabels = labels,
        selectedIndex = selectedIndex,
        showDisableAll = showDisableAll,
        showEnableAll = showEnableAll,
        showDisableLow = showDisableLow,
        showEnableLow = showEnableLow,
        showDisableHigh = showDisableHigh,
        showEnableHigh = showEnableHigh,
    )

    @Test
    fun statusIsRendered() {
        composeRule.setContent { SnoozeScreen(state = state(status = "Active alert exists"), onSnooze = {}, onDisable = { _, _ -> }, onClear = {}, onRemoteSnooze = {}, onBack = {}) }

        composeRule.onNodeWithTag("snooze_status").assertTextEquals("Active alert exists")
    }

    @Test
    fun snoozeControlsHiddenWithoutActiveAlert() {
        composeRule.setContent { SnoozeScreen(state = state(showSnooze = false), onSnooze = {}, onDisable = { _, _ -> }, onClear = {}, onRemoteSnooze = {}, onBack = {}) }

        composeRule.onNodeWithTag("snooze_button").assertDoesNotExist()
        composeRule.onNodeWithTag("snooze_picker").assertDoesNotExist()
    }

    @Test
    fun snoozeDispatchesSelectedValue() {
        var minutes: Int? = null
        composeRule.setContent {
            SnoozeScreen(
                state = state(showSnooze = true, selectedIndex = SnoozeActivity.getSnoozeLocation(60)),
                onSnooze = { minutes = it },
                onDisable = { _, _ -> },
                onClear = {},
                onRemoteSnooze = {},
                onBack = {},
            )
        }

        composeRule.onNodeWithTag("snooze_button").performClick()

        assertThat(minutes).isEqualTo(60)
    }

    @Test
    fun disableAllOpensPickerAndDispatches() {
        var disabledType: SnoozeActivity.SnoozeType? = null
        var disabledMinutes: Long? = null
        composeRule.setContent {
            SnoozeScreen(
                state = state(),
                onSnooze = {},
                onDisable = { type, minutes -> disabledType = type; disabledMinutes = minutes },
                onClear = {},
                onRemoteSnooze = {},
                onBack = {},
            )
        }

        composeRule.onNodeWithTag("snooze_disable_all_alerts").performScrollTo().performClick()
        composeRule.onNodeWithText("Set").performClick()

        assertThat(disabledType).isEqualTo(SnoozeActivity.SnoozeType.ALL_ALERTS)
        assertThat(disabledMinutes).isEqualTo(60L)
    }

    @Test
    fun untilYouReenableDispatchesInfinite() {
        var disabledMinutes: Long? = null
        composeRule.setContent {
            SnoozeScreen(
                state = state(),
                onSnooze = {},
                onDisable = { _, minutes -> disabledMinutes = minutes },
                onClear = {},
                onRemoteSnooze = {},
                onBack = {},
            )
        }

        composeRule.onNodeWithTag("snooze_disable_low_alerts").performScrollTo().performClick()
        composeRule.onNodeWithTag("snooze_option_${labels.size}").performScrollTo().performClick()
        composeRule.onNodeWithText("Set").performClick()

        assertThat(disabledMinutes).isEqualTo(SnoozeActivity.infiniteSnoozeValueInMinutes)
    }

    @Test
    fun clearDispatchesType() {
        var cleared: SnoozeActivity.SnoozeType? = null
        composeRule.setContent {
            SnoozeScreen(
                state = state(showDisableLow = false, showEnableLow = true),
                onSnooze = {},
                onDisable = { _, _ -> },
                onClear = { cleared = it },
                onRemoteSnooze = {},
                onBack = {},
            )
        }

        composeRule.onNodeWithTag("snooze_enable_low_alerts").performScrollTo().performClick()

        assertThat(cleared).isEqualTo(SnoozeActivity.SnoozeType.LOW_ALERTS)
    }

    @Test
    fun remoteSnoozeVisibilityAndDispatch() {
        var remote = false
        composeRule.setContent {
            SnoozeScreen(
                state = state(showRemoteSnooze = true, showDisableAll = false, showDisableLow = false, showDisableHigh = false),
                onSnooze = {},
                onDisable = { _, _ -> },
                onClear = {},
                onRemoteSnooze = { remote = true },
                onBack = {},
            )
        }

        composeRule.onNodeWithTag("snooze_remote").performScrollTo().performClick()

        assertThat(remote).isTrue()
    }

    @Test
    fun allDisabledHidesPerTypeButtons() {
        composeRule.setContent {
            SnoozeScreen(
                state = state(showDisableAll = false, showEnableAll = true, showDisableLow = false, showEnableLow = false, showDisableHigh = false, showEnableHigh = false),
                onSnooze = {},
                onDisable = { _, _ -> },
                onClear = {},
                onRemoteSnooze = {},
                onBack = {},
            )
        }

        composeRule.onNodeWithTag("snooze_enable_all_alerts").assertExists()
        composeRule.onNodeWithTag("snooze_disable_low_alerts").assertDoesNotExist()
        composeRule.onNodeWithTag("snooze_enable_low_alerts").assertDoesNotExist()
        composeRule.onNodeWithTag("snooze_disable_high_alerts").assertDoesNotExist()
    }
}
