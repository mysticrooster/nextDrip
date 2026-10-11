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
    const val BACKFILL_GLUCOSE = "ilet_backfill_glucose"
    const val SHOW_PUMP_IOB_LINE = "show_ilet_pump_iob_line"
    const val SHOW_PREDICTED_BG_LINE = "show_ilet_predicted_bg_line"

    // Latest pump predicted BG, kept in memory-of-prefs only (no history table): the
    // graph draws a single live stub for the newest reading, not a stored series.
    const val PREDICTED_BG_MGDL = "ilet_predicted_bg_mgdl"
    const val PREDICTED_BG_READING_MGDL = "ilet_predicted_bg_reading_mgdl"
    const val PREDICTED_BG_READING_MS = "ilet_predicted_bg_reading_ms"

    const val LAST_ADDRESS = "ilet_last_address"
    const val DEVICE_SERIAL = "ilet_device_serial"
    const val DEVICE_MODEL = "ilet_device_model"

    /**
     * Cheap mirror of IletCredentialStore.hasIdentity() so the UI can gate enrolment
     * without decrypting the keystore-backed store on the main thread.
     */
    const val HAS_IDENTITY = "ilet_has_identity"

    const val WATERMARK_GLUCOSE = "ilet_watermark_glucose"
    const val WATERMARK_INSULIN = "ilet_watermark_insulin"
    const val WATERMARK_ALGO = "ilet_watermark_algo"
    const val WATERMARK_ALARM = "ilet_watermark_alarm"

    /** Pending "backfill from date" request, Unix millis; 0 = none. */
    const val BACKFILL_FROM = "ilet_backfill_from"

    /** True while a user-requested "from date to now" fill is still catching up. */
    const val FORWARD_FILL_ACTIVE = "ilet_forward_fill_active"

    /** Consecutive cycles with no forward-fill progress; resets when data advances. */
    const val FORWARD_FILL_STALLED_CYCLES = "ilet_forward_fill_stalled_cycles"

    /** Publish a 0 U/hr basal profile to Nightscout/Nocturne/Tidepool. */
    const val PUBLISH_ZERO_BASAL = "ilet_publish_zero_basal"

    /** True once the 0 U/hr profile has been published (Nightscout/Nocturne are one-shot). */
    const val ZERO_BASAL_PUBLISHED = "ilet_zero_basal_published"

    /** Drift beyond this (seconds) is worth warning the user about. */
    const val CLOCK_DRIFT_WARN_SECONDS = 120L

    /** Infusion-set (pump patch) session window in Unix millis, from the 0x1515 control packet. */
    const val PATCH_SESSION_START_MS = "ilet_patch_start_ms"
    const val PATCH_SESSION_END_MS = "ilet_patch_end_ms"

    /** Unix millis when insulin delivery was suspended (0 = not suspended), from 0x1515. */
    const val INSULIN_PAUSED_SINCE_MS = "ilet_insulin_paused_since_ms"

    /** Last observed pump-vs-phone clock drift, seconds (positive = pump behind). */
    const val CLOCK_DRIFT_SECONDS = "ilet_clock_drift_seconds"

    /** Unix millis of the last time we prompted about a large drift. */
    const val CLOCK_DRIFT_PROMPT = "ilet_clock_drift_prompt"

    /** Unix millis of the last successful pump contact (device info/time/realtime). */
    const val LAST_CONTACT = "ilet_last_contact"

    /**
     * One-time deep-backfill floor (Unix millis). 0 = not yet run. On the first cycle
     * after install/upgrade the watermarks are rewound to this point so glucose and
     * treatments extend back a full day, not just to wherever the old record-count
     * first run stopped.
     */
    const val DEEP_BACKFILL_FLOOR = "ilet_deep_backfill_floor"

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

    /**
     * Historical (back-filled) glucose import. Opt-in: even when iLet is the
     * collector, the user must enable this to pull glucose history from the pump.
     * Real-time glucose is unaffected.
     */
    @JvmStatic
    fun backfillGlucose(): Boolean = Pref.getBooleanDefaultFalse(BACKFILL_GLUCOSE)

    @JvmStatic
    fun setBackfillGlucose(enabled: Boolean) = Pref.setBoolean(BACKFILL_GLUCOSE, enabled)

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

    /** The pump's own predicted-BG graph line is off by default. */
    @JvmStatic
    fun showPredictedBgLine(): Boolean = Pref.getBooleanDefaultFalse(SHOW_PREDICTED_BG_LINE)

    @JvmStatic
    fun setShowPredictedBgLine(enabled: Boolean) = Pref.setBoolean(SHOW_PREDICTED_BG_LINE, enabled)

    /**
     * Remember the pump's latest predicted BG and the reading it came with, so the graph
     * can draw one live stub. Not a history: overwritten on every CGM slot, so it cannot
     * be drawn for anything but the newest reading.
     */
    @JvmStatic
    fun setPredictedBg(predictedMgdl: Int, readingMgdl: Int, readingMs: Long) {
        Pref.setInt(PREDICTED_BG_MGDL, predictedMgdl)
        Pref.setInt(PREDICTED_BG_READING_MGDL, readingMgdl)
        Pref.setLong(PREDICTED_BG_READING_MS, readingMs)
    }

    @JvmStatic
    fun predictedBgMgdl(): Int = Pref.getInt(PREDICTED_BG_MGDL, 0)

    @JvmStatic
    fun predictedReadingMgdl(): Int = Pref.getInt(PREDICTED_BG_READING_MGDL, 0)

    @JvmStatic
    fun predictedReadingMs(): Long = Pref.getLong(PREDICTED_BG_READING_MS, 0L)

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

    /** Cached identity flag; maintained by IletCredentialStore. Safe on the main thread. */
    @JvmStatic
    fun hasIdentityPref(): Boolean = Pref.getBooleanDefaultFalse(HAS_IDENTITY)

    @JvmStatic
    fun setHasIdentityPref(hasIdentity: Boolean) = Pref.setBoolean(HAS_IDENTITY, hasIdentity)

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

    @JvmStatic
    fun watermarkAlarm(): Long = Pref.getLong(WATERMARK_ALARM, -1L)

    @JvmStatic
    fun setWatermarkAlarm(sequence: Long) = Pref.setLong(WATERMARK_ALARM, sequence)

    // Lower cursor of the contiguous processed range [low, high-watermark], and the
    // oldest sequence we intend to reach. The walk fills newest-first between them.
    @JvmStatic
    fun lowWatermark(typeName: String): Long = Pref.getLong("ilet_watermark_low_$typeName", -1L)

    @JvmStatic
    fun setLowWatermark(typeName: String, sequence: Long) =
        Pref.setLong("ilet_watermark_low_$typeName", sequence)

    @JvmStatic
    fun historyFloor(typeName: String): Long = Pref.getLong("ilet_history_floor_$typeName", -1L)

    @JvmStatic
    fun setHistoryFloor(typeName: String, sequence: Long) =
        Pref.setLong("ilet_history_floor_$typeName", sequence)

    @JvmStatic
    fun backfillFrom(): Long = Pref.getLong(BACKFILL_FROM, 0L)

    @JvmStatic
    fun setBackfillFrom(timestampMs: Long) = Pref.setLong(BACKFILL_FROM, timestampMs)

    @JvmStatic
    fun forwardFillActive(): Boolean = Pref.getBooleanDefaultFalse(FORWARD_FILL_ACTIVE)

    @JvmStatic
    fun setForwardFillActive(active: Boolean) = Pref.setBoolean(FORWARD_FILL_ACTIVE, active)

    @JvmStatic
    fun forwardFillStalledCycles(): Int = Pref.getInt(FORWARD_FILL_STALLED_CYCLES, 0)

    @JvmStatic
    fun setForwardFillStalledCycles(cycles: Int) = Pref.setInt(FORWARD_FILL_STALLED_CYCLES, cycles)

    @JvmStatic
    fun publishZeroBasal(): Boolean = Pref.getBooleanDefaultFalse(PUBLISH_ZERO_BASAL)

    @JvmStatic
    fun zeroBasalPublished(): Boolean = Pref.getBooleanDefaultFalse(ZERO_BASAL_PUBLISHED)

    @JvmStatic
    fun setZeroBasalPublished(published: Boolean) = Pref.setBoolean(ZERO_BASAL_PUBLISHED, published)

    @JvmStatic
    fun insulinPausedSinceMs(): Long = Pref.getLong(INSULIN_PAUSED_SINCE_MS, 0L)

    @JvmStatic
    fun setInsulinPausedSinceMs(millis: Long) = Pref.setLong(INSULIN_PAUSED_SINCE_MS, millis)

    @JvmStatic
    fun patchSessionStartMs(): Long = Pref.getLong(PATCH_SESSION_START_MS, 0L)

    @JvmStatic
    fun setPatchSessionStartMs(millis: Long) = Pref.setLong(PATCH_SESSION_START_MS, millis)

    @JvmStatic
    fun patchSessionEndMs(): Long = Pref.getLong(PATCH_SESSION_END_MS, 0L)

    @JvmStatic
    fun setPatchSessionEndMs(millis: Long) = Pref.setLong(PATCH_SESSION_END_MS, millis)

    @JvmStatic
    fun clockDriftSeconds(): Long = Pref.getLong(CLOCK_DRIFT_SECONDS, 0L)

    @JvmStatic
    fun setClockDriftSeconds(seconds: Long) = Pref.setLong(CLOCK_DRIFT_SECONDS, seconds)

    @JvmStatic
    fun lastClockDriftPrompt(): Long = Pref.getLong(CLOCK_DRIFT_PROMPT, 0L)

    @JvmStatic
    fun setLastClockDriftPrompt(timestampMs: Long) = Pref.setLong(CLOCK_DRIFT_PROMPT, timestampMs)

    @JvmStatic
    fun lastContact(): Long = Pref.getLong(LAST_CONTACT, 0L)

    @JvmStatic
    fun setLastContact(timestampMs: Long) = Pref.setLong(LAST_CONTACT, timestampMs)

    @JvmStatic
    fun deepBackfillFloor(): Long = Pref.getLong(DEEP_BACKFILL_FLOOR, 0L)

    @JvmStatic
    fun setDeepBackfillFloor(timestampMs: Long) = Pref.setLong(DEEP_BACKFILL_FLOOR, timestampMs)
}
