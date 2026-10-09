package com.eveningoutpost.dexdrip.cgm.ilet

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig
import org.junit.Assert.assertEquals
import org.junit.Test

/** Locks the pump-wall-clock -> true-UTC timezone auto-correction. */
class IletEpochTest : RobolectricTestWithConfig() {

    private fun nowSec() = System.currentTimeMillis() / 1000L

    @Test
    fun phoneLocalWallClockIsCorrectedBackToUtc() {
        val tzSec = -4L * 3600L // US Eastern
        val pumpOffset = 835_479_720L
        val processor = IletDataProcessor()
        // Pump stored the phone's local wall clock (= UTC + tz).
        processor.updateEpoch(pumpOffset, nowSec() + tzSec, tzSec)
        assertEquals(pumpOffset - tzSec, processor.epochOffsetSeconds)
        assertEquals(pumpOffset, processor.rawPumpOffsetSeconds)
    }

    @Test
    fun trueUtcWallClockIsLeftAlone() {
        val tzSec = -4L * 3600L
        val pumpOffset = 835_479_720L
        val processor = IletDataProcessor()
        processor.updateEpoch(pumpOffset, nowSec(), tzSec)
        assertEquals(pumpOffset, processor.epochOffsetSeconds)
    }

    @Test
    fun easternTimezoneLocalWallClockIsCorrected() {
        val tzSec = 2L * 3600L // Central Europe
        val pumpOffset = 835_479_720L
        val processor = IletDataProcessor()
        processor.updateEpoch(pumpOffset, nowSec() + tzSec, tzSec)
        assertEquals(pumpOffset - tzSec, processor.epochOffsetSeconds)
    }
}
