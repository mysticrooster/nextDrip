package com.eveningoutpost.dexdrip.ui.secondary

import androidx.compose.ui.test.assertTextEquals
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

/** Compose tests for the Pass A (Data & admin) migrated screens. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [BuildConfig.targetSDK], application = TestingApplication::class)
class AdminScreensTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun importDbWarningOkDispatches() {
        var ok = false
        composeRule.setContent {
            ImportDbScreen(
                databaseNames = emptyList(),
                showWarning = true,
                resultMessage = null,
                databaseNameAt = { "" },
                onWarningOk = { ok = true },
                onImport = {},
                onResultOk = {},
                onBack = {},
            )
        }

        composeRule.onNodeWithTag("import_db_warning_ok").performScrollTo().performClick()

        assertThat(ok).isTrue()
    }

    @Test
    fun importDbItemDispatchesPositionAfterConfirm() {
        var imported: Int? = null
        composeRule.setContent {
            ImportDbScreen(
                databaseNames = listOf("a.sqlite", "b.sqlite"),
                showWarning = false,
                resultMessage = null,
                databaseNameAt = { "a.sqlite" },
                onWarningOk = {},
                onImport = { imported = it },
                onResultOk = {},
                onBack = {},
            )
        }

        composeRule.onNodeWithTag("import_db_item_1").performScrollTo().performClick()
        composeRule.onNodeWithText("OK").performClick()

        assertThat(imported).isEqualTo(1)
    }

    @Test
    fun importDbResultDialogDispatchesReturnHome() {
        var returned = false
        composeRule.setContent {
            ImportDbScreen(
                databaseNames = emptyList(),
                showWarning = false,
                resultMessage = "Done",
                databaseNameAt = { "" },
                onWarningOk = {},
                onImport = {},
                onResultOk = { returned = true },
                onBack = {},
            )
        }

        composeRule.onNodeWithText("Import Result").assertExists()
        composeRule.onNodeWithText("OK").performClick()

        assertThat(returned).isTrue()
    }

    @Test
    fun sdcardButtonsDispatch() {
        var saved = false
        var loaded = false
        var deleted = false
        composeRule.setContent {
            SdcardImportExportScreen(
                onSave = { saved = true },
                onLoad = { loaded = true },
                onDelete = { deleted = true },
                onBack = {},
            )
        }

        composeRule.onNodeWithTag("sdcard_save").performScrollTo().performClick()
        composeRule.onNodeWithTag("sdcard_load").performScrollTo().performClick()
        composeRule.onNodeWithTag("sdcard_delete").performScrollTo().performClick()

        assertThat(saved).isTrue()
        assertThat(loaded).isTrue()
        assertThat(deleted).isTrue()
    }

    @Test
    fun updateTogglesDispatchAndProgressHidden() {
        var auto = true
        var internal = true
        composeRule.setContent {
            UpdateScreen(
                channel = "Update channel: Beta",
                detail = "detail",
                message = "message",
                progressLabel = "",
                progressValue = 0,
                progressMax = 0,
                progressVisible = false,
                autoUpdate = true,
                internalDownloader = true,
                onDownload = {},
                onAutoUpdateChange = { auto = it },
                onInternalDownloaderChange = { internal = it },
                onBack = {},
            )
        }

        composeRule.onNodeWithTag("update_progress").assertDoesNotExist()
        composeRule.onNodeWithTag("update_auto").performScrollTo().performClick()
        composeRule.onNodeWithTag("update_internal_downloader").performScrollTo().performClick()

        assertThat(auto).isFalse()
        assertThat(internal).isFalse()
    }

    @Test
    fun updateProgressShownWhenDownloading() {
        composeRule.setContent {
            UpdateScreen(
                channel = "c",
                detail = "d",
                message = "m",
                progressLabel = "50 / 100 KB",
                progressValue = 50,
                progressMax = 100,
                progressVisible = true,
                autoUpdate = true,
                internalDownloader = true,
                onDownload = {},
                onAutoUpdateChange = {},
                onInternalDownloaderChange = {},
                onBack = {},
            )
        }

        composeRule.onNodeWithTag("update_progress").assertExists()
        composeRule.onNodeWithTag("update_progress_label").assertTextEquals("50 / 100 KB")
    }
}
