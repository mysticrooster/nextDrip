package com.eveningoutpost.dexdrip.models

import com.activeandroid.query.Delete
import com.eveningoutpost.dexdrip.RobolectricTestWithConfig
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/** Locks the iLet predicted-BG sample store used by the optional graph line. */
class IletPredictedBgTest : RobolectricTestWithConfig() {

    // The ActiveAndroid DB leaks between tests in the JVM; keep this table clean.
    @Before
    fun clear() {
        Delete().from(IletPredictedBg::class.java).execute<IletPredictedBg>()
    }

    @After
    fun clean() {
        Delete().from(IletPredictedBg::class.java).execute<IletPredictedBg>()
    }

    @Test
    fun storesAndReadsBackInOrder() {
        IletPredictedBg.store(1_000L, 120.0)
        IletPredictedBg.store(2_000L, 130.0)

        val rows = IletPredictedBg.latestForGraph(100, 0L, 10_000L)

        assertEquals(2, rows.size)
        assertEquals(120.0, rows[0].predicted, 0.001)
        assertEquals(130.0, rows[1].predicted, 0.001)
    }

    @Test
    fun rejectsOutOfRangeAndDuplicateTimestamps() {
        IletPredictedBg.store(1_000L, 120.0)
        IletPredictedBg.store(1_000L, 200.0) // duplicate timestamp is ignored
        IletPredictedBg.store(2_000L, -5.0) // out of range
        IletPredictedBg.store(3_000L, 900.0) // out of range

        val rows = IletPredictedBg.latestForGraph(100, 0L, 10_000L)

        assertEquals(1, rows.size)
        assertEquals(120.0, rows[0].predicted, 0.001)
    }

    @Test
    fun windowExcludesRowsOutsideTheRange() {
        IletPredictedBg.store(1_000L, 120.0)
        IletPredictedBg.store(5_000L, 140.0)

        val rows = IletPredictedBg.latestForGraph(100, 4_000L, 10_000L)

        assertEquals(1, rows.size)
        assertEquals(140.0, rows[0].predicted, 0.001)
    }
}
