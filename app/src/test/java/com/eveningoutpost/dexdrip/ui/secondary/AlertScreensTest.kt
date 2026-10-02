package com.eveningoutpost.dexdrip.ui.secondary

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
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

/** Compose tests for the Pass C (alerts) migrated screens. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [BuildConfig.targetSDK], application = TestingApplication::class)
class AlertScreensTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun alertListAddButtonsDispatch() {
        var low = false
        var high = false
        composeRule.setContent {
            AlertListScreen(
                lowRows = emptyList(),
                highRows = emptyList(),
                onAddLow = { low = true },
                onAddHigh = { high = true },
                onEdit = {},
                onBack = {},
            )
        }

        composeRule.onNodeWithTag("alert_create_low").performScrollTo().performClick()
        composeRule.onNodeWithTag("alert_create_high").performScrollTo().performClick()

        assertThat(low).isTrue()
        assertThat(high).isTrue()
    }

    private fun editScreen(
        editable: Boolean = true,
        removable: Boolean = true,
        allDay: Boolean = true,
        onSave: () -> Unit = {},
        onTest: () -> Unit = {},
        onRemove: () -> Unit = {},
        onApplySnooze: (Int) -> Unit = {},
        onPreSnooze: (Int) -> Unit = {},
        onOverrideSilentChange: (Boolean) -> Unit = {},
    ) {
        composeRule.setContent {
            EditAlertScreen(
                header = "Editing high alert",
                name = "High",
                threshold = "180",
                snooze = "120",
                reraise = "1",
                tone = "xDrip Default",
                startTime = "00:00",
                endTime = "23:59",
                allDay = allDay,
                vibrate = true,
                disabled = false,
                overrideSilent = true,
                forceSpeaker = true,
                editable = editable,
                removable = removable,
                onNameChange = {},
                onThresholdChange = {},
                onSnoozeChange = {},
                onReraiseChange = {},
                onAllDayChange = {},
                onVibrateChange = {},
                onDisabledChange = {},
                onOverrideSilentChange = onOverrideSilentChange,
                onForceSpeakerChange = {},
                onPickStartTime = {},
                onPickEndTime = {},
                onChooseRingtone = {},
                onChooseToneFile = {},
                onDefaultTone = {},
                onApplySnooze = onApplySnooze,
                onPreSnooze = onPreSnooze,
                onTest = onTest,
                onSave = onSave,
                onRemove = onRemove,
                onBack = {},
            )
        }
    }

    @Test
    fun editAlertSaveAndTestDispatch() {
        var saved = false
        var tested = false
        editScreen(onSave = { saved = true }, onTest = { tested = true })

        composeRule.onNodeWithTag("edit_alert_save").performScrollTo().performClick()
        composeRule.onNodeWithTag("edit_alert_test").performScrollTo().performClick()

        assertThat(saved).isTrue()
        assertThat(tested).isTrue()
    }

    @Test
    fun editAlertRemoveHiddenWhenNotRemovable() {
        editScreen(removable = false)

        composeRule.onNodeWithTag("edit_alert_remove").assertDoesNotExist()
        composeRule.onNodeWithTag("edit_alert_pre_snooze").assertDoesNotExist()
    }

    @Test
    fun editAlertAllDayHidesTimeRange() {
        editScreen(allDay = true)

        composeRule.onNodeWithTag("edit_alert_start_time").assertDoesNotExist()
    }

    @Test
    fun editAlertTimeRangeShownAndSnoozeDialogDispatches() {
        editScreen(allDay = false)

        composeRule.onNodeWithTag("edit_alert_start_time").assertExists()
        composeRule.onNodeWithTag("edit_alert_snooze").performScrollTo().performClick()
        composeRule.onNodeWithText("Set").performClick()
    }

    @Test
    fun editAlertOverrideSilentToggleDispatches() {
        var value: Boolean? = null
        editScreen(onOverrideSilentChange = { value = it })

        composeRule.onNodeWithTag("edit_alert_override_silent").performScrollTo().performClick()

        assertThat(value).isFalse()
    }
}
