package com.eveningoutpost.dexdrip.insulin

import com.eveningoutpost.dexdrip.cgm.ilet.IletPrefs
import com.eveningoutpost.dexdrip.utilitymodels.Pref
import com.eveningoutpost.dexdrip.utils.DexCollectionType

/**
 * Registry and selection state for the `Devices → Insulin Pumps` selector.
 *
 * The selector is intentionally general and lists every pump driver, whether or
 * not it can supply glucose. A pump with a non-null [Pump.collectorName] can be
 * made the primary glucose source; the primary source is always
 * `dex_collection_method`, so the Hardware Data Source list mirrors this
 * selection. A pump may also be active purely for pump data while a different
 * collector supplies glucose (pump-only mode).
 *
 * To add a future driver, append a [Pump] entry; use
 * [DexCollectionType.getInternalName] for a CGM-capable pump, or `null` for a
 * pump-only driver.
 */
object InsulinPumps {

    const val NONE = "None"
    const val PREF_SELECTED = "pump_selected"
    const val PREF_PREVIOUS_COLLECTOR = "pump_previous_collector"

    data class Pump(val value: String, val collectorName: String?) {
        val canBeGlucoseSource: Boolean get() = collectorName != null
    }

    val ALL: List<Pump> = listOf(
        Pump(value = "iLet", collectorName = DexCollectionType.ILet.internalName),
    )

    @JvmStatic
    fun pumpFor(value: String): Pump? = ALL.firstOrNull { it.value == value }

    @JvmStatic
    fun selectedValue(): String = Pref.getString(PREF_SELECTED, NONE)

    @JvmStatic
    fun selectedPump(): Pump? = pumpFor(selectedValue())

    @JvmStatic
    fun setSelectedValue(value: String) {
        Pref.setString(PREF_SELECTED, value)
    }

    /** The collector to restore when the pump stops being the glucose source. */
    @JvmStatic
    fun previousCollector(): String =
        Pref.getString(PREF_PREVIOUS_COLLECTOR, DexCollectionType.Disabled.internalName)

    @JvmStatic
    fun rememberCollector(name: String) {
        Pref.setString(PREF_PREVIOUS_COLLECTOR, name)
    }

    /** True when the given collector name belongs to a registered pump. */
    @JvmStatic
    fun pumpForCollector(collectorName: String): Pump? = ALL.firstOrNull { it.collectorName == collectorName }

    /**
     * Enable/disable the backing pump driver for [pump]. iLet is the only
     * driver today, so any other selection (including `null`) disables it;
     * future entries branch here.
     */
    @JvmStatic
    fun setDriverEnabled(pump: Pump?, enabled: Boolean) {
        IletPrefs.setEnabled(enabled && pump?.value == "iLet")
    }
}
