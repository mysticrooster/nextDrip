package com.eveningoutpost.dexdrip.cgm.ilet

import com.activeandroid.query.Delete
import com.eveningoutpost.dexdrip.RobolectricTestWithConfig
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.ILET_GLUCOSE_ACTIVE_MARKER
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.ILET_GLUCOSE_HIST_BYTES
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletCodec
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletCrc
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletGetHistoricalRecordResponse
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletHistoricalGlucoseRecord
import com.eveningoutpost.dexdrip.models.BgReading
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Locks that glucose backfill keeps detecting readings when the pump clock offset is
 * non-zero: the pump-epoch field is informational (log-only, never a gate), a backfilled
 * row is stamped with the drift-corrected time, and a stale (pre-correction) row is moved
 * onto the corrected time instead of pinning the graph.
 */
class IletBackfillDetectionTest : RobolectricTestWithConfig() {

    private val clockOffset = 835_479_720L

    // ActiveAndroid keeps the DB across tests in the JVM, so a leaked reading would break
    // other tests (e.g. BestGlucoseTest expects an empty DB). Clear the readings only —
    // deleting Sensor rows trips a foreign key held by other tables.
    @Before
    fun clearReadings() {
        Delete().from(BgReading::class.java).execute<BgReading>()
    }

    @After
    fun restoreReadings() {
        Delete().from(BgReading::class.java).execute<BgReading>()
        IletPrefs.setBackfillGlucose(false)
    }

    @Test
    fun historicalGlucoseImportsWithDriftedClockAndMismatchedEpoch() {
        IletPrefs.setBackfillGlucose(true)
        try {
            val processor = IletDataProcessor()
            val nowSec = System.currentTimeMillis() / 1000L
            // Pump wall clock is 90 s ahead of true time; timezone-correct (no tz correction).
            processor.updateEpoch(clockOffset, nowSec + 90L, 0L)

            val rtc = (nowSec + 90L - clockOffset) - 300L
            val record = glucoseRecord(seq = 7, rtc = rtc, reading = 155, epoch = 424242L, active = false)

            // The embedded epoch does not match the raw GetTime offset: this must not drop it.
            assertTrue("test needs an epoch mismatch", !record.hasExpectedEpoch(processor.rawPumpOffsetSeconds))

            processor.processHistoricalGlucose(record)

            val expectedMs = (rtc + processor.epochOffsetSeconds) * 1000L
            val row = BgReading.getForPreciseTimestamp(expectedMs, 2_000L)
            assertNotNull("backfilled reading must be imported despite the epoch mismatch", row)
            assertEquals(155.0, row!!.calculated_value, 0.001)
            // Drift folded in: the row lands on true 5-minutes-ago, not 90 s in the future.
            assertTrue(
                "timestamp should be corrected, got delta ${expectedMs - (nowSec - 300L) * 1000L}",
                Math.abs(expectedMs - (nowSec - 300L) * 1000L) < 2_000L
            )
        } finally {
            IletPrefs.setBackfillGlucose(false)
        }
    }

    @Test
    fun staleIletRowIsRestampedToCorrectedTime() {
        val processor = IletDataProcessor()
        val nowSec = System.currentTimeMillis() / 1000L
        processor.updateEpoch(clockOffset, nowSec + 90L, 0L)

        val rtc = (nowSec + 90L - clockOffset) - 120L
        // A row already stored with the uncorrected (old) offset, i.e. 90 s too late.
        val oldMs = (rtc + clockOffset) * 1000L
        BgReading.bgReadingInsertFromIlet(150.0, oldMs, null, 1_000L)
        assertNotNull(BgReading.getForPreciseTimestamp(oldMs, 2_000L))

        IletPrefs.setBackfillGlucose(true)
        try {
            val record = glucoseRecord(seq = 8, rtc = rtc, reading = 150, epoch = clockOffset, active = true)
            processor.processHistoricalGlucose(record)
        } finally {
            IletPrefs.setBackfillGlucose(false)
        }

        val correctedMs = (rtc + processor.epochOffsetSeconds) * 1000L
        assertNotNull("row must now sit at the corrected time", BgReading.getForPreciseTimestamp(correctedMs, 2_000L))
        assertNull("row must no longer sit at the old offset", BgReading.getForPreciseTimestamp(oldMs, 500L))
    }

    private fun glucoseRecord(seq: Long, rtc: Long, reading: Int, epoch: Long, active: Boolean): IletHistoricalGlucoseRecord {
        val body = glucoseHistBody(seq, rtc, reading, epoch, active)
        return IletGetHistoricalRecordResponse.parse(
            IletCodec.concat(byteArrayOf(1, 1), IletCodec.u16(seq.toInt()), body)
        ).record() as IletHistoricalGlucoseRecord
    }

    private fun glucoseHistBody(seq: Long, rtc: Long, reading: Int, epoch: Long, active: Boolean): ByteArray {
        val head = IletCodec.concat(
            ByteArray(2),
            IletCodec.u16(ILET_GLUCOSE_HIST_BYTES + 20),
            hex("cefa9810"),
            IletCodec.u16(1),
            IletCodec.u16(45),
            IletCodec.u64(rtc),
            IletCodec.u64(rtc + 300L),
            IletCodec.u64(0),
            IletCodec.u64(epoch),
            IletCodec.u32(seq),
            ByteArray(1),
        )
        val readingBlock = IletCodec.concat(
            IletCodec.i16(reading),
            IletCodec.i16(reading - 3),
            byteArrayOf(3, 2, 2, 2),
        )
        val marker = if (active) ILET_GLUCOSE_ACTIVE_MARKER else ByteArray(4)
        val preChecksum = IletCodec.concat(head, readingBlock, marker, ByteArray(4))
        val checksum = IletCrc.payloadCrc(preChecksum, 4, preChecksum.size - 4)
        return IletCodec.fixed(
            IletCodec.concat(preChecksum, IletCodec.u32(checksum.toLong() and 0xFFFFFFFFL)),
            ILET_GLUCOSE_HIST_BYTES,
        )
    }

    private fun hex(value: String): ByteArray {
        val out = ByteArray(value.length / 2)
        for (i in out.indices) out[i] = value.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        return out
    }
}
