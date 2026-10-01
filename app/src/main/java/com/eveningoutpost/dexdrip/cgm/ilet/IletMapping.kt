package com.eveningoutpost.dexdrip.cgm.ilet

/**
 * Pure mapping helpers for turning iLet protocol values into xDrip units. Kept
 * free of Android imports so the arithmetic can be unit-tested.
 */
object IletMapping {

    /** Convert a pump RTC timestamp (seconds) to Unix millis. */
    fun glucoseMillis(rtcSeconds: Long, epochOffsetSeconds: Long): Long =
        (rtcSeconds + epochOffsetSeconds) * 1000L

    /**
     * Unix millis for a historical glucose row, or null when the record's
     * embedded RTC offset does not match the GetTime-derived offset (layout
     * changed -> skip and log).
     */
    fun historicalGlucoseMillis(
        currentRtcSeconds: Long,
        recordPumpEpoch: Long,
        expectedEpochOffset: Long,
    ): Long? = if (recordPumpEpoch == expectedEpochOffset) {
        (currentRtcSeconds + expectedEpochOffset) * 1000L
    } else {
        null
    }

    /**
     * A historical insulin row's Unix millis.
     *
     * The pump's internal UTC field is unreliable (observed stuck at 2019 on a
     * production unit), so the validated RTC plus the GetTime-derived offset is
     * used unconditionally, matching the glucose path.
     */
    fun insulinMillis(rtcSeconds: Long, epochOffsetSeconds: Long): Long =
        (rtcSeconds + epochOffsetSeconds) * 1000L

    /** Total dose (units) from the pump's x1000 integer. */
    fun totalDoseUnits(units1000: Int): Double = units1000 / 1000.0

    /**
     * Carbs for a meal row. The meal_type/meal_size semantics are not yet
     * characterised against a live pump, so this returns 0 and the caller keeps
     * the insulin-only treatment. Encode the real mapping here once known.
     */
    fun mealCarbs(mealType: Int, mealSize: Int): Double = 0.0

    /** Basal rate (U/h) from the algorithm record, preferring the instant rate. */
    fun basalRate(instantBasalX10: Int, nominalBasalX10: Int): Double? {
        val value = if (instantBasalX10 > 0) instantBasalX10 else nominalBasalX10
        return if (value <= 0) null else value / 10.0
    }
}
