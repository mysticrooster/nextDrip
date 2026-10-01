package com.eveningoutpost.dexdrip.ui.secondary

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.eveningoutpost.dexdrip.BuildConfig
import com.eveningoutpost.dexdrip.TestingApplication
import com.eveningoutpost.dexdrip.utilitymodels.SendFeedBack
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [BuildConfig.targetSDK], application = TestingApplication::class)
class SendFeedBackScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Composable
    private fun screen(
        initialText: String = "",
        initialContact: String = "",
        initialType: String = "Bug Report",
        ratingVisible: Boolean = true,
        prepareSend: (String, String) -> SendFeedBack.FeedbackAction = { _, _ -> SendFeedBack.FeedbackAction.SUBMIT },
        onSubmit: (String, String, Float, String) -> Unit = { _, _, _, _ -> },
    ) = SendFeedBackScreen(
        initialText = initialText,
        initialContact = initialContact,
        initialType = initialType,
        ratingVisible = ratingVisible,
        prepareSend = prepareSend,
        onSubmit = onSubmit,
        onClose = {},
    )

    @Test
    fun ratingRowVisibilityFollowsFlag() {
        composeRule.setContent { screen(ratingVisible = true) }
        composeRule.onNodeWithTag("feedback_rating").assertExists()
    }

    @Test
    fun ratingRowHiddenForLogPush() {
        composeRule.setContent { screen(ratingVisible = false) }
        composeRule.onNodeWithTag("feedback_rating").assertDoesNotExist()
    }

    @Test
    fun submitDispatchesEnteredValues() {
        var submitted: String? = null
        composeRule.setContent {
            screen(onSubmit = { text, contact, _, type -> submitted = "$text|$contact|$type" })
        }

        composeRule.onNodeWithTag("feedback_text").performTextReplacement("hello")
        composeRule.onNodeWithTag("feedback_contact").performTextReplacement("a@b.c")
        composeRule.onNodeWithTag("feedback_send").performScrollTo().performClick()

        assertThat(submitted).isEqualTo("hello|a@b.c|Bug Report")
    }

    @Test
    fun ignoredSendDoesNothing() {
        var submitted = false
        composeRule.setContent {
            screen(prepareSend = { _, _ -> SendFeedBack.FeedbackAction.IGNORE }, onSubmit = { _, _, _, _ -> submitted = true })
        }

        composeRule.onNodeWithTag("feedback_send").performScrollTo().performClick()

        assertThat(submitted).isFalse()
    }

    @Test
    fun missingContactAsksForEmailAndSubmitsWithIt() {
        var submittedContact: String? = null
        composeRule.setContent {
            screen(
                prepareSend = { _, _ -> SendFeedBack.FeedbackAction.CONFIRM_EMAIL },
                onSubmit = { _, contact, _, _ -> submittedContact = contact },
            )
        }

        composeRule.onNodeWithTag("feedback_text").performTextReplacement("hello")
        composeRule.onNodeWithTag("feedback_send").performScrollTo().performClick()
        composeRule.onNodeWithText("Please supply email address or other contact reference").assertExists()
        composeRule.onNodeWithTag("feedback_email_input").performTextReplacement("x@y.z")
        composeRule.onNodeWithText("OK").performClick()

        assertThat(submittedContact).isEqualTo("x@y.z")
    }

    @Test
    fun unknownTypeAsksForTypeThenSubmitsWithIt() {
        var submittedType: String? = null
        composeRule.setContent {
            screen(
                initialType = "Unknown",
                onSubmit = { _, _, _, type -> submittedType = type },
            )
        }

        composeRule.onNodeWithText("Type of feedback?").assertExists()
        composeRule.onNodeWithTag("feedback_type_Bug Report").performClick()
        composeRule.onNodeWithTag("feedback_text").performTextReplacement("hello")
        composeRule.onNodeWithTag("feedback_contact").performTextReplacement("a@b.c")
        composeRule.onNodeWithTag("feedback_send").performScrollTo().performClick()

        assertThat(submittedType).isEqualTo("Bug Report")
    }

    @Test
    fun ratingCanBeSelected() {
        composeRule.setContent { screen() }

        composeRule.onNodeWithTag("feedback_star_3").performScrollTo().performClick()

        composeRule.onNodeWithTag("feedback_star_3").assertTextEquals("\u2605")
        composeRule.onNodeWithTag("feedback_star_4").assertTextEquals("\u2606")
    }
}
