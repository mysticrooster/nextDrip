package com.eveningoutpost.dexdrip.cgm.ilet

import org.junit.Test

/** Pure mapping-check coverage for [IletMapping]. */
class IletMappingTest {

    @Test
    fun glucoseMillisAddsTheOffset() {
        check(IletMapping.glucoseMillis(900_000_000L, 835_479_720L) == 1_735_479_720_000L)
    }

    @Test
    fun historicalGlucoseMillisRejectsAMismatchedEpoch() {
        check(IletMapping.historicalGlucoseMillis(1_700_000_000L, 835_479_720L, 835_479_720L) == 2_535_479_720_000L)
        check(IletMapping.historicalGlucoseMillis(1_700_000_000L, 1L, 835_479_720L) == null)
    }

    @Test
    fun insulinMillisUsesTheValidatedRtcOffset() {
        // The pump's UTC field can be years stale, so rtc + GetTime offset wins.
        check(IletMapping.insulinMillis(900_000_000L, 835_479_720L) == 1_735_479_720_000L)
    }

    @Test
    fun doseAndBasalConversions() {
        check(Math.abs(IletMapping.totalDoseUnits(2500) - 2.5) < 1e-9)
        check(IletMapping.basalRate(15, 10) == 1.5)
        check(IletMapping.basalRate(0, 12) == 1.2)
        check(IletMapping.basalRate(0, 0) == null)
    }

    @Test
    fun mealLabelMapsTypeAndSize() {
        check(IletMapping.mealLabel(0, 4) == null)
        check(IletMapping.mealLabel(1, 0) == "Breakfast, More")
        check(IletMapping.mealLabel(2, 1) == "Lunch, Usual")
        check(IletMapping.mealLabel(3, 2) == "Dinner, Less")
        check(IletMapping.mealLabel(9, 9) == "Meal, size 9")
    }
}
