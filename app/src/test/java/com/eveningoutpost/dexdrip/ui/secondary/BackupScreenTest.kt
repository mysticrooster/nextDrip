package com.eveningoutpost.dexdrip.ui.secondary

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [BuildConfig.targetSDK], application = TestingApplication::class)
class BackupScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Composable
    private fun screen(
        status: String = "Ready",
        idle: Boolean = true,
        showAuto: Boolean = false,
        metaData: Map<String, String> = emptyMap(),
        automaticEnabled: Boolean = false,
        automaticMobile: Boolean = true,
        onSelectFile: () -> Unit = {},
        onBackupNow: () -> Unit = {},
        onRestoreNow: () -> Unit = {},
        onAutomaticEnabledChange: (Boolean) -> Unit = {},
        onAutomaticMobileChange: (Boolean) -> Unit = {},
    ) = BackupScreen(
        status = status,
        idle = idle,
        showAuto = showAuto,
        metaData = metaData,
        automaticEnabled = automaticEnabled,
        automaticMobile = automaticMobile,
        onSelectFile = onSelectFile,
        onBackupNow = onBackupNow,
        onRestoreNow = onRestoreNow,
        onAutomaticEnabledChange = onAutomaticEnabledChange,
        onAutomaticMobileChange = onAutomaticMobileChange,
        onBack = {},
    )

    @Test
    fun statusIsRendered() {
        composeRule.setContent { screen(status = "Reading file") }

        composeRule.onNodeWithTag("backup_status").assertTextContains("Reading file")
    }

    @Test
    fun metadataRowsOnlyShownWhenValuePresent() {
        composeRule.setContent {
            screen(
                metaData = mapOf(
                    "selectedLocation" to "xdrip-backup.bak",
                    "selectedLocationString" to "Selected location",
                    "lastDevice" to "",
                    "lastDeviceString" to "Backup made by",
                ),
            )
        }

        composeRule.onNodeWithTag("backup_row_selectedLocation").assertExists()
        composeRule.onNodeWithText("Selected location").assertExists()
        composeRule.onNodeWithText("xdrip-backup.bak").assertExists()
        composeRule.onNodeWithTag("backup_row_lastDevice").assertDoesNotExist()
    }

    @Test
    fun buttonsDispatchActions() {
        var select = false
        var backup = false
        var restore = false
        composeRule.setContent {
            screen(
                onSelectFile = { select = true },
                onBackupNow = { backup = true },
                onRestoreNow = { restore = true },
            )
        }

        composeRule.onNodeWithTag("backup_select").performScrollTo().performClick()
        composeRule.onNodeWithTag("backup_now").performScrollTo().performClick()
        composeRule.onNodeWithTag("backup_restore").performScrollTo().performClick()

        assertThat(select).isTrue()
        assertThat(backup).isTrue()
        assertThat(restore).isTrue()
    }

    @Test
    fun busyStateDisablesButtonsAndShowsProgress() {
        composeRule.setContent { screen(idle = false) }

        composeRule.onNodeWithTag("backup_progress").assertExists()
        composeRule.onNodeWithTag("backup_select").assertIsNotEnabled()
        composeRule.onNodeWithTag("backup_now").assertIsNotEnabled()
        composeRule.onNodeWithTag("backup_restore").assertIsNotEnabled()
    }

    @Test
    fun idleStateHidesProgressAndEnablesButtons() {
        composeRule.setContent { screen(idle = true) }

        composeRule.onNodeWithTag("backup_progress").assertDoesNotExist()
        composeRule.onNodeWithTag("backup_select").assertIsEnabled()
    }

    @Test
    fun automaticRowsHiddenUnlessSuitable() {
        composeRule.setContent { screen(showAuto = false) }

        composeRule.onNodeWithTag("backup_auto").assertDoesNotExist()
        composeRule.onNodeWithTag("backup_auto_mobile").assertDoesNotExist()
    }

    @Test
    fun automaticRowsShownAndMobileDependsOnMaster() {
        composeRule.setContent { screen(showAuto = true, automaticEnabled = false) }

        composeRule.onNodeWithTag("backup_auto").assertExists().assertIsEnabled()
        composeRule.onNodeWithTag("backup_auto_mobile").assertExists().assertIsNotEnabled()
    }

    @Test
    fun automaticRowsDispatch() {
        var automatic: Boolean? = null
        var mobile: Boolean? = null
        composeRule.setContent {
            screen(
                showAuto = true,
                automaticEnabled = true,
                onAutomaticEnabledChange = { automatic = it },
                onAutomaticMobileChange = { mobile = it },
            )
        }

        composeRule.onNodeWithTag("backup_auto").performScrollTo().performClick()
        composeRule.onNodeWithTag("backup_auto_mobile").performScrollTo().performClick()

        assertThat(automatic).isFalse()
        assertThat(mobile).isFalse()
    }
}
