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
     * BEST-GUESS meal label. The meal_type / meal_size enums are NOT characterised
     * against a live pump (captures only show type/size 1/2 and 0/4), so the names
     * below are unverified. Type 0 means "no meal announced". The raw codes are
     * always included so a wrong guess can be corrected from stored notes.
     * Returns null when no meal was announced.
     */
    fun mealLabel(mealType: Int, mealSize: Int): String? {
        if (mealType == 0) return null
        val type = when (mealType) {
            1 -> "Breakfast"
            2 -> "Lunch"
            3 -> "Dinner"
            else -> "Meal"
        }
        // Meal size is user-confirmed: 0 = Less, 1 = Usual, 2 = More.
        val size = when (mealSize) {
            0 -> "Less"
            1 -> "Usual"
            2 -> "More"
            else -> "size $mealSize"
        }
        return "$type, $size"
    }

    /** Basal rate (U/h) from the algorithm record, preferring the instant rate. */
    fun basalRate(instantBasalX10: Int, nominalBasalX10: Int): Double? {
        val value = if (instantBasalX10 > 0) instantBasalX10 else nominalBasalX10
        return if (value <= 0) null else value / 10.0
    }
}
