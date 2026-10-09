package com.eveningoutpost.dexdrip.cgm.ilet

import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletCodec
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletCrc
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletHistoricalGlucoseRecord
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletProtocolError
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletSector
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.ILET_GLUCOSE_ACTIVE_MARKER
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.ILET_PUMP_EPOCH_REFERENCE
import org.junit.Test

/**
 * Sector (0x2333) decoder coverage. Builds a synthetic 4096-byte sector with a
 * glucose record and checks header CRC, walking and walk-body reconstruction
 * against [ILET_PUMP_EPOCH_REFERENCE].
 */
class IletSectorTest {

    @Test
    fun sectorHeaderAndRecordWalk() {
        val sector = ByteArray(IletSector.SECTOR_BYTES)

        // header
        System.arraycopy(IletCodec.u16(IletSector.SECTOR_MAGIC), 0, sector, 0, 2)
        System.arraycopy(IletCodec.u16(1), 0, sector, 2, 2) // version
        System.arraycopy(IletCodec.u64(1_700_000_000L), 0, sector, 4, 8) // rtc
        System.arraycopy(IletCodec.u64(1_700_007_200L), 0, sector, 12, 8) // utc
        System.arraycopy(IletCodec.u64(1_700_007_200L), 0, sector, 20, 8) // local
        System.arraycopy(IletCodec.u64(1234), 0, sector, 28, 8) // sequence
        System.arraycopy(IletCodec.u32(10), 0, sector, 60, 4) // start glucose
        System.arraycopy(IletCodec.u32(11), 0, sector, 64, 4) // end glucose
        // record header crc covers bytes 0..92 and lives at 92
        val headerCrc = IletCrc.frameCrc(sector.copyOfRange(0, 92)).toLong() and 0xFFFFFFFFL
        System.arraycopy(IletCodec.u32(headerCrc), 0, sector, 92, 4)

        // one glucose record at offset 96
        val raw = glucoseSectorRecord(seq = 10, reading = 264, predicted = 260)
        System.arraycopy(raw, 0, sector, IletSector.SECTOR_HEADER_BYTES, raw.size)
        System.arraycopy(byteArrayOf(0xEF.toByte(), 0xBE.toByte(), 0xEF.toByte(), 0xBE.toByte()), 0, sector, 4092, 4)

        val (header, records) = IletSector.parseSector(sector)
        check(header.isValid)
        check(header.glucoseRange == (10L to 11L))
        check(records.size == 1) { "one record, got ${records.size}" }
        check(records[0].sequenceNumber == 10L) { "sequence from the sector record" }
        val parsed = records[0].parsed() as IletHistoricalGlucoseRecord
        check(parsed.readingMgDl == 264) { "reading survives the walk-body rebuild, got ${parsed.readingMgDl}" }
        check(parsed.predictedReading == 260)
        check(parsed.cgmActive)
        check(parsed.hasExpectedEpoch(ILET_PUMP_EPOCH_REFERENCE))

        // A sector whose header CRC is wrong must be rejected outright.
        val broken = sector.copyOf()
        broken[92] = (broken[92].toInt() xor 0xFF).toByte()
        try {
            IletSector.parseHeader(broken)
            throw AssertionError("bad sector header CRC must be rejected")
        } catch (expected: IletProtocolError) {
            // expected
        }
    }

    /** Build a 68-byte sector record that reconstructs to a valid glucose body. */
    private fun glucoseSectorRecord(seq: Long, reading: Int, predicted: Int): ByteArray {
        val raw = ByteArray(IletSector.GLUCOSE_RECORD_BYTES)
        System.arraycopy(byteArrayOf(0xCE.toByte(), 0xFA.toByte(), 0x98.toByte(), 0x10), 0, raw, 0, 4)
        System.arraycopy(IletCodec.u16(1), 0, raw, 4, 2)
        System.arraycopy(IletCodec.u16(45), 0, raw, 6, 2)
        System.arraycopy(IletCodec.u64(1_700_000_000L), 0, raw, 8, 8)
        System.arraycopy(IletCodec.u64(1_700_000_300L), 0, raw, 16, 8)
        System.arraycopy(IletCodec.u64(0), 0, raw, 24, 8)
        System.arraycopy(IletCodec.u64(ILET_PUMP_EPOCH_REFERENCE), 0, raw, 32, 8)
        System.arraycopy(IletCodec.u32(seq), 0, raw, 40, 4)
        // byte 44 reserved
        System.arraycopy(IletCodec.i16(reading), 0, raw, 45, 2)
        System.arraycopy(IletCodec.i16(predicted), 0, raw, 47, 2)
        raw[49] = 3
        raw[50] = 2
        raw[51] = 2
        raw[52] = 2
        System.arraycopy(ILET_GLUCOSE_ACTIVE_MARKER, 0, raw, 53, 4)
        // bytes 57..60 zero
        val checksum = IletCrc.payloadCrc(raw, 0, 61).toLong() and 0xFFFFFFFFL
        System.arraycopy(IletCodec.u32(checksum), 0, raw, 61, 4)
        return raw
    }
}
