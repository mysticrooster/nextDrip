package com.eveningoutpost.dexdrip.ui.theme

import android.content.SharedPreferences
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.eveningoutpost.dexdrip.R
import com.eveningoutpost.dexdrip.utilitymodels.ColorCache
import com.eveningoutpost.dexdrip.utilitymodels.Pref

/**
 * Registry of every theme colour the app uses.
 *
 * The rule is: **Material You is the default value of every colour; any colour the user explicitly
 * picks overrides that default.** Overrides live in [ThemeColorStore] under [key]. Chrome roles map
 * to a Material 3 [ColorScheme] role; data/chart colours keep their legacy `ColorCache` key so the
 * legacy screens stay consistent while they are migrated.
 */
/**
 * Editor grouping, mirroring the legacy `xdrip_plus_color_settings` titled sections (plus
 * [Chrome] for the Material 3 role set added in S0). [NumberWall] colours are rendered on the
 * Number Wall settings screen instead of the theme editor, matching the legacy XML.
 */
enum class ThemeColorGroup {
    Chrome,
    GlucoseValues,
    BgValues,
    TreatmentsPrediction,
    AverageTarget,
    AnnotationsDots,
    Backgrounds,
    PluginsFeatures,
    InsulinColors,
    Flair,
    NumberWall,
}

enum class ThemeColor(
    val key: String,
    val labelRes: Int,
    val group: ThemeColorGroup,
    val legacyColor: ColorCache.X? = null,
) {
    // --- Material 3 chrome roles -------------------------------------------------------------
    PRIMARY("theme_color_primary", R.string.theme_role_primary, ThemeColorGroup.Chrome),
    ON_PRIMARY("theme_color_on_primary", R.string.theme_role_on_primary, ThemeColorGroup.Chrome),
    PRIMARY_CONTAINER("theme_color_primary_container", R.string.theme_role_primary_container, ThemeColorGroup.Chrome),
    ON_PRIMARY_CONTAINER("theme_color_on_primary_container", R.string.theme_role_on_primary_container, ThemeColorGroup.Chrome),
    INVERSE_PRIMARY("theme_color_inverse_primary", R.string.theme_role_inverse_primary, ThemeColorGroup.Chrome),
    SECONDARY("theme_color_secondary", R.string.theme_role_secondary, ThemeColorGroup.Chrome),
    ON_SECONDARY("theme_color_on_secondary", R.string.theme_role_on_secondary, ThemeColorGroup.Chrome),
    SECONDARY_CONTAINER("theme_color_secondary_container", R.string.theme_role_secondary_container, ThemeColorGroup.Chrome),
    ON_SECONDARY_CONTAINER("theme_color_on_secondary_container", R.string.theme_role_on_secondary_container, ThemeColorGroup.Chrome),
    TERTIARY("theme_color_tertiary", R.string.theme_role_tertiary, ThemeColorGroup.Chrome),
    ON_TERTIARY("theme_color_on_tertiary", R.string.theme_role_on_tertiary, ThemeColorGroup.Chrome),
    TERTIARY_CONTAINER("theme_color_tertiary_container", R.string.theme_role_tertiary_container, ThemeColorGroup.Chrome),
    ON_TERTIARY_CONTAINER("theme_color_on_tertiary_container", R.string.theme_role_on_tertiary_container, ThemeColorGroup.Chrome),
    BACKGROUND("theme_color_background", R.string.theme_role_background, ThemeColorGroup.Chrome),
    ON_BACKGROUND("theme_color_on_background", R.string.theme_role_on_background, ThemeColorGroup.Chrome),
    SURFACE("theme_color_surface", R.string.theme_role_surface, ThemeColorGroup.Chrome),
    ON_SURFACE("theme_color_on_surface", R.string.theme_role_on_surface, ThemeColorGroup.Chrome),
    SURFACE_VARIANT("theme_color_surface_variant", R.string.theme_role_surface_variant, ThemeColorGroup.Chrome),
    ON_SURFACE_VARIANT("theme_color_on_surface_variant", R.string.theme_role_on_surface_variant, ThemeColorGroup.Chrome),
    SURFACE_TINT("theme_color_surface_tint", R.string.theme_role_surface_tint, ThemeColorGroup.Chrome),
    INVERSE_SURFACE("theme_color_inverse_surface", R.string.theme_role_inverse_surface, ThemeColorGroup.Chrome),
    INVERSE_ON_SURFACE("theme_color_inverse_on_surface", R.string.theme_role_inverse_on_surface, ThemeColorGroup.Chrome),
    ERROR("theme_color_error", R.string.theme_role_error, ThemeColorGroup.Chrome),
    ON_ERROR("theme_color_on_error", R.string.theme_role_on_error, ThemeColorGroup.Chrome),
    ERROR_CONTAINER("theme_color_error_container", R.string.theme_role_error_container, ThemeColorGroup.Chrome),
    ON_ERROR_CONTAINER("theme_color_on_error_container", R.string.theme_role_on_error_container, ThemeColorGroup.Chrome),
    OUTLINE("theme_color_outline", R.string.theme_role_outline, ThemeColorGroup.Chrome),
    OUTLINE_VARIANT("theme_color_outline_variant", R.string.theme_role_outline_variant, ThemeColorGroup.Chrome),
    SCRIM("theme_color_scrim", R.string.theme_role_scrim, ThemeColorGroup.Chrome),
    SURFACE_BRIGHT("theme_color_surface_bright", R.string.theme_role_surface_bright, ThemeColorGroup.Chrome),
    SURFACE_DIM("theme_color_surface_dim", R.string.theme_role_surface_dim, ThemeColorGroup.Chrome),
    SURFACE_CONTAINER("theme_color_surface_container", R.string.theme_role_surface_container, ThemeColorGroup.Chrome),
    SURFACE_CONTAINER_HIGH("theme_color_surface_container_high", R.string.theme_role_surface_container_high, ThemeColorGroup.Chrome),
    SURFACE_CONTAINER_HIGHEST("theme_color_surface_container_highest", R.string.theme_role_surface_container_highest, ThemeColorGroup.Chrome),
    SURFACE_CONTAINER_LOW("theme_color_surface_container_low", R.string.theme_role_surface_container_low, ThemeColorGroup.Chrome),
    SURFACE_CONTAINER_LOWEST("theme_color_surface_container_lowest", R.string.theme_role_surface_container_lowest, ThemeColorGroup.Chrome),

    // --- Legacy colour-screen groups (order/titles mirror the XML) ---------------------------
    HIGH_VALUES("theme_color_high_values", R.string.high_glucose_values, ThemeColorGroup.GlucoseValues, ColorCache.X.color_high_values),
    IN_RANGE_VALUES("theme_color_in_range_values", R.string.in_range_glucose_values, ThemeColorGroup.GlucoseValues, ColorCache.X.color_inrange_values),
    LOW_VALUES("theme_color_low_values", R.string.low_glucose_values, ThemeColorGroup.GlucoseValues, ColorCache.X.color_low_values),
    BAD_VALUES("theme_color_bad_values", R.string.bad_glucose_values, ThemeColorGroup.GlucoseValues, ColorCache.X.color_bad_values),
    FILTERED("theme_color_filtered", R.string.filtered_values, ThemeColorGroup.GlucoseValues, ColorCache.X.color_filtered),
    HIGH_BG_VALUES("theme_color_high_bg_values", R.string.high_glucose_values, ThemeColorGroup.BgValues, ColorCache.X.color_high_bg_values),
    IN_RANGE_BG_VALUES("theme_color_in_range_bg_values", R.string.in_range_glucose_values, ThemeColorGroup.BgValues, ColorCache.X.color_inrange_bg_values),
    LOW_BG_VALUES("theme_color_low_bg_values", R.string.low_glucose_values, ThemeColorGroup.BgValues, ColorCache.X.color_low_bg_values),
    LOW_PREDICTED_CRITICAL_NOTE("theme_color_low_predicted_critical_note", R.string.low_predicted_too_close_note, ThemeColorGroup.BgValues, ColorCache.X.color_low_predicted_critical_note),
    TREATMENT("theme_color_treatment", R.string.treatment_color, ThemeColorGroup.TreatmentsPrediction, ColorCache.X.color_treatment),
    TREATMENT_DARK("theme_color_treatment_dark", R.string.treatment_color_dark, ThemeColorGroup.TreatmentsPrediction, ColorCache.X.color_treatment_dark),
    PREDICTIVE("theme_color_predictive", R.string.predictive_color, ThemeColorGroup.TreatmentsPrediction, ColorCache.X.color_predictive),
    PREDICTIVE_DARK("theme_color_predictive_dark", R.string.predictive_color_dark, ThemeColorGroup.TreatmentsPrediction, ColorCache.X.color_predictive_dark),
    AVERAGE1_LINE("theme_color_average1_line", R.string.eight_hour_average_line, ThemeColorGroup.AverageTarget, ColorCache.X.color_average1_line),
    AVERAGE2_LINE("theme_color_average2_line", R.string.twenty_four_hour_average_line, ThemeColorGroup.AverageTarget, ColorCache.X.color_average2_line),
    TARGET_LINE("theme_color_target_line", R.string.glucose_target_line, ThemeColorGroup.AverageTarget, ColorCache.X.color_target_line),
    CALIBRATION_DOT_BACKGROUND("theme_color_calibration_dot_background", R.string.blood_test_background, ThemeColorGroup.AnnotationsDots, ColorCache.X.color_calibration_dot_background),
    CALIBRATION_DOT_FOREGROUND("theme_color_calibration_dot_foreground", R.string.blood_test_foreground, ThemeColorGroup.AnnotationsDots, ColorCache.X.color_calibration_dot_foreground),
    TREATMENT_DOT_BACKGROUND("theme_color_treatment_dot_background", R.string.treatment_background, ThemeColorGroup.AnnotationsDots, ColorCache.X.color_treatment_dot_background),
    TREATMENT_DOT_FOREGROUND("theme_color_treatment_dot_foreground", R.string.treatment_foreground, ThemeColorGroup.AnnotationsDots, ColorCache.X.color_treatment_dot_foreground),
    HOME_CHART_BACKGROUND("theme_color_home_chart_background", R.string.main_chart_background, ThemeColorGroup.Backgrounds, ColorCache.X.color_home_chart_background),
    NOTIFICATION_CHART_BACKGROUND("theme_color_notification_chart_background", R.string.notification_chart_background, ThemeColorGroup.Backgrounds, ColorCache.X.color_notification_chart_background),
    WIDGET_CHART_BACKGROUND("theme_color_widget_chart_background", R.string.widget_chart_background, ThemeColorGroup.Backgrounds, ColorCache.X.color_widget_chart_background),
    SECONDARY_GLUCOSE_VALUE("theme_color_secondary_glucose_value", R.string.secondary_plugin_glucose_value, ThemeColorGroup.PluginsFeatures, ColorCache.X.color_secondary_glucose_value),
    STEP_COUNTER1("theme_color_step_counter1", R.string.step_counter_1st_color, ThemeColorGroup.PluginsFeatures, ColorCache.X.color_step_counter1),
    STEP_COUNTER2("theme_color_step_counter2", R.string.step_counter_2nd_color, ThemeColorGroup.PluginsFeatures, ColorCache.X.color_step_counter2),
    HEART_RATE1("theme_color_heart_rate1", R.string.title_color_heart_rate1, ThemeColorGroup.PluginsFeatures, ColorCache.X.color_heart_rate1),
    BASAL_TBR("theme_color_basal_tbr", R.string.title_color_basal_tbr, ThemeColorGroup.InsulinColors, ColorCache.X.color_basal_tbr),
    SMB_ICON("theme_color_smb_icon", R.string.title_color_smb_icon, ThemeColorGroup.InsulinColors, ColorCache.X.color_smb_icon),
    SMB_LINE("theme_color_smb_line", R.string.title_color_smb_line, ThemeColorGroup.InsulinColors, ColorCache.X.color_smb_line),
    UPPER_FLAIR_BAR("theme_color_upper_flair_bar", R.string.upper_title_bar_flair, ThemeColorGroup.Flair, ColorCache.X.color_upper_flair_bar),
    LOWER_FLAIR_BAR("theme_color_lower_flair_bar", R.string.lower_button_bar_falir, ThemeColorGroup.Flair, ColorCache.X.color_lower_flair_bar),
    NUMBER_WALL("theme_color_number_wall", R.string.title_color_number_wall, ThemeColorGroup.NumberWall, ColorCache.X.color_number_wall),
    NUMBER_WALL_SHADOW("theme_color_number_wall_shadow", R.string.title_color_number_wall_shadow, ThemeColorGroup.NumberWall, ColorCache.X.color_number_wall_shadow),
    ;

    val isData: Boolean get() = group != ThemeColorGroup.Chrome

    /** The Material You default for this colour, read from [scheme]. */
    fun defaultFrom(scheme: ColorScheme): Color = when (this) {
        PRIMARY -> scheme.primary
        ON_PRIMARY -> scheme.onPrimary
        PRIMARY_CONTAINER -> scheme.primaryContainer
        ON_PRIMARY_CONTAINER -> scheme.onPrimaryContainer
        INVERSE_PRIMARY -> scheme.inversePrimary
        SECONDARY -> scheme.secondary
        ON_SECONDARY -> scheme.onSecondary
        SECONDARY_CONTAINER -> scheme.secondaryContainer
        ON_SECONDARY_CONTAINER -> scheme.onSecondaryContainer
        TERTIARY -> scheme.tertiary
        ON_TERTIARY -> scheme.onTertiary
        TERTIARY_CONTAINER -> scheme.tertiaryContainer
        ON_TERTIARY_CONTAINER -> scheme.onTertiaryContainer
        BACKGROUND -> scheme.background
        ON_BACKGROUND -> scheme.onBackground
        SURFACE -> scheme.surface
        ON_SURFACE -> scheme.onSurface
        SURFACE_VARIANT -> scheme.surfaceVariant
        ON_SURFACE_VARIANT -> scheme.onSurfaceVariant
        SURFACE_TINT -> scheme.surfaceTint
        INVERSE_SURFACE -> scheme.inverseSurface
        INVERSE_ON_SURFACE -> scheme.inverseOnSurface
        ERROR -> scheme.error
        ON_ERROR -> scheme.onError
        ERROR_CONTAINER -> scheme.errorContainer
        ON_ERROR_CONTAINER -> scheme.onErrorContainer
        OUTLINE -> scheme.outline
        OUTLINE_VARIANT -> scheme.outlineVariant
        SCRIM -> scheme.scrim
        SURFACE_BRIGHT -> scheme.surfaceBright
        SURFACE_DIM -> scheme.surfaceDim
        SURFACE_CONTAINER -> scheme.surfaceContainer
        SURFACE_CONTAINER_HIGH -> scheme.surfaceContainerHigh
        SURFACE_CONTAINER_HIGHEST -> scheme.surfaceContainerHighest
        SURFACE_CONTAINER_LOW -> scheme.surfaceContainerLow
        SURFACE_CONTAINER_LOWEST -> scheme.surfaceContainerLowest
        HIGH_VALUES -> scheme.error
        IN_RANGE_VALUES -> scheme.primary
        LOW_VALUES -> scheme.tertiary
        BAD_VALUES -> scheme.outline
        FILTERED -> scheme.outlineVariant
        HIGH_BG_VALUES -> scheme.errorContainer
        IN_RANGE_BG_VALUES -> scheme.primaryContainer
        LOW_BG_VALUES -> scheme.tertiaryContainer
        LOW_PREDICTED_CRITICAL_NOTE -> scheme.error
        TREATMENT -> scheme.secondary
        TREATMENT_DARK -> scheme.secondaryContainer
        PREDICTIVE -> scheme.tertiary
        PREDICTIVE_DARK -> scheme.tertiaryContainer
        AVERAGE1_LINE -> scheme.primary
        AVERAGE2_LINE -> scheme.secondary
        TARGET_LINE -> scheme.tertiary
        CALIBRATION_DOT_BACKGROUND -> scheme.surfaceVariant
        CALIBRATION_DOT_FOREGROUND -> scheme.onSurfaceVariant
        TREATMENT_DOT_BACKGROUND -> scheme.secondaryContainer
        TREATMENT_DOT_FOREGROUND -> scheme.onSecondaryContainer
        HOME_CHART_BACKGROUND -> scheme.surfaceVariant
        NOTIFICATION_CHART_BACKGROUND -> scheme.surface
        WIDGET_CHART_BACKGROUND -> scheme.surfaceContainer
        SECONDARY_GLUCOSE_VALUE -> scheme.onSurfaceVariant
        STEP_COUNTER1 -> scheme.primary
        STEP_COUNTER2 -> scheme.secondary
        HEART_RATE1 -> scheme.error
        BASAL_TBR -> scheme.secondary
        SMB_ICON -> scheme.primary
        SMB_LINE -> scheme.primary
        UPPER_FLAIR_BAR -> scheme.surfaceVariant
        LOWER_FLAIR_BAR -> scheme.onSurface
        NUMBER_WALL -> scheme.onBackground
        NUMBER_WALL_SHADOW -> scheme.onSurfaceVariant
    }

    /** Applies a user [color] for this role to [scheme] (chrome roles only). */
    fun applyTo(scheme: ColorScheme, color: Color): ColorScheme = when (this) {
        PRIMARY -> scheme.copy(primary = color)
        ON_PRIMARY -> scheme.copy(onPrimary = color)
        PRIMARY_CONTAINER -> scheme.copy(primaryContainer = color)
        ON_PRIMARY_CONTAINER -> scheme.copy(onPrimaryContainer = color)
        INVERSE_PRIMARY -> scheme.copy(inversePrimary = color)
        SECONDARY -> scheme.copy(secondary = color)
        ON_SECONDARY -> scheme.copy(onSecondary = color)
        SECONDARY_CONTAINER -> scheme.copy(secondaryContainer = color)
        ON_SECONDARY_CONTAINER -> scheme.copy(onSecondaryContainer = color)
        TERTIARY -> scheme.copy(tertiary = color)
        ON_TERTIARY -> scheme.copy(onTertiary = color)
        TERTIARY_CONTAINER -> scheme.copy(tertiaryContainer = color)
        ON_TERTIARY_CONTAINER -> scheme.copy(onTertiaryContainer = color)
        BACKGROUND -> scheme.copy(background = color)
        ON_BACKGROUND -> scheme.copy(onBackground = color)
        SURFACE -> scheme.copy(surface = color)
        ON_SURFACE -> scheme.copy(onSurface = color)
        SURFACE_VARIANT -> scheme.copy(surfaceVariant = color)
        ON_SURFACE_VARIANT -> scheme.copy(onSurfaceVariant = color)
        SURFACE_TINT -> scheme.copy(surfaceTint = color)
        INVERSE_SURFACE -> scheme.copy(inverseSurface = color)
        INVERSE_ON_SURFACE -> scheme.copy(inverseOnSurface = color)
        ERROR -> scheme.copy(error = color)
        ON_ERROR -> scheme.copy(onError = color)
        ERROR_CONTAINER -> scheme.copy(errorContainer = color)
        ON_ERROR_CONTAINER -> scheme.copy(onErrorContainer = color)
        OUTLINE -> scheme.copy(outline = color)
        OUTLINE_VARIANT -> scheme.copy(outlineVariant = color)
        SCRIM -> scheme.copy(scrim = color)
        SURFACE_BRIGHT -> scheme.copy(surfaceBright = color)
        SURFACE_DIM -> scheme.copy(surfaceDim = color)
        SURFACE_CONTAINER -> scheme.copy(surfaceContainer = color)
        SURFACE_CONTAINER_HIGH -> scheme.copy(surfaceContainerHigh = color)
        SURFACE_CONTAINER_HIGHEST -> scheme.copy(surfaceContainerHighest = color)
        SURFACE_CONTAINER_LOW -> scheme.copy(surfaceContainerLow = color)
        SURFACE_CONTAINER_LOWEST -> scheme.copy(surfaceContainerLowest = color)
        else -> scheme
    }

    companion object {
        @JvmStatic
        fun fromLegacyColor(color: ColorCache.X?): ThemeColor? =
            if (color == null) null else entries.firstOrNull { it.legacyColor == color }

        @JvmStatic
        fun fromLegacyKey(key: String?): ThemeColor? =
            if (key == null) null else entries.firstOrNull { it.legacyColor?.internalName == key }

        /** The legacy `color_*` XML defaults keyed by preference key, for `SettingsDefaults`. */
        @JvmStatic
        fun legacyColorDefaultsMap(): Map<String, Int> =
            legacyColorDefaults.entries.associate { it.key.internalName to it.value }
    }
}

/** The XML defaults the legacy `ColorPicker` prefs shipped with, used for migration/reset. */
internal val legacyColorDefaults: Map<ColorCache.X, Int> = mapOf(
    ColorCache.X.color_high_values to 0xFFFFBB33.toInt(),
    ColorCache.X.color_inrange_values to 0xFF33B5E6.toInt(),
    ColorCache.X.color_low_values to 0xFFC30909.toInt(),
    ColorCache.X.color_bad_values to 0xFFFFFFFF.toInt(),
    ColorCache.X.color_filtered to 0xFFA0A0A0.toInt(),
    ColorCache.X.color_high_bg_values to 0xFFFFBB33.toInt(),
    ColorCache.X.color_inrange_bg_values to 0xFFFFFFFF.toInt(),
    ColorCache.X.color_low_bg_values to 0xFFC30909.toInt(),
    ColorCache.X.color_low_predicted_critical_note to 0xFFC30909.toInt(),
    ColorCache.X.color_treatment to 0xFF77AA00.toInt(),
    ColorCache.X.color_treatment_dark to 0xFF334400.toInt(),
    ColorCache.X.color_predictive to 0xFFAA66CC.toInt(),
    ColorCache.X.color_predictive_dark to 0xFF7700AA.toInt(),
    ColorCache.X.color_average1_line to 0xFF558800.toInt(),
    ColorCache.X.color_average2_line to 0xFFC56F9D.toInt(),
    ColorCache.X.color_target_line to 0xFFA4A409.toInt(),
    ColorCache.X.color_calibration_dot_background to 0xFFFFFFFF.toInt(),
    ColorCache.X.color_calibration_dot_foreground to 0xFFFF4444.toInt(),
    ColorCache.X.color_treatment_dot_background to 0xFFFFFFFF.toInt(),
    ColorCache.X.color_treatment_dot_foreground to 0xFF77AA00.toInt(),
    ColorCache.X.color_home_chart_background to 0xFF212121.toInt(),
    ColorCache.X.color_notification_chart_background to 0x00000000,
    ColorCache.X.color_widget_chart_background to 0xB3000000.toInt(),
    ColorCache.X.color_secondary_glucose_value to 0x8A6B4C33.toInt(),
    ColorCache.X.color_step_counter1 to 0x3E29937B.toInt(),
    ColorCache.X.color_step_counter2 to 0x3C72C3C0.toInt(),
    ColorCache.X.color_heart_rate1 to 0x2B212FDE.toInt(),
    ColorCache.X.color_basal_tbr to 0xD33172DE.toInt(),
    ColorCache.X.color_smb_icon to 0xD4DED843.toInt(),
    ColorCache.X.color_smb_line to 0xD6597FCC.toInt(),
    ColorCache.X.color_upper_flair_bar to 0x00000000,
    ColorCache.X.color_lower_flair_bar to 0xFF000000.toInt(),
    ColorCache.X.color_number_wall to 0xFFEEEEEE.toInt(),
    ColorCache.X.color_number_wall_shadow to 0xFF000000.toInt(),
)

/**
 * Reads and writes user colour overrides, and resolves the app's effective [ColorScheme].
 *
 * Overrides are stored as ARGB ints under [ThemeColor.key]; absence means "use the Material You
 * default". Data colours additionally mirror into their legacy `ColorCache` key so legacy screens
 * stay consistent during the settings migration.
 */
object ThemeColorStore {

    private const val MIGRATION_FLAG = "theme_color_migration_v1"

    private fun prefs(): SharedPreferences? = Pref.getInstance()

    /** The user's chosen ARGB for [color], or null when it follows the Material You default. */
    fun overrideArgb(color: ThemeColor): Int? {
        val prefs = prefs() ?: return null
        return if (prefs.contains(color.key)) prefs.getInt(color.key, 0) else null
    }

    fun isOverridden(color: ThemeColor): Boolean = overrideArgb(color) != null

    /** The effective ARGB for [color]: the override if present, else [defaultArgb]. */
    fun effectiveArgb(color: ThemeColor, defaultArgb: Int): Int = overrideArgb(color) ?: defaultArgb

    fun setOverride(color: ThemeColor, argb: Int) {
        val prefs = prefs() ?: return
        prefs.edit().putInt(color.key, argb).apply()
        color.legacyColor?.let { Pref.setInt(it.internalName, argb) }
        ColorCache.invalidateCache()
    }

    fun clearOverride(color: ThemeColor) {
        val prefs = prefs() ?: return
        prefs.edit().remove(color.key).apply()
        color.legacyColor?.let { legacy ->
            legacyColorDefaults[legacy]?.let { Pref.setInt(legacy.internalName, it) }
        }
        ColorCache.invalidateCache()
    }

    fun clearAll() {
        val editor = prefs()?.edit() ?: return
        for (color in ThemeColor.entries) {
            editor.remove(color.key)
            color.legacyColor?.let { legacy ->
                legacyColorDefaults[legacy]?.let { editor.putInt(legacy.internalName, it) }
            }
        }
        editor.apply()
        ColorCache.invalidateCache()
    }

    /**
     * Mirrors a legacy `ColorCache` preference change (e.g. from the legacy color page) into an
     * override, so the Compose theme follows it. Called from a preference-change listener.
     */
    @JvmStatic
    fun mirrorLegacyPreference(key: String?) {
        val color = ThemeColor.fromLegacyKey(key) ?: return
        val legacy = color.legacyColor ?: return
        val prefs = prefs() ?: return
        val fallback = legacyColorDefaults[legacy] ?: 0
        val value = try {
            prefs.getInt(legacy.internalName, fallback)
        } catch (e: ClassCastException) {
            return
        }
        if (value == fallback) {
            prefs.edit().remove(color.key).apply()
        } else {
            prefs.edit().putInt(color.key, value).apply()
        }
        ColorCache.invalidateCache()
    }

    /**
     * One-time migration (runs after `setDefaultValues`): copies any legacy colour that differs
     * from its XML default into an override, so existing customisations are preserved instead of
     * being replaced by the Material You default.
     */
    @JvmStatic
    fun migrateLegacyColors() {
        val prefs = prefs() ?: return
        if (prefs.getBoolean(MIGRATION_FLAG, false)) return
        val editor = prefs.edit()
        for (color in ThemeColor.entries) {
            val legacy = color.legacyColor ?: continue
            val default = legacyColorDefaults[legacy] ?: continue
            if (prefs.contains(legacy.internalName)) {
                val current = prefs.getInt(legacy.internalName, default)
                if (current != default) editor.putInt(color.key, current)
            }
        }
        editor.putBoolean(MIGRATION_FLAG, true).apply()
        ColorCache.invalidateCache()
    }
}

/** Resolves the user's colour overrides on top of the Material You [base] scheme. */
fun resolveXdripColorScheme(base: ColorScheme): ColorScheme {
    var scheme = base
    for (color in ThemeColor.entries) {
        if (color.isData) continue
        val argb = ThemeColorStore.overrideArgb(color) ?: continue
        scheme = color.applyTo(scheme, argb.asComposeColor())
    }
    return scheme
}

/** The ARGB the given role currently resolves to, given the Material You [scheme]. */
fun ThemeColor.currentArgb(scheme: ColorScheme): Int =
    ThemeColorStore.overrideArgb(this) ?: defaultFrom(scheme).toArgb()
