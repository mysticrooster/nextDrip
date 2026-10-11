package com.eveningoutpost.dexdrip.cgm.ilet

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
        // A correctly-locale-adjusted pump needs no residual clock correction.
        assertEquals(0L, processor.clockDriftSeconds)
    }

    @Test
    fun trueUtcWallClockIsLeftAlone() {
        val tzSec = -4L * 3600L
        val pumpOffset = 835_479_720L
        val processor = IletDataProcessor()
        processor.updateEpoch(pumpOffset, nowSec(), tzSec)
        assertEquals(pumpOffset, processor.epochOffsetSeconds)
        assertEquals(0L, processor.clockDriftSeconds)
    }

    @Test
    fun easternTimezoneLocalWallClockIsCorrected() {
        val tzSec = 2L * 3600L // Central Europe
        val pumpOffset = 835_479_720L
        val processor = IletDataProcessor()
        processor.updateEpoch(pumpOffset, nowSec() + tzSec, tzSec)
        assertEquals(pumpOffset - tzSec, processor.epochOffsetSeconds)
    }

    @Test
    fun residualPumpClockErrorIsFoldedIntoTheOffset() {
        val pumpOffset = 835_479_720L
        val processor = IletDataProcessor()
        // UTC-correct timezone, but the pump clock itself is 300 s behind.
        processor.updateEpoch(pumpOffset, nowSec() - 300L, 0L)
        assertTrue("drift ${processor.clockDriftSeconds}", processor.clockDriftSeconds in 298L..302L)
        assertEquals(pumpOffset + processor.clockDriftSeconds, processor.epochOffsetSeconds)
    }

    @Test
    fun correctedTimestampMatchesTrueElapsedTimeWhenPumpClockIsBehind() {
        val processor = IletDataProcessor()
        val nowSec = System.currentTimeMillis() / 1000L
        val pumpWall = nowSec - 300L // pump clock 300 s behind true UTC
        val clockOffset = 835_479_720L
        processor.updateEpoch(clockOffset, pumpWall, 0L)
        // A sample the pump clocked 600 s ago must map to 600 s before true now.
        val rtcNow = pumpWall - clockOffset
        val rtcSample = rtcNow - 600L
        val corrected = rtcSample + processor.epochOffsetSeconds
        assertTrue("corrected=$corrected", Math.abs(corrected - (nowSec - 600L)) <= 1L)
    }

    @Test
    fun correctedTimestampMatchesTrueElapsedTimeWhenPumpClockIsAhead() {
        val processor = IletDataProcessor()
        val nowSec = System.currentTimeMillis() / 1000L
        val pumpWall = nowSec + 420L // pump clock 7 min ahead
        val clockOffset = 835_479_720L
        processor.updateEpoch(clockOffset, pumpWall, 0L)
        val rtcNow = pumpWall - clockOffset
        val rtcSample = rtcNow - 120L
        val corrected = rtcSample + processor.epochOffsetSeconds
        assertTrue("corrected=$corrected", Math.abs(corrected - (nowSec - 120L)) <= 1L)
    }
}
