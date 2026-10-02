package com.eveningoutpost.dexdrip.ui.secondary

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.eveningoutpost.dexdrip.BuildConfig
import com.eveningoutpost.dexdrip.TestingApplication
import com.eveningoutpost.dexdrip.models.UserError
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Compose tests for the Pass B (logs, tables, keypad) migrated screens. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [BuildConfig.targetSDK], application = TestingApplication::class)
class LogScreensTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun errorsSeverityAndUploadDispatch() {
        var severity: Pair<Int, Boolean>? = null
        var uploaded = false
        composeRule.setContent {
            ErrorsScreen(
                severityEnabled = mapOf(1 to false, 2 to true, 3 to true, 5 to true, 6 to true),
                autoRefresh = false,
                errors = emptyList(),
                onSeverityChange = { s, v -> severity = s to v },
                onAutoRefreshChange = {},
                onUpload = { uploaded = true },
                onBack = {},
            )
        }

        composeRule.onNodeWithTag("errors_severity_1").performClick()
        composeRule.onNodeWithTag("errors_upload").performClick()

        assertThat(severity).isEqualTo(1 to true)
        assertThat(uploaded).isTrue()
    }

    @Test
    fun errorsRowsRenderMessage() {
        composeRule.setContent {
            ErrorsScreen(
                severityEnabled = mapOf(1 to true, 2 to true, 3 to true, 5 to true, 6 to true),
                autoRefresh = false,
                errors = listOf(UserError(3, "Short", "Long message")),
                onSeverityChange = { _, _ -> },
                onAutoRefreshChange = {},
                onUpload = {},
                onBack = {},
            )
        }

        composeRule.onNodeWithText("Long message").assertExists()
    }

    @Test
    fun eventLogSearchAndActionsDispatch() {
        var query = ""
        var uploaded = false
        var saved = false
        composeRule.setContent {
            EventLogScreen(
                query = "",
                severities = mapOf(1 to true, 2 to true, 3 to true, 5 to true, 6 to true),
                loading = false,
                visible = emptyList(),
                showScrollToTop = true,
                showThisTitle = { true },
                listState = rememberLazyListState(),
                onQueryChange = { query = it },
                onSeverityChange = { _, _ -> },
                onTitleLongClick = {},
                onUpload = { uploaded = true },
                onSave = { saved = true },
                onTop = {},
                onBack = {},
            )
        }

        composeRule.onNodeWithTag("event_log_search").performTextInput("abc")
        composeRule.onNodeWithTag("event_log_upload").performClick()
        composeRule.onNodeWithTag("event_log_save").performClick()

        assertThat(query).isEqualTo("abc")
        assertThat(uploaded).isTrue()
        assertThat(saved).isTrue()
    }

    @Test
    fun phoneKeypadTabsAndSubmitDispatch() {
        var tab: String? = null
        var submitted = false
        composeRule.setContent {
            PhoneKeypadScreen(
                currentTab = "insulin-1",
                text = "4.5",
                suffix = " units",
                multipleInsulins = false,
                insulinProfileNames = listOf("Novorapid", null, null),
                activeInsulinProfile = 1,
                hasValue = true,
                invalidTime = { false },
                onTabSelected = { tab = it },
                onInsulinProfileSelected = {},
                onTextChange = {},
                onClear = {},
                onSubmit = { submitted = true },
                onSpeech = {},
                onTextRecognition = {},
                onBack = {},
            )
        }

        composeRule.onNodeWithTag("keypad_tab_carbs").performScrollTo().performClick()
        composeRule.onNodeWithTag("keypad_submit").performScrollTo().performClick()

        assertThat(tab).isEqualTo("carbs")
        assertThat(submitted).isTrue()
    }

    @Test
    fun phoneKeypadInvalidTimeShowsDialogAndDoesNotSubmit() {
        var submitted = false
        composeRule.setContent {
            PhoneKeypadScreen(
                currentTab = "time",
                text = "99.99",
                suffix = " when",
                multipleInsulins = false,
                insulinProfileNames = listOf(null, null, null),
                activeInsulinProfile = 1,
                hasValue = true,
                invalidTime = { true },
                onTabSelected = {},
                onInsulinProfileSelected = {},
                onTextChange = {},
                onClear = {},
                onSubmit = { submitted = true },
                onSpeech = {},
                onTextRecognition = {},
                onBack = {},
            )
        }

        composeRule.onNodeWithTag("keypad_submit").performScrollTo().performClick()

        composeRule.onNodeWithText("Invalid time").assertExists()
        assertThat(submitted).isFalse()
    }

    @Test
    fun phoneKeypadSubmitDisabledWithoutValue() {
        composeRule.setContent {
            PhoneKeypadScreen(
                currentTab = "insulin-1",
                text = "",
                suffix = " units",
                multipleInsulins = false,
                insulinProfileNames = listOf(null, null, null),
                activeInsulinProfile = 1,
                hasValue = false,
                invalidTime = { false },
                onTabSelected = {},
                onInsulinProfileSelected = {},
                onTextChange = {},
                onClear = {},
                onSubmit = {},
                onSpeech = {},
                onTextRecognition = {},
                onBack = {},
            )
        }

        composeRule.onNodeWithTag("keypad_submit").assertIsNotEnabled()
    }
}
