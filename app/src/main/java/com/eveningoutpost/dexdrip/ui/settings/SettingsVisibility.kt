package com.eveningoutpost.dexdrip.ui.settings

import com.eveningoutpost.dexdrip.utilitymodels.Pref
import com.eveningoutpost.dexdrip.utils.DexCollectionType

/**
 * Legacy `removePreference(...)` / `addPreference(...)` gating expressed as Compose conditions.
 *
 * Reads the collection method through [SettingsState] so the screen recomposes when the user
 * changes it (the legacy fragment rebuilt itself for the same reason).
 */
internal object SettingsVisibility {

    fun collectionType(state: SettingsState): DexCollectionType =
        DexCollectionType.getType(state.string("dex_collection_method", "BluetoothWixel"))

    fun isEngineeringMode(): Boolean = Pref.getBoolean("engineering_mode", false)

    fun hasLibre(type: DexCollectionType): Boolean = DexCollectionType.hasLibre(type)

    fun hasWifi(): Boolean = DexCollectionType.hasWifi()

    fun bestCollectorHardwareName(): String = DexCollectionType.getBestCollectorHardwareName()
}
