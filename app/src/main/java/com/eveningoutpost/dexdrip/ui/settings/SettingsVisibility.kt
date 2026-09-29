package com.eveningoutpost.dexdrip.ui.settings

import com.eveningoutpost.dexdrip.utils.DexCollectionType
import com.eveningoutpost.dexdrip.services.DexCollectionService
import com.eveningoutpost.dexdrip.watch.miband.MiBand

/**
 * Legacy `removePreference(...)` / `addPreference(...)` gating expressed as Compose conditions.
 *
 * Reads the collection method through [SettingsState] so the screen recomposes when the user
 * changes it (the legacy fragment rebuilt itself for the same reason).
 */
internal object SettingsVisibility {

    fun collectionType(state: SettingsState): DexCollectionType =
        DexCollectionType.getType(state.string("dex_collection_method", "BluetoothWixel"))

    fun isEngineeringMode(state: SettingsState): Boolean = state.bool("engineering_mode", false)

    fun hasLibre(type: DexCollectionType): Boolean = DexCollectionType.hasLibre(type)

    fun hasWifi(): Boolean = DexCollectionType.hasWifi()

    fun bestCollectorHardwareName(): String = DexCollectionType.getBestCollectorHardwareName()

    fun isBlueReader(): Boolean = DexCollectionService.getBestLimitterHardwareName().equals("BlueReader")

    fun isLibreReceiver(): Boolean = DexCollectionType.getDexCollectionType() == DexCollectionType.LibreReceiver

    /**
     * Pebble sync type as read reactively through [SettingsState], mirroring
     * `PebbleUtil.getCurrentPebbleSyncType()`: the list value defaults to "2" when the master
     * `broadcast_to_pebble` is on, otherwise "1".
     */
    fun pebbleSyncType(state: SettingsState): Int {
        val default = if (state.bool("broadcast_to_pebble", false)) "2" else "1"
        return state.string("broadcast_to_pebble_type", default).toIntOrNull() ?: 1
    }

    /** MiBand model, populated when a band has actually connected (not a `Pref`). */
    fun mibandType(): MiBand.MiBandType = MiBand.getMibandType()
}
