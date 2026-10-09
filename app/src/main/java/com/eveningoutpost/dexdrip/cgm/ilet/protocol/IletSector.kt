package com.eveningoutpost.dexdrip.cgm.ilet.protocol

/**
 * Decoder for the 0x2333 backfill sector. Ported from `ilet/sector.py`.
 *
 * The 0x2333 reply stream is not a sequence of records: it is a single
 * 4096-byte flash sector, chunked across four 1036-byte frames. The sector
 * holds a 96-byte header, then a packed run of glucose and insulin records,
 * then a fixed trailer (24 bytes padding, u32 CRC, `ef be ef be`).
 *
 * A sector record is a 0x2334 walk body with its 4-byte preamble stripped and
 * its trailing zero padding trimmed; [IletSectorRecord.walkBody] reconstructs
 * the 204-byte form so [parseHistoricalBody] reads it unchanged.
 */
object IletSector {

    const val SECTOR_BYTES: Int = 4096
    const val SECTOR_HEADER_BYTES: Int = 96
    const val SECTOR_MAGIC: Int = 0xCAFE

    const val GLUCOSE_RECORD_BYTES: Int = 68
    const val INSULIN_RECORD_BYTES: Int = 44

    private const val GLUCOSE_DECLARED: Int = 45
    private const val INSULIN_DECLARED: Int = 24

    private val GLUCOSE_PREAMBLE: ByteArray = byteArrayOf(
        0, 0, ((GLUCOSE_DECLARED + 20) and 0xFF).toByte(), ((GLUCOSE_DECLARED + 20) shr 8).toByte()
    )
    private val INSULIN_PREAMBLE: ByteArray = byteArrayOf(
        0, 0, ((INSULIN_DECLARED + 20) and 0xFF).toByte(), ((INSULIN_DECLARED + 20) shr 8).toByte()
    )

    private const val GLUCOSE_SEQ_OFFSET: Int = 40
    private const val INSULIN_SEQ_OFFSET: Int = 32
    private const val TRAILER_BYTES: Int = 32

    fun parseHeader(data: ByteArray): IletSectorHeader {
        val header = IletSectorHeader.parse(data)
        if (!header.isValid) {
            throw IletProtocolError(
                String.format("sector magic is 0x%04x, expected 0x%04x", header.startSignature, SECTOR_MAGIC)
            )
        }
        if (!header.hasValidCrc(data)) {
            throw IletProtocolError(
                String.format("sector header CRC mismatch: stored 0x%08X", header.recordHeaderCrc)
            )
        }
        return header
    }

    /**
     * Walk every record in a sector. Stops at the first byte that is not a
     * record magic; the trailer is left unconsumed.
     */
    fun iterRecords(data: ByteArray, header: IletSectorHeader? = null): List<IletSectorRecord> {
        if (data.size != SECTOR_BYTES) {
            throw IletProtocolError("sector is ${data.size} bytes, expected $SECTOR_BYTES")
        }
        val resolved = header ?: parseHeader(data)
        val out = ArrayList<IletSectorRecord>()
        var pos = SECTOR_HEADER_BYTES
        val limit = SECTOR_BYTES - TRAILER_BYTES
        while (pos + 4 <= limit) {
            val magic = data.copyOfRange(pos, pos + 4)
            val kind: IletHistoricalRecordType
            val size: Int
            val seqOffset: Int
            val preamble: ByteArray
            when {
                magic.contentEquals(ILET_HIST_MAGIC_GLUCOSE) -> {
                    kind = IletHistoricalRecordType.GLUCOSE
                    size = GLUCOSE_RECORD_BYTES
                    seqOffset = GLUCOSE_SEQ_OFFSET
                    preamble = GLUCOSE_PREAMBLE
                }
                magic.contentEquals(ILET_HIST_MAGIC_INSULIN) -> {
                    kind = IletHistoricalRecordType.INSULIN
                    size = INSULIN_RECORD_BYTES
                    seqOffset = INSULIN_SEQ_OFFSET
                    preamble = INSULIN_PREAMBLE
                }
                else -> return out
            }
            if (pos + size > data.size) return out
            val raw = data.copyOfRange(pos, pos + size)
            val seq = IletCodec.readU32(raw, seqOffset)
            val walk = IletCodec.concat(preamble, raw)
            val walkBody = IletCodec.fixed(walk, ILET_GLUCOSE_HIST_BYTES)
            out.add(IletSectorRecord(pos, kind, size, raw, seq, walkBody))
            pos += size
        }
        return out
    }

    /** Parse a sector header and every record; raises on a malformed record. */
    fun parseSector(data: ByteArray): Pair<IletSectorHeader, List<IletSectorRecord>> {
        val header = parseHeader(data)
        val records = iterRecords(data, header)
        for (rec in records) {
            rec.parsed()
        }
        return header to records
    }

    /** Same as [parseSector] but collects per-record errors instead of raising. */
    fun parseSectorLenient(data: ByteArray): Triple<IletSectorHeader, List<IletSectorRecord>, List<Pair<Int, String>>> {
        val header = parseHeader(data)
        val records = ArrayList<IletSectorRecord>()
        val errors = ArrayList<Pair<Int, String>>()
        for (rec in iterRecords(data, header)) {
            try {
                rec.parsed()
            } catch (e: IletProtocolError) {
                errors.add(rec.offset to e.message.orEmpty())
                continue
            }
            records.add(rec)
        }
        return Triple(header, records, errors)
    }
}

/** The 96-byte header that opens every 0x2333 sector. */
data class IletSectorHeader(
    val startSignature: Int,
    val recordHeaderVersion: Int,
    val rtcEpoch: Long,
    val utcEpoch: Long,
    val localEpoch: Long,
    val recordSequenceNumber: Long,
    val hostTimestamp: Long,
    val totalBytesWrittenInsideRecord: Long,
    val numberOfLogs: Long,
    val storageType: Long,
    val reserved3: Int,
    val rtcResetIndex: Int,
    val serialCrc: Int,
    val startGlucoseSequence: Long,
    val endGlucoseSequence: Long,
    val startInsulinSequence: Long,
    val endInsulinSequence: Long,
    val startEventSequence: Long,
    val endEventSequence: Long,
    val startReservedSequence: Long,
    val endReservedSequence: Long,
    val recordHeaderCrc: Long,
) {
    val isValid: Boolean get() = startSignature == IletSector.SECTOR_MAGIC

    val glucoseRange: Pair<Long, Long> get() = startGlucoseSequence to endGlucoseSequence
    val insulinRange: Pair<Long, Long> get() = startInsulinSequence to endInsulinSequence

    /** Check RecordHeaderCRC against frame_crc(sector[0:92]). */
    fun hasValidCrc(sector: ByteArray): Boolean =
        (IletCrc.frameCrc(sector.copyOfRange(0, 92)).toLong() and 0xFFFFFFFFL) ==
            (recordHeaderCrc and 0xFFFFFFFFL)

    companion object {
        fun parse(data: ByteArray): IletSectorHeader {
            if (data.size < IletSector.SECTOR_HEADER_BYTES) {
                throw IletProtocolError(
                    "sector header is ${data.size} bytes, need ${IletSector.SECTOR_HEADER_BYTES}"
                )
            }
            val r = IletReader(data)
            return IletSectorHeader(
                startSignature = r.u16(),
                recordHeaderVersion = r.u16(),
                rtcEpoch = r.u64(),
                utcEpoch = r.u64(),
                localEpoch = r.u64(),
                recordSequenceNumber = r.u64(),
                hostTimestamp = r.u64(),
                totalBytesWrittenInsideRecord = r.u32(),
                numberOfLogs = r.u32(),
                storageType = r.u32(),
                reserved3 = r.u8(),
                rtcResetIndex = r.u8(),
                serialCrc = r.u16(),
                startGlucoseSequence = r.u32(),
                endGlucoseSequence = r.u32(),
                startInsulinSequence = r.u32(),
                endInsulinSequence = r.u32(),
                startEventSequence = r.u32(),
                endEventSequence = r.u32(),
                startReservedSequence = r.u32(),
                endReservedSequence = r.u32(),
                recordHeaderCrc = r.u32(),
            )
        }
    }
}

/** One record inside the sector body. */
data class IletSectorRecord(
    val offset: Int,
    val kind: IletHistoricalRecordType,
    val size: Int,
    val raw: ByteArray,
    val sequenceNumber: Long,
    val walkBody: ByteArray,
) {
    /** Parse via the walk-body path. Returns null for unsupported types. */
    fun parsed(): Any? = parseHistoricalBody(kind.value, walkBody)
}
