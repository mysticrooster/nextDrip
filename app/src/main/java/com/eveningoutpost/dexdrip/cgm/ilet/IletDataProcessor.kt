package com.eveningoutpost.dexdrip.cgm.ilet

import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletAlarmHistory
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletAlgoStepRecord
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletHistoricalGlucoseRecord
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletHistoricalInsulinRecord
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletRealTimeControlPumpData
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletRealtimeSnapshot
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletRealTimeData
import com.eveningoutpost.dexdrip.models.APStatus
import com.eveningoutpost.dexdrip.models.BgReading
import com.eveningoutpost.dexdrip.models.PumpIobReading
import com.eveningoutpost.dexdrip.models.Treatments
import com.eveningoutpost.dexdrip.models.UserError
import com.eveningoutpost.dexdrip.utilitymodels.PumpStatus
import java.util.UUID

/**
 * Maps iLet pump records onto xDrip models: glucose to BgReading (collector mode
 * only), boluses to Treatments (insulin only, no carbs), basal to APStatus, and
 * realtime status to PumpStatus / PumpIobReading / IletState.
 */
class IletDataProcessor {

    /** RTC -> Unix offset, already corrected for the phone timezone (used for timestamps). */
    @Volatile
    var epochOffsetSeconds: Long = -1L
        private set

    /** The raw GetTime offset, used only to sanity-check a record's embedded pump epoch. */
    @Volatile
    var rawPumpOffsetSeconds: Long = -1L
        private set

    /**
     * Residual pump-vs-phone clock error in seconds after the timezone correction
     * (positive = the pump clock is behind true time). Folded into
     * [epochOffsetSeconds] so every pump timestamp lands on real time, and stored so
     * the UI can still warn that the pump clock itself is wrong.
     */
    @Volatile
    var clockDriftSeconds: Long = 0L
        private set

    private var lastIobStoreMs = 0L
    private var lastAlgoMs = 0L
    private var lastRealtimeKind = -1
    private var lastMealAnnouncementState = -1
    private var lastGlucoseMarkerLog = 0L
    private var lastPatchExpiryLogged = 0L

    // Per-backfill-pass counters, logged so an empty import can be explained.
    private var statGlucoseInserted = 0
    private var statGlucoseSkipped = 0
    private var statGlucoseExisting = 0
    private var statInsulinBoluses = 0
    private var statInsulinNotes = 0
    private var statInsulinZero = 0
    private var statInsulinDup = 0
    private var statInsulinCorrected = 0

    /**
     * Adopt the RTC->Unix offset derived from GetTime (0x2510).
     *
     * The pump syncs its clock from the last phone that paired. Depending on that
     * phone it stores either true UTC or that phone's *local* wall clock. Treating a
     * local wall clock as UTC shifts every timestamp earlier by the timezone (4 h for
     * US Eastern). Decide which interpretation matches the phone's clock now and apply
     * the timezone correction only when it does.
     */
    @JvmOverloads
    fun updateEpoch(
        offsetSeconds: Long,
        pumpWallEpochSeconds: Long = -1L,
        explicitTzOffsetSeconds: Long? = null,
    ) {
        rawPumpOffsetSeconds = offsetSeconds
        val nowMs = System.currentTimeMillis()
        val tzOffsetSeconds = explicitTzOffsetSeconds
            ?: (java.util.TimeZone.getDefault().getOffset(nowMs) / 1000L)
        var correction = 0L
        if (pumpWallEpochSeconds > 0) {
            val drift = nowMs / 1000L - pumpWallEpochSeconds
            val utcError = kotlin.math.abs(drift)
            val localError = kotlin.math.abs(drift + tzOffsetSeconds)
            if (localError < utcError) correction = tzOffsetSeconds
        }
        // Residual pump-vs-phone clock error after that timezone correction. Folding it
        // in means every pump timestamp (glucose, insulin, basal, suspend, patch session,
        // realtime) lands on the phone's true clock instead of the pump's own clock.
        var driftSeconds = 0L
        if (pumpWallEpochSeconds > 0) {
            driftSeconds = nowMs / 1000L - (pumpWallEpochSeconds - correction)
        }
        clockDriftSeconds = driftSeconds
        epochOffsetSeconds = offsetSeconds - correction + driftSeconds
        UserError.Log.d(
            TAG,
            "epoch: pumpOffset=$offsetSeconds wall=$pumpWallEpochSeconds tz=$tzOffsetSeconds " +
                "correction=$correction drift=$driftSeconds adjusted=$epochOffsetSeconds"
        )
    }

    /** The pump epoch a record should carry (raw, not timezone-corrected). */
    private fun expectedPumpEpoch(): Long =
        if (rawPumpOffsetSeconds >= 0) rawPumpOffsetSeconds else epochOffsetSeconds

    /** Call before an oldest->newest algo walk (APStatus rejects out-of-order rows). */
    fun resetAlgoOrdering() {
        lastAlgoMs = 0L
    }

    private val serial: String get() = IletPrefs.pumpNamespace()

    /**
     * Dedupe window for iLet glucose rows: half the primary collector's sample period, but at
     * least the iLet-wide window (realtime vs backfilled rows for one pump CGM slot can differ
     * by a little). One value used by both the realtime and history paths so they cannot
     * disagree. The measured pump-vs-phone drift is already folded into the timestamps, so it
     * is no longer added to the window — the old widening against the drift was what made
     * corrected rows collapse onto the stale, uncorrected ones.
     */
    private fun glucoseDedupeWindow(): Long {
        return maxOf(
            BgReading.ILET_DEDUPE_WINDOW_MS,
            com.eveningoutpost.dexdrip.utils.DexCollectionType.getCollectorSamplePeriod(
                com.eveningoutpost.dexdrip.utils.DexCollectionType.getDexCollectionType()
            ) / 2,
        )
    }

    private fun hex(bytes: ByteArray, from: Int, to: Int): String =
        bytes.copyOfRange(from, minOf(to, bytes.size)).joinToString("") { "%02x".format(it) }

    fun resetHistoryStats() {
        statGlucoseInserted = 0
        statGlucoseSkipped = 0
        statGlucoseExisting = 0
        statInsulinBoluses = 0
        statInsulinNotes = 0
        statInsulinZero = 0
        statInsulinDup = 0
        statInsulinCorrected = 0
    }

    fun historyStats(): String =
        "glucoseInserted=$statGlucoseInserted glucoseExisting=$statGlucoseExisting glucoseSkipped=$statGlucoseSkipped " +
            "insulinBoluses=$statInsulinBoluses insulinNotes=$statInsulinNotes " +
            "insulinZero=$statInsulinZero insulinDup=$statInsulinDup insulinCorrected=$statInsulinCorrected"

    // ------------------------------------------------------------ realtime

    fun processRealtime(data: IletRealTimeData) {
        PumpStatus.setReservoir(data.cartridgeRemainingUnits)
        PumpStatus.setBolusIoB(data.iobUnits)
        PumpStatus.setBattery(data.batteryPct.toDouble())
        PumpStatus.syncUpdate()
        IletState.update(data)
        if (!data.isInsulinPaused) IletPrefs.setInsulinPausedSinceMs(0L)

        maybeStorePumpIob(data)

        val reading = data.glucose.readingMgDl
        if (reading <= 0) return
        val offset = if (epochOffsetSeconds >= 0) epochOffsetSeconds else data.clockOffset
        val timestamp = IletMapping.glucoseMillis(data.glucose.currentTimestamp, offset)
        maybeStorePredictedBg(data.glucose.predictedReading, reading, timestamp)
        if (!IletPrefs.downloadGlucose()) return
        BgReading.bgReadingInsertFromIlet(reading.toDouble(), timestamp, null, glucoseDedupeWindow())
    }

    /** Dispatch a real-time packet that may be 0x1506 or the 0x1515 superset. */
    fun processRealtimeSnapshot(snapshot: IletRealtimeSnapshot) {
        // Testing only: log when the packet kind changes so it is obvious whether the pump
        // ever emits the 0x1515 control superset, without a line every cycle.
        val kind = when (snapshot) {
            is IletRealtimeSnapshot.Standard -> 0x1506
            is IletRealtimeSnapshot.Control -> 0x1515
        }
        if (kind != lastRealtimeKind) {
            lastRealtimeKind = kind
            UserError.Log.uel(TAG, "iLet realtime packet switched to 0x" + kind.toString(16))
        }
        when (snapshot) {
            is IletRealtimeSnapshot.Standard -> processRealtime(snapshot.data)
            is IletRealtimeSnapshot.Control -> processControl(snapshot.data)
        }
    }

    /** 0x1515 adds meal-announcement state and the pump-patch (infusion set) session window. */
    fun processControl(data: IletRealTimeControlPumpData) {
        PumpStatus.setReservoir(data.cartridgeRemainingUnits)
        PumpStatus.setBolusIoB(data.iobUnits)
        PumpStatus.setBattery(data.batteryPct.toDouble())
        PumpStatus.syncUpdate()
        IletState.updateControl(data)
        maybeStorePumpIobRaw(data.iobUnits, data.cartridgeRemainingUnits, data.batteryPct.toDouble())

        // Suspend start time is only in the 0x1515 control packet.
        val controlOffset = if (epochOffsetSeconds >= 0) epochOffsetSeconds else data.clockOffset
        if (data.isInsulinPaused && data.insulinPausedTime > 0) {
            IletPrefs.setInsulinPausedSinceMs((data.insulinPausedTime + controlOffset) * 1000L)
        } else if (!data.isInsulinPaused) {
            IletPrefs.setInsulinPausedSinceMs(0L)
        }

        val reading = data.glucose.readingMgDl
        if (reading > 0) {
            val offset = if (epochOffsetSeconds >= 0) epochOffsetSeconds else data.clockOffset
            val timestamp = IletMapping.glucoseMillis(data.glucose.currentTimestamp, offset)
            maybeStorePredictedBg(data.glucose.predictedReading, reading, timestamp)
            if (IletPrefs.downloadGlucose()) {
                BgReading.bgReadingInsertFromIlet(reading.toDouble(), timestamp, null, glucoseDedupeWindow())
            }
        }

        logMealAnnouncement(data.mealAnnouncementState)
        logPatchSession(data)
        UserError.Log.uel(TAG, "iLet 0x1515 body\n" + data.formatString())
    }

    private fun logMealAnnouncement(state: Int) {
        if (state <= 0 || state == lastMealAnnouncementState) return
        UserError.Log.uel(TAG, "iLet meal announcement: state=$state (raw; enum unverified)")
        lastMealAnnouncementState = state
    }

    private fun logPatchSession(data: IletRealTimeControlPumpData) {
        val start = data.pumpPatchInfoSessionStartTime
        val end = data.pumpPatchInfoSessionEndTime
        if (start == 0L && end == 0L) return
        // Timestamps are on the pump clock; convert with the session offset when known.
        val offset = if (epochOffsetSeconds >= 0) epochOffsetSeconds else data.clockOffset
        val startMs = if (start > 0) (start + offset) * 1000L else 0L
        val endMs = if (end > 0) (end + offset) * 1000L else 0L
        if (startMs > 0) IletPrefs.setPatchSessionStartMs(startMs)
        if (endMs > 0) IletPrefs.setPatchSessionEndMs(endMs)
        if (endMs > 0 && endMs != lastPatchExpiryLogged) {
            lastPatchExpiryLogged = endMs
            UserError.Log.uel(
                TAG,
                "iLet patch/infusion-set session: end ${com.eveningoutpost.dexdrip.models.JoH.dateTimeText(endMs)}" +
                    if (endMs < System.currentTimeMillis()) " (EXPIRED)" else ""
            )
        }
    }

    private fun maybeStorePumpIob(data: IletRealTimeData) {
        maybeStorePumpIobRaw(data.iobUnits, data.cartridgeRemainingUnits, data.batteryPct.toDouble())
    }

    private fun maybeStorePumpIobRaw(iobUnits: Double, reservoirUnits: Double, batteryPct: Double) {
        val now = System.currentTimeMillis()
        // Time-gated only: realtime packets stream continuously.
        if (now - lastIobStoreMs < IOB_STORE_INTERVAL_MS) return
        lastIobStoreMs = now
        PumpIobReading.store(now, iobUnits, reservoirUnits, batteryPct)
    }

    /**
     * Remember the pump's predicted BG immediately for the newest reading so the graph can
     * draw a live stub. Nothing is persisted as history: every packet updates the same
     * current value, so it is only ever shown for the latest reading.
     */
    private fun maybeStorePredictedBg(predictedMgdl: Int, readingMgdl: Int, timestampMs: Long) {
        if (predictedMgdl <= 0) return
        IletPrefs.setPredictedBg(predictedMgdl, readingMgdl, timestampMs)
    }

    // ------------------------------------------------------------ history

    fun processHistoricalGlucose(rec: IletHistoricalGlucoseRecord) {
        if (!rec.hasExpectedEpoch(expectedPumpEpoch())) {
            // The parser already verified the body magic and checksum, so a mismatch is a
            // clock-skew warning, not a reason to drop data.
            UserError.Log.e(TAG, "glucose record epoch ${rec.pumpEpoch} != expected ${expectedPumpEpoch()}; using rtc+offset anyway")
        }
        // Historical glucose is opt-in and independent of the live collector: the user
        // must enable glucose backfill, but it works in pump-only mode too.
        if (!IletPrefs.backfillGlucose()) return
        // Only a positive reading is required. The CGM "active" marker is not reliable on
        // sector-sourced history, so it is logged for diagnosis but does not gate import.
        if (rec.readingMgDl <= 0) {
            statGlucoseSkipped++
            return
        }
        if (!rec.cgmActive && System.currentTimeMillis() - lastGlucoseMarkerLog > 60_000L) {
            lastGlucoseMarkerLog = System.currentTimeMillis()
            UserError.Log.d(
                TAG,
                "glucose import with cgmActive=false seq=${rec.sequenceNumber} reading=${rec.readingMgDl} " +
                    "marker=${hex(rec.raw, 57, 61)} body53=${hex(rec.raw, 53, 61)}"
            )
        }
        val timestamp = IletMapping.glucoseMillis(rec.currentTimestamp, epochOffsetSeconds)
        // Only fill gaps: skip if any BgReading already covers this slot, whether from a
        // previous iLet import or from the primary xDrip CGM. The dedupe (with the shared
        // window) now happens entirely inside the inserter, so this is a single query per
        // record; a null result means the slot was already covered.
        val reading = BgReading.bgReadingInsertFromIlet(
            rec.readingMgDl.toDouble(), timestamp, null, glucoseDedupeWindow()
        )
        if (reading != null) statGlucoseInserted++ else statGlucoseExisting++
    }

    fun processHistoricalInsulin(rec: IletHistoricalInsulinRecord) {
        if (!rec.hasExpectedEpoch(expectedPumpEpoch())) {
            UserError.Log.e(TAG, "insulin record epoch ${rec.pumpEpoch} != expected ${expectedPumpEpoch()}; using rtc+offset anyway")
        }
        if (!IletPrefs.downloadBoluses()) return
        val units = rec.totalDoseUnits
        val mealAllowed = IletPrefs.downloadMeals()
        val label = if (mealAllowed) IletMapping.mealLabel(rec.mealType, rec.mealSize) else null
        if (rec.mealType != 0) {
            UserError.Log.d(
                TAG,
                "iLet insulin record meal seq=${rec.sequenceNumber} dose=${units}U " +
                    "type=${rec.mealType} size=${rec.mealSize} mealAllowed=$mealAllowed label=$label"
            )
        }
        // Zero-dose rows are periodic snapshots, not boluses; a meal with no dose is kept as a note.
        if (units <= 0 && label == null) {
            statInsulinZero++
            return
        }

        val timestamp = IletMapping.insulinMillis(rec.recordTimestampRtc, epochOffsetSeconds)
        val uuid = UUID.nameUUIDFromBytes("ilet:$serial:insulin:${rec.sequenceNumber}".toByteArray()).toString()
        val existing = Treatments.byuuid(uuid)
        if (existing != null) {
            // Already imported: correct the timestamp/note in place (deterministic UUID means
            // a plain re-import would be skipped and any earlier wrong time would persist).
            if (Treatments.reconcileIletTreatment(existing, timestamp, label)) statInsulinCorrected++ else statInsulinDup++
            return
        }
        if (units > 0) {
            Treatments.createWithNotes(units, timestamp, uuid, label)
            statInsulinBoluses++
        } else {
            Treatments.create_note(label, timestamp, -1.0, uuid)
            statInsulinNotes++
        }
    }

    fun processAlgoStep(rec: IletAlgoStepRecord) {
        UserError.Log.uel(TAG, "iLet algo-step body\n" + rec.formatString())
        if (!IletPrefs.downloadBasal()) return
        val rate = IletMapping.basalRate(rec.instantBasalX10, rec.nominalBasalX10) ?: return
        val timestamp = IletMapping.insulinMillis(rec.recordTimestampRtc, epochOffsetSeconds)
        if (timestamp <= lastAlgoMs) return
        lastAlgoMs = timestamp
        APStatus.createEfficientRecord(timestamp, rate)
    }

    /** Alarm records are empty on current firmware; enum meaning is unverified. */
    fun processAlarm(rec: IletAlarmHistory) {
        UserError.Log.uel(
            TAG,
            "iLet alarm seq=${rec.sequenceNumber} enum=${rec.alertEnum} level=${rec.alertLevel} status=${rec.alertStatus}"
        )
    }

    companion object {
        private const val TAG = "iLet"
        private const val IOB_STORE_INTERVAL_MS = 5 * 60 * 1000L
    }
}
