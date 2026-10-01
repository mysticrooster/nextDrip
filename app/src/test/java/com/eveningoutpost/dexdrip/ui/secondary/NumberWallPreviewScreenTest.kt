package com.eveningoutpost.dexdrip.ui.secondary

import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
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
class NumberWallPreviewScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Composable
    private fun screen(
        backgroundSet: Boolean = false,
        multi: Boolean = false,
        width: Int = 100,
        height: Int = 100,
        spacer: Int = 20,
        onWidthChange: (Int) -> Unit = {},
        onHeightChange: (Int) -> Unit = {},
        onSpacerChange: (Int) -> Unit = {},
        onPaletteClick: () -> Unit = {},
        onPaletteLongClick: () -> Unit = {},
        onFolderClick: () -> Unit = {},
        onMultiClick: () -> Unit = {},
    ) = NumberWallPreviewScreen(
        bitmap = null,
        backgroundSet = backgroundSet,
        multi = multi,
        width = width,
        height = height,
        spacer = spacer,
        onWidthChange = onWidthChange,
        onHeightChange = onHeightChange,
        onSpacerChange = onSpacerChange,
        onPaletteClick = onPaletteClick,
        onPaletteLongClick = onPaletteLongClick,
        onFolderClick = onFolderClick,
        onMultiClick = onMultiClick,
        onBack = {},
    )

    @Test
    fun slidersDispatchTheirValues() {
        var width: Int? = null
        var height: Int? = null
        var spacer: Int? = null
        composeRule.setContent {
            screen(
                onWidthChange = { width = it },
                onHeightChange = { height = it },
                onSpacerChange = { spacer = it },
            )
        }

        composeRule.onNodeWithTag("nwp_width").performSemanticsAction(SemanticsActions.SetProgress) { it(360f) }
        composeRule.onNodeWithTag("nwp_height").performSemanticsAction(SemanticsActions.SetProgress) { it(200f) }
        composeRule.onNodeWithTag("nwp_spacer").performSemanticsAction(SemanticsActions.SetProgress) { it(90f) }

        assertThat(width).isEqualTo(360)
        assertThat(height).isEqualTo(200)
        assertThat(spacer).isEqualTo(90)
    }

    @Test
    fun paletteClickAndLongClickDispatch() {
        var clicked = false
        var longClicked = false
        composeRule.setContent { screen(onPaletteClick = { clicked = true }, onPaletteLongClick = { longClicked = true }) }

        composeRule.onNodeWithTag("nwp_palette").performClick()
        composeRule.onNodeWithTag("nwp_palette").performTouchInput { longClick() }

        assertThat(clicked).isTrue()
        assertThat(longClicked).isTrue()
    }

    @Test
    fun folderAndMultiButtonsDispatch() {
        var folder = false
        var multi = false
        composeRule.setContent { screen(onFolderClick = { folder = true }, onMultiClick = { multi = true }) }

        composeRule.onNodeWithTag("nwp_folder").performClick()
        composeRule.onNodeWithTag("nwp_multi").performClick()

        assertThat(folder).isTrue()
        assertThat(multi).isTrue()
    }
}
