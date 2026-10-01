package com.eveningoutpost.dexdrip.ui.secondary

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.eveningoutpost.dexdrip.BuildConfig
import com.eveningoutpost.dexdrip.TestingApplication
import com.eveningoutpost.dexdrip.cloud.backup.BackupActivity
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlertDialog

/** Wiring test for the Compose `BackupScreen` hosted in the real activity. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [BuildConfig.targetSDK], application = TestingApplication::class)
class BackupActivityTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<BackupActivity>()

    @Test
    fun initialStateRendersViewModelStatus() {
        composeRule.onNodeWithTag("backup_status").assertExists()
        composeRule.onNodeWithTag("backup_select").assertIsEnabled()
    }

    @Test
    fun statusFieldChangesAreBridgedToCompose() {
        composeRule.runOnIdle { composeRule.activity.viewModel.status.set("Bridged status") }

        composeRule.onNodeWithTag("backup_status").assertTextContains("Bridged status")
    }

    @Test
    fun selectFileButtonOpensPickerDialog() {
        composeRule.onNodeWithTag("backup_select").performClick()

        assertThat(ShadowAlertDialog.getLatestAlertDialog()).isNotNull()
    }
}
