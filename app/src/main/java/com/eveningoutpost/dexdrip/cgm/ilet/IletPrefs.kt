package com.eveningoutpost.dexdrip.cgm.ilet

import com.eveningoutpost.dexdrip.utilitymodels.Pref
import com.eveningoutpost.dexdrip.utils.DexCollectionType

/**
 * Preference keys and helpers for the iLet integration.
 *
 * Keys are new and namespaced under `ilet_`/`show_ilet_`; no existing preference
 * key or type is touched, so backups and installs keep working.
 */
object IletPrefs {

    const val ENABLED = "ilet_enabled"
    const val DOWNLOAD_BOLUSES = "ilet_download_boluses"
    const val DOWNLOAD_MEALS = "ilet_download_meals"
    const val DOWNLOAD_BASAL = "ilet_download_basal"
    const val SHOW_PUMP_IOB_LINE = "show_ilet_pump_iob_line"

    const val LAST_ADDRESS = "ilet_last_address"
    const val DEVICE_SERIAL = "ilet_device_serial"
    const val DEVICE_MODEL = "ilet_device_model"

    const val WATERMARK_GLUCOSE = "ilet_watermark_glucose"
    const val WATERMARK_INSULIN = "ilet_watermark_insulin"
    const val WATERMARK_ALGO = "ilet_watermark_algo"

    /** Master switch for everything iLet. Off by default. */
    @JvmStatic
    fun isEnabled(): Boolean = Pref.getBooleanDefaultFalse(ENABLED)

    @JvmStatic
    fun setEnabled(enabled: Boolean) = Pref.setBoolean(ENABLED, enabled)

    /** True when iLet is the selected glucose collector. */
    @JvmStatic
    fun isCollectorSelected(): Boolean =
        Pref.getString(DexCollectionType.DEX_COLLECTION_METHOD, "") == "iLet"

    /**
     * Pump-only mode: iLet is enabled but another CGM is the collector. The
     * service still reads boluses/basal/IOB but suppresses BgReading writes.
     */
    @JvmStatic
    fun isPumpOnly(): Boolean = isEnabled() && !isCollectorSelected()

    /**
     * Whether iLet glucose should be written. Being the selected collector is
     * the single definition of "primary glucose source" (see InsulinPumps); in
     * pump-only mode another collector supplies glucose and this is false.
     */
    @JvmStatic
    fun downloadGlucose(): Boolean = isCollectorSelected()

    @JvmStatic
    fun downloadBoluses(): Boolean = Pref.getBoolean(DOWNLOAD_BOLUSES, true)

    @JvmStatic
    fun downloadMeals(): Boolean = Pref.getBoolean(DOWNLOAD_MEALS, true)

    @JvmStatic
    fun downloadBasal(): Boolean = Pref.getBoolean(DOWNLOAD_BASAL, true)

    /** The extra pump-IOB graph line is off by default to avoid a duplicate IOB trace. */
    @JvmStatic
    fun showPumpIobLine(): Boolean = Pref.getBooleanDefaultFalse(SHOW_PUMP_IOB_LINE)

    @JvmStatic
    fun setShowPumpIobLine(enabled: Boolean) = Pref.setBoolean(SHOW_PUMP_IOB_LINE, enabled)

    @JvmStatic
    fun lastAddress(): String = Pref.getString(LAST_ADDRESS, "")

    @JvmStatic
    fun setLastAddress(address: String) = Pref.setString(LAST_ADDRESS, address)

    @JvmStatic
    fun deviceSerial(): String = Pref.getString(DEVICE_SERIAL, "")

    @JvmStatic
    fun setDeviceSerial(serial: String) = Pref.setString(DEVICE_SERIAL, serial)

    @JvmStatic
    fun deviceModel(): String = Pref.getString(DEVICE_MODEL, "")

    /**
     * Stable identity for deterministic treatment UUIDs. The serial is derived
     * from GetDeviceInfo (0x2321); if it has not been seen yet, fall back to the
     * user-set Bluetooth address so dedupe stays stable across sessions.
     */
    @JvmStatic
    fun pumpNamespace(): String = deviceSerial().ifEmpty { lastAddress().ifEmpty { "unknown" } }

    // ------------------------------------------------------------ re-enrolment backoff

    const val REENROL_ATTEMPTS = "ilet_reenrol_attempts"
    const val REENROL_LAST_ATTEMPT = "ilet_reenrol_last_attempt"

    /**
     * True when a rejected credential may be cleared and re-minted. Backs off
     * exponentially (15 min doubling, capped at 6 h) so a pump/account mismatch
     * does not hit the cloud every service cycle.
     */
    @JvmStatic
    fun canReenrol(): Boolean {
        val last = Pref.getLong(REENROL_LAST_ATTEMPT, 0L)
        if (last <= 0L) return true
        return System.currentTimeMillis() - last >= reenrolBackoffMs(Pref.getLong(REENROL_ATTEMPTS, 0L))
    }

    @JvmStatic
    fun noteReenrolAttempt() {
        Pref.setLong(REENROL_ATTEMPTS, Pref.getLong(REENROL_ATTEMPTS, 0L) + 1L)
        Pref.setLong(REENROL_LAST_ATTEMPT, System.currentTimeMillis())
    }

    @JvmStatic
    fun resetReenrolAttempts() {
        Pref.setLong(REENROL_ATTEMPTS, 0L)
        Pref.setLong(REENROL_LAST_ATTEMPT, 0L)
    }

    private fun reenrolBackoffMs(attempts: Long): Long {
        val shift = attempts.coerceIn(0L, 6L).toInt()
        return minOf((15 * 60 * 1000L) shl shift, 6 * 60 * 60 * 1000L)
    }

    @JvmStatic
    fun setDeviceModel(model: String) = Pref.setString(DEVICE_MODEL, model)

    @JvmStatic
    fun watermarkGlucose(): Long = Pref.getLong(WATERMARK_GLUCOSE, -1L)

    @JvmStatic
    fun setWatermarkGlucose(sequence: Long) = Pref.setLong(WATERMARK_GLUCOSE, sequence)

    @JvmStatic
    fun watermarkInsulin(): Long = Pref.getLong(WATERMARK_INSULIN, -1L)

    @JvmStatic
    fun setWatermarkInsulin(sequence: Long) = Pref.setLong(WATERMARK_INSULIN, sequence)

    @JvmStatic
    fun watermarkAlgo(): Long = Pref.getLong(WATERMARK_ALGO, -1L)

    @JvmStatic
    fun setWatermarkAlgo(sequence: Long) = Pref.setLong(WATERMARK_ALGO, sequence)
}
