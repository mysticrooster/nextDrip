package com.eveningoutpost.dexdrip.ui.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.eveningoutpost.dexdrip.BuildConfig
import com.eveningoutpost.dexdrip.TestingApplication
import com.eveningoutpost.dexdrip.utilitymodels.ColorCache
import com.eveningoutpost.dexdrip.utilitymodels.Pref
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [BuildConfig.targetSDK], application = TestingApplication::class)
class ThemeColorTest {

    @Test
    fun unsetColorHasNoOverrideAndFollowsMaterialDefault() {
        assertThat(ThemeColorStore.overrideArgb(ThemeColor.HIGH_VALUES)).isNull()
        assertThat(ThemeColorStore.isOverridden(ThemeColor.HIGH_VALUES)).isFalse()

        val scheme = lightColorScheme()
        assertThat(ThemeColor.HIGH_VALUES.currentArgb(scheme)).isEqualTo(scheme.error.toArgb())
    }

    @Test
    fun chromeOverrideAppliesToResolvedScheme() {
        val base = lightColorScheme()
        val custom = 0xFFFF0000.toInt()

        ThemeColorStore.setOverride(ThemeColor.PRIMARY, custom)

        val resolved = resolveXdripColorScheme(base)
        assertThat(resolved.primary).isEqualTo(Color(custom))
        // Unrelated roles are untouched.
        assertThat(resolved.secondary).isEqualTo(base.secondary)
    }

    @Test
    fun dataOverrideWritesLegacyKeyAndClearsBackToDefault() {
        val custom = 0xFF00FF00.toInt()
        ThemeColorStore.setOverride(ThemeColor.HIGH_VALUES, custom)

        assertThat(ThemeColorStore.overrideArgb(ThemeColor.HIGH_VALUES)).isEqualTo(custom)
        // Legacy screens (ColorCache.getCol) read the same key.
        assertThat(ColorCache.getCol(ColorCache.X.color_high_values)).isEqualTo(custom)

        ThemeColorStore.clearOverride(ThemeColor.HIGH_VALUES)

        assertThat(ThemeColorStore.overrideArgb(ThemeColor.HIGH_VALUES)).isNull()
        assertThat(ColorCache.getCol(ColorCache.X.color_high_values))
            .isEqualTo(0xFFFFBB33.toInt())
    }

    @Test
    fun clearAllRemovesEveryOverride() {
        ThemeColorStore.setOverride(ThemeColor.PRIMARY, 0xFF010203.toInt())
        ThemeColorStore.setOverride(ThemeColor.HIGH_VALUES, 0xFF040506.toInt())

        ThemeColorStore.clearAll()

        for (color in ThemeColor.entries) {
            assertThat(ThemeColorStore.overrideArgb(color)).isNull()
        }
    }

    @Test
    fun migrationCopiesNonDefaultLegacyColorsOnly() {
        Pref.setInt(ColorCache.X.color_high_values.internalName, 0xFF00FF00.toInt())
        Pref.setInt(ColorCache.X.color_low_values.internalName, 0xFFC30909.toInt())

        ThemeColorStore.migrateLegacyColors()

        // Customised legacy value becomes an override...
        assertThat(ThemeColorStore.overrideArgb(ThemeColor.HIGH_VALUES)).isEqualTo(0xFF00FF00.toInt())
        // ...while a value equal to the old default is left to follow Material You.
        assertThat(ThemeColorStore.overrideArgb(ThemeColor.LOW_VALUES)).isNull()
    }
}
