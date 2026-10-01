package com.eveningoutpost.dexdrip.ui.secondary

import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import com.eveningoutpost.dexdrip.BuildConfig
import com.eveningoutpost.dexdrip.TestingApplication
import com.eveningoutpost.dexdrip.eassist.EmergencyContact
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Replaces the removed view-based test coverage of `EmergencyAssistActivity`: the Compose screen
 * renders the bridged preference state and dispatches every user action through the activity
 * (contact picker/permissions stay in Java).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [BuildConfig.targetSDK], application = TestingApplication::class)
class EmergencyAssistScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun reason(
        tag: String,
        title: String = tag,
        checked: Boolean = false,
        minutes: Int = 0,
        minutesText: String = "",
        maxMinutes: Int = 360,
        onCheckedChange: (Boolean) -> Unit = {},
        onMinutesChange: (Int) -> Unit = {},
    ) = AssistReasonState(
        tag = tag,
        title = title,
        checked = checked,
        minutes = minutes,
        minutesText = minutesText,
        maxMinutes = maxMinutes,
        onCheckedChange = onCheckedChange,
        onMinutesChange = onMinutesChange,
    )

    @Composable
    private fun screen(
        enabled: Boolean = false,
        onEnabledChange: (Boolean) -> Unit = {},
        username: String = "",
        onUsernameChange: (String) -> Unit = {},
        previewText: String = "Preview Text",
        contacts: List<EmergencyContact> = emptyList(),
        onAddContact: () -> Unit = {},
        onRemoveContact: (EmergencyContact) -> Unit = {},
        reasons: List<AssistReasonState> = emptyList(),
        onTest: () -> Unit = {},
    ) = EmergencyAssistScreen(
        enabled = enabled,
        onEnabledChange = onEnabledChange,
        username = username,
        onUsernameChange = onUsernameChange,
        previewText = previewText,
        contacts = contacts,
        onAddContact = onAddContact,
        onRemoveContact = onRemoveContact,
        reasons = reasons,
        onTest = onTest,
        onBack = {},
    )

    @Test
    fun masterSwitchDispatchesToggle() {
        var changed: Boolean? = null
        composeRule.setContent { screen(enabled = true, onEnabledChange = { changed = it }) }

        composeRule.onNodeWithTag("ea_enable").assertIsOn().performClick()

        assertThat(changed).isFalse()
    }

    @Test
    fun reasonSwitchDispatchesToggle() {
        var changed: Boolean? = null
        composeRule.setContent {
            screen(reasons = listOf(reason("low", onCheckedChange = { changed = it })))
        }

        composeRule.onNodeWithTag("ea_low_switch").assertIsOff().performScrollTo().performClick()

        assertThat(changed).isTrue()
    }

    @Test
    fun thresholdSliderHiddenUntilReasonEnabled() {
        composeRule.setContent {
            screen(reasons = listOf(reason("low", checked = false), reason("high", checked = true)))
        }

        composeRule.onNodeWithTag("ea_low_slider").assertDoesNotExist()
        composeRule.onNodeWithTag("ea_high_slider").assertExists()
    }

    @Test
    fun thresholdSliderDispatchesMinutes() {
        var minutes: Int? = null
        composeRule.setContent {
            screen(
                reasons = listOf(
                    reason("low", checked = true, minutes = 60, minutesText = "1h", onMinutesChange = { minutes = it }),
                ),
            )
        }

        composeRule.onNodeWithTag("ea_low_slider")
            .performScrollTo()
            .performSemanticsAction(SemanticsActions.SetProgress) { it(250f) }

        assertThat(minutes).isEqualTo(250)
    }

    @Test
    fun contactsAndCountAreRendered() {
        val alice = EmergencyContact("Alice", "0123456789")
        composeRule.setContent { screen(contacts = listOf(alice)) }

        composeRule.onNodeWithTag("ea_contact_Alice").assertExists()
        composeRule.onNodeWithText("Alice").assertExists()
        composeRule.onNodeWithText("0123456789").assertExists()
        composeRule.onNodeWithTag("ea_contacts_count")
            .assertTextContains("1 selected contacts for text messages")
    }

    @Test
    fun deleteAsksForConfirmationAndCancelKeepsContact() {
        var removed: EmergencyContact? = null
        val alice = EmergencyContact("Alice", "0123456789")
        composeRule.setContent { screen(contacts = listOf(alice), onRemoveContact = { removed = it }) }

        composeRule.onNodeWithTag("ea_contact_delete_Alice").performScrollTo().performClick()
        composeRule.onNodeWithText("Remove?").assertExists()
        composeRule.onNodeWithText("Remove Alice from emergency text message receivers list?").assertExists()
        composeRule.onNodeWithText("No").performClick()

        assertThat(removed).isNull()
        composeRule.onNodeWithTag("ea_contact_Alice").assertExists()
    }

    @Test
    fun deleteConfirmsAndDispatchesContact() {
        var removed: EmergencyContact? = null
        val alice = EmergencyContact("Alice", "0123456789")
        composeRule.setContent { screen(contacts = listOf(alice), onRemoveContact = { removed = it }) }

        composeRule.onNodeWithTag("ea_contact_delete_Alice").performScrollTo().performClick()
        composeRule.onNodeWithText("Yes").performClick()

        assertThat(removed?.name).isEqualTo("Alice")
    }

    @Test
    fun addContactDispatches() {
        var added = false
        composeRule.setContent { screen(onAddContact = { added = true }) }

        composeRule.onNodeWithTag("ea_add_contact").performScrollTo().performClick()

        assertThat(added).isTrue()
    }

    @Test
    fun testButtonHiddenWithoutContacts() {
        composeRule.setContent { screen(onTest = {}) }

        composeRule.onNodeWithTag("ea_test").assertDoesNotExist()
    }

    @Test
    fun testButtonDispatchesWithContacts() {
        var tested = false
        composeRule.setContent {
            screen(contacts = listOf(EmergencyContact("Alice", "0123456789")), onTest = { tested = true })
        }

        composeRule.onNodeWithTag("ea_test").performScrollTo().performClick()

        assertThat(tested).isTrue()
    }

    @Test
    fun usernameEditsAndPreviewAreShown() {
        var username: String? = null
        composeRule.setContent {
            screen(previewText = "Help me! Near home", onUsernameChange = { username = it })
        }

        composeRule.onNodeWithTag("ea_preview").assertTextContains("Help me! Near home")
        composeRule.onNodeWithTag("ea_username").performScrollTo().performTextReplacement("Bob")

        assertThat(username).isEqualTo("Bob")
    }

    @Test
    fun snapDefaultsMatchLegacyPrefsViewStringSnapDefaults() {
        assertThat(snapMinutesValue("emergency_assist_low_alert_minutes", "")).isEqualTo("60")
        assertThat(snapMinutesValue("emergency_assist_low_alert_minutes", "0")).isEqualTo("60")
        assertThat(snapMinutesValue("emergency_assist_high_alert_minutes", "")).isEqualTo("240")
        assertThat(snapMinutesValue("emergency_assist_inactivity_minutes", "0")).isEqualTo("1440")
        assertThat(snapMinutesValue("emergency_assist_lowest_alert_minutes", "")).isEqualTo("")
        assertThat(snapMinutesValue("emergency_assist_lowest_alert_minutes", "0")).isEqualTo("0")
        assertThat(snapMinutesValue("emergency_assist_low_alert_minutes", "5")).isEqualTo("5")
    }
}
