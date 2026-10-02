package com.eveningoutpost.dexdrip.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import com.eveningoutpost.dexdrip.models.JoH
import com.eveningoutpost.dexdrip.utilitymodels.Constants
import com.eveningoutpost.dexdrip.utilitymodels.Pref
import com.eveningoutpost.dexdrip.utils.SettingsSupport

/**
 * Pref access + formatting helpers shared by the Compose settings screens.
 *
 * Backed by the same preference keys as the legacy settings, so nothing about the stored data
 * changes; these mirror the legacy `bindPreferenceSummaryToValue*` formatting helpers.
 */
object SettingsPrefs {
    const val UNIT_MGDL = "mgdl"
    const val UNIT_MMOL = "mmol"

    fun units(): String = Pref.getString("units", UNIT_MGDL)

    fun isMgdl(): Boolean = units() == UNIT_MGDL

    fun unitSuffix(): String = if (isMgdl()) "mg/dl" else "mmol/l"

    fun isNumeric(value: String): Boolean = SettingsSupport.isNumeric(value)

    /** Summary used by the legacy `sBindNumericPreferenceSummaryToValueListener`. */
    fun numericSummary(value: String): String = value

    /** Summary used by the legacy `sBindNumericUnitizedPreferenceSummaryToValueListener`. */
    fun unitizedSummary(value: String): String = "$value  ${unitSuffix()}"

    /**
     * Mirrors the range check in the legacy unitized numeric listener: the value must parse and,
     * converted to mg/dL, fall within the accepted glucose input range.
     */
    fun isValidGlucoseInput(value: String): Boolean {
        if (!SettingsSupport.isNumeric(value)) return false
        val mgdl = if (isMgdl()) {
            JoH.tolerantParseDouble(value)
        } else {
            JoH.tolerantParseDouble(value) * Constants.MMOLL_TO_MGDL
        }
        return mgdl in SettingsSupport.MIN_GLUCOSE_INPUT..SettingsSupport.MAX_GLUCOSE_INPUT
    }
}

/**
 * Snapshot-buffered view of preferences for a settings screen.
 *
 * Reads are reactive: after a value is written through this holder, any composable that read the
 * key recomposes. Initial reads fall through to [Pref]; writes go to both the [Pref] store and the
 * local snapshot.
 */
class SettingsState {
    private val overrides = mutableStateMapOf<String, Any>()

    fun bool(key: String, default: Boolean): Boolean =
        (overrides[key] as? Boolean) ?: Pref.getBoolean(key, default)

    fun string(key: String, default: String): String =
        (overrides[key] as? String) ?: (Pref.getString(key, default) ?: default)

    fun long(key: String, default: Long): Long =
        (overrides[key] as? Long) ?: Pref.getLong(key, default)

    fun int(key: String, default: Int): Int =
        (overrides[key] as? Int) ?: Pref.getInt(key, default)

    fun setBool(key: String, value: Boolean) {
        overrides[key] = value
        Pref.setBoolean(key, value)
    }

    fun setString(key: String, value: String) {
        overrides[key] = value
        Pref.setString(key, value)
    }

    fun setLong(key: String, value: Long) {
        overrides[key] = value
        Pref.setLong(key, value)
    }

    fun setInt(key: String, value: Int) {
        overrides[key] = value
        Pref.setInt(key, value)
    }

    /** Drop a local override so subsequent reads fall through to [Pref]. */
    fun clearOverride(key: String) {
        overrides.remove(key)
    }

    /**
     * Reproduces `android:dependency` / `android:disableDependentsState`: a dependent row is
     * enabled when the master equals the "enabled" state (master checked, unless
     * [disableDependentsState] inverts it).
     */
    fun dependentEnabled(masterKey: String, masterDefault: Boolean = true, disableDependentsState: Boolean = false): Boolean {
        val master = bool(masterKey, masterDefault)
        return if (disableDependentsState) !master else master
    }
}

@Composable
fun rememberSettingsState(): SettingsState = remember { SettingsState() }
