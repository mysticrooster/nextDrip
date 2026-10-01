package com.eveningoutpost.dexdrip.cgm.ilet

import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletAlgoStepRecord
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletHistoricalGlucoseRecord
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletHistoricalInsulinRecord
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletRealTimeData
import com.eveningoutpost.dexdrip.db.AppDatabase
import com.eveningoutpost.dexdrip.models.APStatus
import com.eveningoutpost.dexdrip.models.BgReading
import com.eveningoutpost.dexdrip.models.PumpIobReading
import com.eveningoutpost.dexdrip.models.Treatments
import com.eveningoutpost.dexdrip.models.UserError
import com.eveningoutpost.dexdrip.utilitymodels.PumpStatus
import com.eveningoutpost.dexdrip.xdrip
import java.util.UUID

/**
 * Maps iLet pump records onto xDrip models: glucose to BgReading (collector mode
 * only), boluses/meals to Treatments, basal to APStatus, and realtime status to
 * PumpStatus plus the PumpIobReading graph entity.
 */
class IletDataProcessor {

    @Volatile
    var epochOffsetSeconds: Long = -1L
        private set

    private var lastIobStoreMs = 0L

    /** Adopt the RTC->Unix offset derived from GetTime (0x2510). */
    fun updateEpoch(offsetSeconds: Long) {
        epochOffsetSeconds = offsetSeconds
    }

    private val serial: String get() = IletPrefs.pumpNamespace()

    // ------------------------------------------------------------ realtime

    fun processRealtime(data: IletRealTimeData) {
        PumpStatus.setReservoir(data.cartridgeRemainingUnits)
        PumpStatus.setBolusIoB(data.iobUnits)
        PumpStatus.setBattery(data.batteryPct.toDouble())
        PumpStatus.syncUpdate()

        maybeStorePumpIob(data)

        if (!IletPrefs.downloadGlucose()) return
        val reading = data.glucose.readingMgDl
        if (reading <= 0) return
        val offset = if (epochOffsetSeconds >= 0) epochOffsetSeconds else data.clockOffset
        val timestamp = IletMapping.glucoseMillis(data.glucose.currentTimestamp, offset)
        BgReading.bgReadingInsertFromIlet(reading.toDouble(), timestamp, null)
    }

    private fun maybeStorePumpIob(data: IletRealTimeData) {
        val now = System.currentTimeMillis()
        // Time-gated only: realtime packets stream continuously, so a change
        // detector would let a moving IOB write on every notification.
        if (now - lastIobStoreMs < IOB_STORE_INTERVAL_MS) {
            return
        }
        lastIobStoreMs = now
        PumpIobReading.store(now, data.iobUnits, data.cartridgeRemainingUnits, data.batteryPct.toDouble())
    }

    // ------------------------------------------------------------ history

    fun processHistoricalGlucose(rec: IletHistoricalGlucoseRecord) {
        if (rec.pumpEpoch != epochOffsetSeconds) {
            UserError.Log.wtf(TAG, "glucose record epoch ${rec.pumpEpoch} != expected $epochOffsetSeconds; skipping")
            return
        }
        if (!rec.isValidReading(epochOffsetSeconds)) return
        if (!IletPrefs.downloadGlucose()) return
        val timestamp = IletMapping.glucoseMillis(rec.currentTimestamp, epochOffsetSeconds)
        BgReading.bgReadingInsertFromIlet(rec.readingMgDl.toDouble(), timestamp, null)
    }

    fun processHistoricalInsulin(rec: IletHistoricalInsulinRecord) {
        if (rec.pumpEpoch != epochOffsetSeconds) {
            UserError.Log.wtf(TAG, "insulin record epoch ${rec.pumpEpoch} != expected $epochOffsetSeconds; skipping")
            return
        }
        if (!IletPrefs.downloadBoluses()) return
        val units = rec.totalDoseUnits
        val carbs = if (IletPrefs.downloadMeals()) IletMapping.mealCarbs(rec.mealType, rec.mealSize) else 0.0
        if (units <= 0 && carbs <= 0) return

        val timestamp = IletMapping.insulinMillis(rec.recordTimestampRtc, epochOffsetSeconds)
        val uuid = UUID.nameUUIDFromBytes("ilet:$serial:insulin:${rec.sequenceNumber}".toByteArray()).toString()
        if (AppDatabase.getInstance(xdrip.getAppContext()).treatmentsDao().byuuid(uuid) != null) return
        Treatments.create(carbs, units, timestamp, uuid)
    }

    fun processAlgoStep(rec: IletAlgoStepRecord) {
        if (!IletPrefs.downloadBasal()) return
        val rate = IletMapping.basalRate(rec.instantBasalX10, rec.nominalBasalX10) ?: return
        val timestamp = IletMapping.insulinMillis(rec.recordTimestampRtc, epochOffsetSeconds)
        APStatus.createEfficientRecord(timestamp, rate)
    }

    companion object {
        private const val TAG = "iLet"
        private const val IOB_STORE_INTERVAL_MS = 5 * 60 * 1000L
    }
}
