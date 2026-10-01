package com.eveningoutpost.dexdrip.cgm.ilet.protocol

/**
 * Typed iLet payloads, builders and historical parsers. Ported from
 * `ilet/messages.py`; every field offset and validation rule is preserved.
 *
 * Note the two incompatible record-type enums: [IletRecordType] for the
 * 0x2508/0x2509 family and [IletHistoricalRecordType] for 0x2333/0x2334/0x2335.
 * Mixing them returns nonsense, not errors.
 */

// ---------------------------------------------------------------- constants

const val ILET_REAL_TIME_PAYLOAD_BYTES: Int = 120
const val ILET_REALTIME_CONTROL_PAYLOAD_BYTES: Int = 216
const val ILET_DEVICE_INFO_MIN_BYTES: Int = 97
const val ILET_BACK_FILL_HISTORY_BYTES: Int = 1036
const val ILET_CONTROL_RANGE_RESPONSE_BYTES: Int = 48

const val ILET_GLUCOSE_HIST_BYTES: Int = 204
const val ILET_INSULIN_HIST_BYTES: Int = 204

val ILET_HIST_MAGIC_GLUCOSE: ByteArray = byteArrayOf(0xCE.toByte(), 0xFA.toByte(), 0x98.toByte(), 0x10)
val ILET_HIST_MAGIC_INSULIN: ByteArray = byteArrayOf(0xCE.toByte(), 0xFA.toByte(), 0x99.toByte(), 0x10)
val ILET_GLUCOSE_ACTIVE_MARKER: ByteArray = byteArrayOf(0x8B.toByte(), 0x42, 0xBC.toByte(), 0x38)

/**
 * The pump's RTC-to-Unix offset at the time the protocol was captured. Kept as
 * a test reference only: production code must derive the offset from GetTime
 * (0x2510) and treat a mismatch as "layout changed -> skip and log".
 */
const val ILET_PUMP_EPOCH_REFERENCE: Long = 835_479_720L

const val ILET_GLUCOSE_BACKFILL_BYTES: Int = 51
const val ILET_INSULIN_BACKFILL_BYTES: Int = 30
const val ILET_ALARM_BACKFILL_BYTES: Int = 38
const val ILET_ALGO_STEP_BYTES: Int = 180

private fun crcSlice(data: ByteArray, from: Int, to: Int): Int = IletCrc.payloadCrc(data, from, to - from)

private fun u32Equals(a: Long, b: Int): Boolean = (a and 0xFFFFFFFFL) == (b.toLong() and 0xFFFFFFFFL)

// ================================================================ shared types

/** 45-byte CGM reading, nested inside RealTimeData and inside reports. */
data class IletGlucoseRecord(
    val currentTimestamp: Long,
    val calculatedTimestamp: Long,
    val localOffset: Long,
    val sequenceNumber: Long,
    val cgmType: Int,
    val readingMgDl: Int,
    val predictedReading: Int,
    val cgmTrend: Int,
    val sensorStatus: Int,
    val readingStatus: Int,
    val displayStatus: Int,
    val warmupEndTime: Long,
) {
    val cgm: IletCgmType? get() = IletCgmType.fromValue(cgmType)

    companion object {
        fun parse(r: IletReader): IletGlucoseRecord = IletGlucoseRecord(
            currentTimestamp = r.u64(),
            calculatedTimestamp = r.u64(),
            localOffset = r.u64(),
            sequenceNumber = r.u32(),
            cgmType = r.u8(),
            readingMgDl = r.i16(),
            predictedReading = r.i16(),
            cgmTrend = r.u8(),
            sensorStatus = r.u8(),
            readingStatus = r.u8(),
            displayStatus = r.u8(),
            warmupEndTime = r.u64(),
        )
    }
}

/** 24-byte insulin delivery summary, nested alongside GlucoseRecord. */
data class IletInsulinRecord(
    val currentTimestamp: Long,
    val localOffset: Long,
    val sequenceNumber: Long,
    val totalDose1000: Int,
    val mealType: Int,
    val mealSize: Int,
) {
    val totalDoseUnits: Double get() = totalDose1000 / 1000.0

    companion object {
        fun parse(r: IletReader): IletInsulinRecord = IletInsulinRecord(
            currentTimestamp = r.u64(),
            localOffset = r.u64(),
            sequenceNumber = r.u32(),
            totalDose1000 = r.u16(),
            mealType = r.u8(),
            mealSize = r.u8(),
        )
    }
}

/** Nested 11-byte status blob inside BleAlgoStepPayload. */
data class IletBlePumpStatus(
    val requestTime: Long,
    val unitsRequestedX1000: Int,
    val unitsDeliveredX1000: Float,
    val available: Int,
) {
    val unitsRequested: Double get() = unitsRequestedX1000 / 1000.0
    val unitsDelivered: Double get() = unitsDeliveredX1000 / 1000.0

    companion object {
        fun parse(payload: ByteArray): IletBlePumpStatus {
            if (payload.size != 11) {
                throw IletProtocolError("pump-status payload is ${payload.size} bytes, expected 11")
            }
            val r = IletReader(payload)
            return IletBlePumpStatus(r.u32(), r.i16(), r.f32(), r.u8())
        }
    }
}

/** 32-byte partition bounds entry, returned twice by CMD_RECORD_READ_SEQ. */
data class IletRecordInfo(
    val sequenceNumber: Long,
    val rtcEpoch: Long,
    val utcEpoch: Long,
    val localEpoch: Long,
) {
    companion object {
        fun parse(r: IletReader): IletRecordInfo =
            IletRecordInfo(r.u64(), r.u64(), r.u64(), r.u64())
    }
}

// ================================================================ real-time

/** CMD stream 0x1506, 120 bytes. */
data class IletRealTimeData(
    val dataVersion: Int,
    val rtcTimestamp: Long,
    val clockOffset: Long,
    val glucose: IletGlucoseRecord,
    val insulin: IletInsulinRecord,
    val batteryPct: Int,
    val isCharging: Boolean,
    val cartridgeRemainingUl: Long,
    val isCartridgeEmpty: Boolean,
    val iob: Float,
    val bionicDisplayBgIconBitmask: Int,
    val bgRunStatus: Int,
    val bgSuspendTime: Long,
    val bgExpirationTime: Long,
    val isInsulinPaused: Boolean,
    val crc32: Long,
) {
    val isBionic: Boolean get() = bionicDisplayBgIconBitmask and 0x01 != 0
    val isDisplayBgIcon: Boolean get() = bionicDisplayBgIconBitmask and 0x02 != 0

    /** Wall-clock Unix seconds; `rtcTimestamp` + pump `clockOffset`. */
    val unixTimestamp: Long get() = rtcTimestamp + clockOffset

    val cartridgeRemainingUnits: Double get() = cartridgeRemainingUl / 10.0
    val iobUnits: Double get() = iob / 1000.0

    companion object {
        fun parse(payload: ByteArray): IletRealTimeData {
            if (payload.size != ILET_REAL_TIME_PAYLOAD_BYTES) {
                throw IletProtocolError(
                    "real-time payload is ${payload.size} bytes, expected $ILET_REAL_TIME_PAYLOAD_BYTES"
                )
            }
            val expected = IletCrc.payloadCrc(payload, 0, payload.size - 4)
            val trailing = IletCodec.readU32(payload, payload.size - 4)
            if (!u32Equals(trailing, expected)) {
                throw IletProtocolError(
                    String.format(
                        "real-time payload checksum is 0x%08X, expected 0x%08X",
                        trailing,
                        expected.toLong() and 0xFFFFFFFFL
                    )
                )
            }
            val r = IletReader(payload)
            return IletRealTimeData(
                dataVersion = r.u8(),
                rtcTimestamp = r.u64(),
                clockOffset = r.u64(),
                glucose = IletGlucoseRecord.parse(r),
                insulin = IletInsulinRecord.parse(r),
                batteryPct = r.u8(),
                isCharging = r.boolean(),
                cartridgeRemainingUl = r.u32(),
                isCartridgeEmpty = r.boolean(),
                iob = r.f32(),
                bionicDisplayBgIconBitmask = r.u8(),
                bgRunStatus = r.u8(),
                bgSuspendTime = r.u64(),
                bgExpirationTime = r.u64(),
                isInsulinPaused = r.boolean(),
                crc32 = r.u32(),
            )
        }
    }
}

/** Extended real-time stream 0x1515, 216 bytes. Superset of [IletRealTimeData]. */
data class IletRealTimeControlPumpData(
    val dataVersion: Int,
    val rtcTimestamp: Long,
    val clockOffset: Long,
    val glucose: IletGlucoseRecord,
    val insulin: IletInsulinRecord,
    val batteryPct: Int,
    val isCharging: Boolean,
    val cartridgeRemainingUl: Long,
    val isCartridgeEmpty: Boolean,
    val iob: Float,
    val bionicDisplayBgIconBitmask: Int,
    val bgRunStatus: Int,
    val bgSuspendTime: Long,
    val bgExpirationTime: Long,
    val isInsulinPaused: Boolean,
    val insulinPausedTime: Long,
    val insulinPausedEndTime: Long,
    val mealAnnouncementState: Int,
    val mealAdaptationStatusBitmask: Int,
    val cgmInfoBitmask: Int,
    val cgmSessionStartTime: Long,
    val cgmSessionEndTime: Long,
    val pumpPatchInfoSessionStartTime: Long,
    val pumpPatchInfoSessionEndTime: Long,
    val fillDetectStatus: Int,
    val silentMode: Int,
    val silentModeStartTime: Long,
    val silentModeEndTime: Long,
    val reserved: ByteArray,
    val crc32: Long,
) {
    val unixTimestamp: Long get() = rtcTimestamp + clockOffset
    val cartridgeRemainingUnits: Double get() = cartridgeRemainingUl / 10.0
    val iobUnits: Double get() = iob / 1000.0

    companion object {
        fun parse(payload: ByteArray): IletRealTimeControlPumpData {
            if (payload.size != ILET_REALTIME_CONTROL_PAYLOAD_BYTES) {
                throw IletProtocolError(
                    "control payload is ${payload.size} bytes, expected $ILET_REALTIME_CONTROL_PAYLOAD_BYTES"
                )
            }
            val expected = IletCrc.payloadCrc(payload, 0, payload.size - 4)
            val trailing = IletCodec.readU32(payload, payload.size - 4)
            if (!u32Equals(trailing, expected)) {
                throw IletProtocolError(
                    String.format(
                        "control-pump payload checksum is 0x%08X, expected 0x%08X",
                        trailing,
                        expected.toLong() and 0xFFFFFFFFL
                    )
                )
            }
            val r = IletReader(payload)
            return IletRealTimeControlPumpData(
                dataVersion = r.u8(),
                rtcTimestamp = r.u64(),
                clockOffset = r.u64(),
                glucose = IletGlucoseRecord.parse(r),
                insulin = IletInsulinRecord.parse(r),
                batteryPct = r.u8(),
                isCharging = r.boolean(),
                cartridgeRemainingUl = r.u32(),
                isCartridgeEmpty = r.boolean(),
                iob = r.f32(),
                bionicDisplayBgIconBitmask = r.u8(),
                bgRunStatus = r.u8(),
                bgSuspendTime = r.u64(),
                bgExpirationTime = r.u64(),
                isInsulinPaused = r.boolean(),
                insulinPausedTime = r.u64(),
                insulinPausedEndTime = r.u64(),
                mealAnnouncementState = r.u8(),
                mealAdaptationStatusBitmask = r.u8(),
                cgmInfoBitmask = r.u8(),
                cgmSessionStartTime = r.u64(),
                cgmSessionEndTime = r.u64(),
                pumpPatchInfoSessionStartTime = r.u64(),
                pumpPatchInfoSessionEndTime = r.u64(),
                fillDetectStatus = r.u8(),
                silentMode = r.u8(),
                silentModeStartTime = r.u64(),
                silentModeEndTime = r.u64(),
                reserved = r.raw(27),
                crc32 = r.u32(),
            )
        }
    }
}

// ================================================================ time

/** CMD_GET_TIME reply (0x2510). Three u64s, no status byte. */
data class IletGetTimeResponse(
    val localTimeEpoch: Long,
    val utcTimeEpoch: Long,
    val rtcTimeEpoch: Long,
) {
    /** RTC -> Unix conversion constant for this pump session. */
    val clockOffset: Long get() = localTimeEpoch - rtcTimeEpoch

    companion object {
        fun parse(payload: ByteArray): IletGetTimeResponse {
            val r = IletReader(payload)
            return IletGetTimeResponse(r.u64(), r.u64(), r.u64())
        }
    }
}

/** CMD_GET_RTC_TIME reply (0x260C). Single u64, no status byte. */
data class IletRtcTimeResponse(val rtcTimeEpoch: Long) {
    companion object {
        fun parse(payload: ByteArray): IletRtcTimeResponse = IletRtcTimeResponse(IletReader(payload).u64())
    }
}

/** CMD_SET_TIME reply (0x2511). One status byte, 0 == success. */
data class IletSetTimeResponse(val status: Int) {
    val isOk: Boolean get() = status == 0

    companion object {
        fun parse(payload: ByteArray): IletSetTimeResponse = IletSetTimeResponse(IletReader(payload).u8())
    }
}

// ================================================================ device info

/** Parsed CMD_GET_DEVICE_INFO reply (0x2321). */
data class IletDeviceInfoResponse(
    val deviceSerial: String,
    val modelId: String,
    val algoVersion: String,
    val hostMajor: Int,
    val hostMinor: Int,
    val hostRev: Int,
    val motorMajor: Int,
    val motorMinor: Int,
    val motorRev: Int,
    val bleMajor: Int,
    val bleMinor: Int,
    val bleRev: Int,
    val hostBlMajor: Int,
    val hostBlMinor: Int,
    val hostBlRev: Int,
    val motorBlMajor: Int,
    val motorBlMinor: Int,
    val motorBlRev: Int,
    val dfuMajor: Int,
    val dfuMinor: Int,
    val dfuRev: Int,
    val trailing: ByteArray,
) {
    val hostVersion: String get() = "$hostMajor.$hostMinor.$hostRev"
    val motorVersion: String get() = "$motorMajor.$motorMinor.$motorRev"
    val bleVersion: String get() = "$bleMajor.$bleMinor.$bleRev"
    val dfuVersion: String get() = "$dfuMajor.$dfuMinor.$dfuRev"

    companion object {
        fun parse(payload: ByteArray): IletDeviceInfoResponse {
            if (payload.size < ILET_DEVICE_INFO_MIN_BYTES) {
                throw IletProtocolError(
                    "device info is ${payload.size} bytes, need at least $ILET_DEVICE_INFO_MIN_BYTES " +
                        "to cover the named fields"
                )
            }
            val r = IletReader(payload)
            val serial = r.ascii(32)
            val model = r.ascii(32)
            val algo = r.ascii(15)
            var v = IntArray(18) { r.u8() }
            return IletDeviceInfoResponse(
                deviceSerial = serial,
                modelId = model,
                algoVersion = algo,
                hostMajor = v[0], hostMinor = v[1], hostRev = v[2],
                motorMajor = v[3], motorMinor = v[4], motorRev = v[5],
                bleMajor = v[6], bleMinor = v[7], bleRev = v[8],
                hostBlMajor = v[9], hostBlMinor = v[10], hostBlRev = v[11],
                motorBlMajor = v[12], motorBlMinor = v[13], motorBlRev = v[14],
                dfuMajor = v[15], dfuMinor = v[16], dfuRev = v[17],
                trailing = r.rest(),
            )
        }
    }
}

// ================================================================ record ops

/** Response to CMD_RECORD_FLUSH (0x2507). Status 0 means success. */
data class IletRecordFlushResponse(val status: Int) {
    val isOk: Boolean get() = status == 0

    companion object {
        fun parse(payload: ByteArray): IletRecordFlushResponse = IletRecordFlushResponse(IletReader(payload).u8())
    }
}

/** Response to CMD_RECORD_READ_SEQ (0x2508). Status 0 means success. */
data class IletRecordReadSeqResponse(
    val status: Int,
    val recordType: Int,
    val oldest: IletRecordInfo,
    val newest: IletRecordInfo,
) {
    val isOk: Boolean get() = status == 0

    companion object {
        fun parse(payload: ByteArray): IletRecordReadSeqResponse {
            val r = IletReader(payload)
            return IletRecordReadSeqResponse(r.u16(), r.u16(), IletRecordInfo.parse(r), IletRecordInfo.parse(r))
        }
    }
}

/** Response to CMD_RECORD_READ (0x2509). Header is 18 bytes on the wire. */
data class IletRecordReadResponse(
    val status: Int,
    val recordType: Int,
    val sequenceNumber: Long,
    val offset: Int,
    val numberOfBytes: Long,
    val data: ByteArray,
) {
    val isOk: Boolean get() = status == 1

    companion object {
        fun parse(payload: ByteArray): IletRecordReadResponse {
            val r = IletReader(payload)
            val status = r.u16()
            val recordType = r.u16()
            val seq = r.u64()
            val offset = r.u16()
            val numberOfBytes = r.u32()
            val rest = r.rest()
            return IletRecordReadResponse(
                status = status,
                recordType = recordType,
                sequenceNumber = seq,
                offset = offset,
                numberOfBytes = numberOfBytes,
                data = rest.copyOfRange(0, minOf(numberOfBytes, rest.size.toLong()).toInt()),
            )
        }
    }
}

/** Response to CMD_GET_SEQUENCE_NUMBER_HISTORY (0x2335). */
data class IletSequenceNumberHistoryResponse(
    val status: Int,
    val recordType: Int,
    val oldestSequenceNumber: Long,
    val newestSequenceNumber: Long,
) {
    val isOk: Boolean get() = status == 1

    companion object {
        fun parse(payload: ByteArray): IletSequenceNumberHistoryResponse {
            val r = IletReader(payload)
            return IletSequenceNumberHistoryResponse(r.u8(), r.u8(), r.u32(), r.u32())
        }
    }
}

// ================================================================ backfill

/** Response to CMD_BACK_FILL_HISTORY (0x2333), fixed 1036-byte frame. */
data class IletBackFillHistoryResponse(
    val status: Int,
    val recordType: Int,
    val sequenceNumber: Long,
    val bytesInChunk: Int,
    val bytesOffset: Int,
    val bytesTotal: Int,
    val data: ByteArray,
) {
    val isOk: Boolean get() = status == 1
    val historicalType: IletHistoricalRecordType? get() = IletHistoricalRecordType.fromValue(recordType)

    companion object {
        fun parse(payload: ByteArray): IletBackFillHistoryResponse {
            if (payload.size != ILET_BACK_FILL_HISTORY_BYTES) {
                throw IletProtocolError(
                    "backfill payload is ${payload.size} bytes, expected $ILET_BACK_FILL_HISTORY_BYTES"
                )
            }
            val r = IletReader(payload)
            val status = r.u8()
            val recordType = r.u8()
            val seq = r.u32()
            val bytesInChunk = r.u16()
            val bytesOffset = r.u16()
            val bytesTotal = r.u16()
            val raw = r.raw(1024)
            return IletBackFillHistoryResponse(
                status = status,
                recordType = recordType,
                sequenceNumber = seq,
                bytesInChunk = bytesInChunk,
                bytesOffset = bytesOffset,
                bytesTotal = bytesTotal,
                data = raw.copyOfRange(0, minOf(bytesInChunk, raw.size)),
            )
        }
    }
}

/** Response to CMD_PATCH_BACK_FILL_HISTORY (0x2380). Header is 10 bytes. */
data class IletPatchBackFillHistoryResponse(
    val status: Int,
    val recordType: Int,
    val fromSequenceNumber: Long,
    val toSequenceNumber: Long,
    val body: ByteArray,
) {
    val isOk: Boolean get() = status == 1
    val historicalType: IletHistoricalRecordType? get() = IletHistoricalRecordType.fromValue(recordType)

    /** Parse the run of fixed-size records, stopping at a mismatched type tag. */
    fun records(): List<Any> {
        val type = IletHistoricalRecordType.fromValue(recordType) ?: return emptyList()
        val size = backfillSize(type) ?: return emptyList()
        val out = ArrayList<Any>()
        var offset = 0
        while (offset + size <= body.size) {
            val chunk = body.copyOfRange(offset, offset + size)
            if (IletCodec.readU8(chunk, 0) != recordType) break
            out.add(parseBackfillRecord(recordType, chunk))
            offset += size
        }
        return out
    }

    companion object {
        fun parse(payload: ByteArray): IletPatchBackFillHistoryResponse {
            if (payload.size < 10) {
                throw IletProtocolError("patchbackfill payload is ${payload.size} bytes")
            }
            val r = IletReader(payload)
            val status = r.u8()
            val recordType = r.u8()
            val fromSeq = r.u32()
            val toSeq = r.u32()
            return IletPatchBackFillHistoryResponse(status, recordType, fromSeq, toSeq, r.rest())
        }
    }
}

/** Response to CMD_GET_HISTORICAL_RECORD (0x2334). */
data class IletGetHistoricalRecordResponse(
    val status: Int,
    val recordType: Int,
    val sequence: Int,
    val body: ByteArray,
) {
    val isOk: Boolean get() = status == 1
    val historicalType: IletHistoricalRecordType? get() = IletHistoricalRecordType.fromValue(recordType)

    /** Decode the body, or null for unsupported types / a magic mismatch. */
    fun record(): Any? = parseHistoricalBody(recordType, body)

    companion object {
        fun parse(payload: ByteArray): IletGetHistoricalRecordResponse {
            if (payload.size < 4) {
                throw IletProtocolError(
                    "get-historical-record payload is ${payload.size} bytes, need at least 4"
                )
            }
            val r = IletReader(payload)
            val status = r.u8()
            val recordType = r.u8()
            val sequence = r.u16()
            return IletGetHistoricalRecordResponse(status, recordType, sequence, r.rest())
        }
    }
}

// ---------------------------------------------------------------- 0x2334 bodies

/**
 * One glucose row from CMD_GET_HISTORICAL_RECORD (0x2334).
 *
 * Reading and prediction fields are little-endian i16 (bytes 49-50 / 51-52).
 * An earlier parser read them big-endian and mis-decoded readings above 255.
 */
data class IletHistoricalGlucoseRecord(
    val sequenceNumber: Long,
    val currentTimestamp: Long,
    val calculatedTimestamp: Long,
    val localOffset: Long,
    val pumpEpoch: Long,
    val readingMgDl: Int,
    val predictedReading: Int,
    val cgmTrend: Int,
    val sensorStatus: Int,
    val readingStatus: Int,
    val displayStatus: Int,
    val checksum: Long,
    val raw: ByteArray,
) {
    /** True when the CGM was delivering data for this slot. */
    val cgmActive: Boolean get() = raw.copyOfRange(57, 61).contentEquals(ILET_GLUCOSE_ACTIVE_MARKER)

    /** Compare the record's embedded RTC offset against the GetTime-derived value. */
    fun hasExpectedEpoch(expected: Long): Boolean = pumpEpoch == expected

    /**
     * True when this row represents a glucose value the pump trusted. False for
     * boot/boundary records, warmup slots, and readings the pump reported as
     * zero (a sensor artifact, not hypoglycemia).
     */
    fun isValidReading(expectedEpoch: Long): Boolean =
        hasExpectedEpoch(expectedEpoch) && cgmActive && readingMgDl > 0

    companion object {
        fun parse(body: ByteArray): IletHistoricalGlucoseRecord {
            if (body.size != ILET_GLUCOSE_HIST_BYTES) {
                throw IletProtocolError(
                    "glucose history body is ${body.size} bytes, expected $ILET_GLUCOSE_HIST_BYTES"
                )
            }
            if (!body.copyOfRange(4, 8).contentEquals(ILET_HIST_MAGIC_GLUCOSE)) {
                throw IletProtocolError(
                    "glucose body magic is ${body.copyOfRange(4, 8).toHexString()}, expected " +
                        "${ILET_HIST_MAGIC_GLUCOSE.toHexString()}"
                )
            }
            val r = IletReader(body, 12)
            val current = r.u64()
            val calculated = r.u64()
            val local = r.u64()
            val epoch = r.u64()
            val seq = r.u32()
            r.raw(1)
            val reading = r.i16()
            val predicted = r.i16()
            val trend = r.u8()
            val sensorStatus = r.u8()
            val readingStatus = r.u8()
            val displayStatus = r.u8()
            r.raw(4)
            r.raw(4)
            val checksum = r.u32()
            val expected = crcSlice(body, 4, 65)
            if (!u32Equals(checksum, expected)) {
                throw IletProtocolError(
                    String.format(
                        "glucose body checksum is 0x%08X, expected 0x%08X",
                        checksum,
                        expected.toLong() and 0xFFFFFFFFL
                    )
                )
            }
            return IletHistoricalGlucoseRecord(
                sequenceNumber = seq,
                currentTimestamp = current,
                calculatedTimestamp = calculated,
                localOffset = local,
                pumpEpoch = epoch,
                readingMgDl = reading,
                predictedReading = predicted,
                cgmTrend = trend,
                sensorStatus = sensorStatus,
                readingStatus = readingStatus,
                displayStatus = displayStatus,
                checksum = checksum,
                raw = body,
            )
        }
    }
}

/** One insulin row from CMD_GET_HISTORICAL_RECORD (0x2334). */
data class IletHistoricalInsulinRecord(
    val sequenceNumber: Long,
    val recordTimestampRtc: Long,
    val recordTimestampUtc: Long,
    val pumpEpoch: Long,
    val totalDose1000: Int,
    val mealType: Int,
    val mealSize: Int,
    val checksum: Long,
    val raw: ByteArray,
) {
    val totalDoseUnits: Double get() = totalDose1000 / 1000.0
    fun hasExpectedEpoch(expected: Long): Boolean = pumpEpoch == expected

    companion object {
        fun parse(body: ByteArray): IletHistoricalInsulinRecord {
            if (body.size != ILET_INSULIN_HIST_BYTES) {
                throw IletProtocolError(
                    "insulin history body is ${body.size} bytes, expected $ILET_INSULIN_HIST_BYTES"
                )
            }
            if (!body.copyOfRange(4, 8).contentEquals(ILET_HIST_MAGIC_INSULIN)) {
                throw IletProtocolError(
                    "insulin body magic is ${body.copyOfRange(4, 8).toHexString()}, expected " +
                        "${ILET_HIST_MAGIC_INSULIN.toHexString()}"
                )
            }
            val r = IletReader(body, 12)
            val rtc = r.u64()
            val utc = r.u64()
            val epoch = r.u64()
            val seq = r.u32()
            val dose = r.u16()
            val mealType = r.u8()
            val mealSize = r.u8()
            val checksum = r.u32()
            val expected = crcSlice(body, 4, 44)
            if (!u32Equals(checksum, expected)) {
                throw IletProtocolError(
                    String.format(
                        "insulin body checksum is 0x%08X, expected 0x%08X",
                        checksum,
                        expected.toLong() and 0xFFFFFFFFL
                    )
                )
            }
            return IletHistoricalInsulinRecord(
                sequenceNumber = seq,
                recordTimestampRtc = rtc,
                recordTimestampUtc = utc,
                pumpEpoch = epoch,
                totalDose1000 = dose,
                mealType = mealType,
                mealSize = mealSize,
                checksum = checksum,
                raw = body,
            )
        }
    }
}

/**
 * Dispatch a 0x2334 body to the right parser. Returns null for the two
 * historical types the APK does not parse and for a body whose magic does not
 * match the declared type.
 */
fun parseHistoricalBody(recordType: Int, body: ByteArray): Any? {
    val type = IletHistoricalRecordType.fromValue(recordType) ?: return null
    return when (type) {
        IletHistoricalRecordType.GLUCOSE -> {
            if (!body.copyOfRange(4, 8).contentEquals(ILET_HIST_MAGIC_GLUCOSE)) null
            else IletHistoricalGlucoseRecord.parse(body)
        }
        IletHistoricalRecordType.INSULIN -> {
            if (!body.copyOfRange(4, 8).contentEquals(ILET_HIST_MAGIC_INSULIN)) null
            else IletHistoricalInsulinRecord.parse(body)
        }
        else -> null
    }
}

/** Response to CMD_GET_CONTROL_SEQUENCE_NUMBER_RANGES (0x2381). */
data class IletControlSequenceNumberRangesResponse(
    val glucoseMin: Long, val glucoseMax: Long,
    val insulinMin: Long, val insulinMax: Long,
    val eventMin: Long, val eventMax: Long,
    val alarmMin: Long, val alarmMax: Long,
    val algoStepMin: Long, val algoStepMax: Long,
    val engMin: Long, val engMax: Long,
) {
    fun rangeFor(rt: IletHistoricalRecordType): Pair<Long, Long> = when (rt) {
        IletHistoricalRecordType.GLUCOSE -> glucoseMin to glucoseMax
        IletHistoricalRecordType.INSULIN -> insulinMin to insulinMax
        IletHistoricalRecordType.EVENT -> eventMin to eventMax
        IletHistoricalRecordType.ALARM -> alarmMin to alarmMax
        IletHistoricalRecordType.ALGORITHM_DATA -> algoStepMin to algoStepMax
        IletHistoricalRecordType.ENGINEERING_DATA -> engMin to engMax
    }

    companion object {
        fun parse(payload: ByteArray): IletControlSequenceNumberRangesResponse {
            if (payload.size != ILET_CONTROL_RANGE_RESPONSE_BYTES) {
                throw IletProtocolError(
                    "control-range payload is ${payload.size} bytes, expected $ILET_CONTROL_RANGE_RESPONSE_BYTES"
                )
            }
            val r = IletReader(payload)
            return IletControlSequenceNumberRangesResponse(
                glucoseMin = r.u32(), glucoseMax = r.u32(),
                insulinMin = r.u32(), insulinMax = r.u32(),
                eventMin = r.u32(), eventMax = r.u32(),
                alarmMin = r.u32(), alarmMax = r.u32(),
                algoStepMin = r.u32(), algoStepMax = r.u32(),
                engMin = r.u32(), engMax = r.u32(),
            )
        }
    }
}

// ------------------------------------------------ backfill record payloads

/** GlucoseBackFillV2Payload (record type 1), 51 bytes. */
data class IletGlucoseBackFillV2(
    val recordType: Int,
    val version: Int,
    val sequenceNumber: Long,
    val recordTimestampRtc: Long,
    val recordTimestampUtc: Long,
    val timeZoneOffset: Int,
    val length: Int,
    val calculatedTimestamp: Long,
    val cgmType: Int,
    val readingMgDl: Int,
    val predictedReading: Int,
    val cgmTrend: Int,
    val sensorStatus: Int,
    val readingStatus: Int,
    val displayStatus: Int,
    val warmUpEndTime: Long,
) {
    val cgm: IletCgmType? get() = IletCgmType.fromValue(cgmType)

    companion object {
        fun parse(payload: ByteArray): IletGlucoseBackFillV2 {
            if (payload.size != ILET_GLUCOSE_BACKFILL_BYTES) {
                throw IletProtocolError(
                    "glucose backfill is ${payload.size} bytes, expected $ILET_GLUCOSE_BACKFILL_BYTES"
                )
            }
            val r = IletReader(payload)
            return IletGlucoseBackFillV2(
                recordType = r.u8(),
                version = r.u8(),
                sequenceNumber = r.u32(),
                recordTimestampRtc = r.u64(),
                recordTimestampUtc = r.u64(),
                timeZoneOffset = r.i16(),
                length = r.u16(),
                calculatedTimestamp = r.u64(),
                cgmType = r.u8(),
                readingMgDl = r.i16(),
                predictedReading = r.i16(),
                cgmTrend = r.u8(),
                sensorStatus = r.u8(),
                readingStatus = r.u8(),
                displayStatus = r.u8(),
                warmUpEndTime = r.u64(),
            )
        }
    }
}

/** InsulinBackFillV2Payload (record type 2), 30 bytes. */
data class IletInsulinBackFillV2(
    val recordType: Int,
    val version: Int,
    val sequenceNumber: Long,
    val recordTimestampRtc: Long,
    val recordTimestampUtc: Long,
    val timeZoneOffset: Int,
    val length: Int,
    val totalDose1000: Int,
    val mealType: Int,
    val mealSize: Int,
) {
    val totalDoseUnits: Double get() = totalDose1000 / 1000.0

    companion object {
        fun parse(payload: ByteArray): IletInsulinBackFillV2 {
            if (payload.size != ILET_INSULIN_BACKFILL_BYTES) {
                throw IletProtocolError(
                    "insulin backfill is ${payload.size} bytes, expected $ILET_INSULIN_BACKFILL_BYTES"
                )
            }
            val r = IletReader(payload)
            return IletInsulinBackFillV2(
                recordType = r.u8(),
                version = r.u8(),
                sequenceNumber = r.u32(),
                recordTimestampRtc = r.u64(),
                recordTimestampUtc = r.u64(),
                timeZoneOffset = r.i16(),
                length = r.u16(),
                totalDose1000 = r.u16(),
                mealType = r.u8(),
                mealSize = r.u8(),
            )
        }
    }
}

/** BleAlarmHistoryPayload (record type 4), 38 bytes. */
data class IletAlarmHistory(
    val recordType: Int,
    val version: Int,
    val sequenceNumber: Long,
    val recordTimestampRtc: Long,
    val recordTimestampUtc: Long,
    val timeZoneOffset: Int,
    val length: Int,
    val alertEnum: Int,
    val alertLevel: Int,
    val alertTime: Long,
    val alertStatus: Int,
) {
    companion object {
        fun parse(payload: ByteArray): IletAlarmHistory {
            if (payload.size != ILET_ALARM_BACKFILL_BYTES) {
                throw IletProtocolError(
                    "alarm history is ${payload.size} bytes, expected $ILET_ALARM_BACKFILL_BYTES"
                )
            }
            val r = IletReader(payload)
            return IletAlarmHistory(
                recordType = r.u8(),
                version = r.u8(),
                sequenceNumber = r.u32(),
                recordTimestampRtc = r.u64(),
                recordTimestampUtc = r.u64(),
                timeZoneOffset = r.i16(),
                length = r.u16(),
                alertEnum = r.u16(),
                alertLevel = r.u8(),
                alertTime = r.u64(),
                alertStatus = r.u8(),
            )
        }
    }
}

/** BleAlgoStepPayload (record type 5), 180 bytes. */
data class IletAlgoStepRecord(
    val recordType: Int,
    val version: Int,
    val sequenceNumber: Long,
    val recordTimestampRtc: Long,
    val recordTimestampUtc: Long,
    val timeZoneOffset: Int,
    val length: Int,
    val algorithmExecutionTime: Long,
    val time: Long,
    val insulinPumpStatus: IletBlePumpStatus,
    val glucagonPumpStatus: IletBlePumpStatus,
    val cgmType: Int,
    val cgmValue: Int,
    val cgmValueTMinus1: Int,
    val cgmValuesRaw: List<Int>,
    val numCgmValuesRaw: Int,
    val bgValue: Int,
    val bgCal: Int,
    val olBolusX10: Int,
    val subjectWeight: Int,
    val mealTime: Int,
    val mealSizePct: Int,
    val setPoint: Int,
    val glucagonBurst: Int,
    val basalRateMultiplier: Int,
    val priming: Int,
    val insulinPaused: Boolean,
    val offsetTargets: Int,
    val gRelToLillyX100: Int,
    val userMbEntry: Int,
    val tMax: Int,
    val dynamicSp: Int,
    val gBasal: Int,
    val dType: Int,
    val pbh: Int,
    val openLoopIsf: Float,
    val openLoopBasalPerHour: Float,
    val openLoopDefaultBreakfast: Float,
    val openLoopDefaultLunch: Float,
    val openLoopDefaultDinner: Float,
    val secondsSinceLastValidCgm: Long,
    val glucagonComputedX1000: Int,
    val insulinBolusBasalComputedX1000: Int,
    val mealInsulinComputedX1000: Int,
    val glucagonDeliveredX1000: Int,
    val bolusInsulinDeliveredX1000: Int,
    val basalInsulinDeliveredX1000: Int,
    val mealInsulinDeliveredX1000: Int,
    val olCf: Int,
    val nominalBasalX10: Int,
    val instantBasalX10: Int,
    val olMbs: ByteArray,
    val ol6hBrsX10: ByteArray,
    val mbsAdaptedIndicator: ByteArray,
    val glucoseTarget: Int,
    val iob: Float,
    val algorithmWarning: Int,
    val executedIndex: Int,
    val currentStepInsulinComputedX1000: Float,
    val currentStepInsulinDeliveredX1000: Float,
    val currentStepFIobUnits: Float,
    val openLoopStatus: Int,
) {
    companion object {
        fun parse(payload: ByteArray): IletAlgoStepRecord {
            if (payload.size != ILET_ALGO_STEP_BYTES) {
                throw IletProtocolError(
                    "algo-step is ${payload.size} bytes, expected $ILET_ALGO_STEP_BYTES"
                )
            }
            val r = IletReader(payload)
            return IletAlgoStepRecord(
                recordType = r.u8(),
                version = r.u8(),
                sequenceNumber = r.u32(),
                recordTimestampRtc = r.u64(),
                recordTimestampUtc = r.u64(),
                timeZoneOffset = r.i16(),
                length = r.u16(),
                algorithmExecutionTime = r.u64(),
                time = r.u32(),
                insulinPumpStatus = IletBlePumpStatus.parse(r.raw(11)),
                glucagonPumpStatus = IletBlePumpStatus.parse(r.raw(11)),
                cgmType = r.u8(),
                cgmValue = r.i16(),
                cgmValueTMinus1 = r.i16(),
                cgmValuesRaw = List(10) { r.i16() },
                numCgmValuesRaw = r.u8(),
                bgValue = r.i16(),
                bgCal = r.i16(),
                olBolusX10 = r.i16(),
                subjectWeight = r.u8(),
                mealTime = r.u8(),
                mealSizePct = r.u8(),
                setPoint = r.u8(),
                glucagonBurst = r.u8(),
                basalRateMultiplier = r.u8(),
                priming = r.u8(),
                insulinPaused = r.boolean(),
                offsetTargets = r.u8(),
                gRelToLillyX100 = r.u8(),
                userMbEntry = r.u8(),
                tMax = r.u8(),
                dynamicSp = r.u8(),
                gBasal = r.u8(),
                dType = r.u8(),
                pbh = r.u8(),
                openLoopIsf = r.f32(),
                openLoopBasalPerHour = r.f32(),
                openLoopDefaultBreakfast = r.f32(),
                openLoopDefaultLunch = r.f32(),
                openLoopDefaultDinner = r.f32(),
                secondsSinceLastValidCgm = r.u32(),
                glucagonComputedX1000 = r.i16(),
                insulinBolusBasalComputedX1000 = r.i16(),
                mealInsulinComputedX1000 = r.i16(),
                glucagonDeliveredX1000 = r.i16(),
                bolusInsulinDeliveredX1000 = r.i16(),
                basalInsulinDeliveredX1000 = r.i16(),
                mealInsulinDeliveredX1000 = r.i16(),
                olCf = r.i16(),
                nominalBasalX10 = r.u8(),
                instantBasalX10 = r.u8(),
                olMbs = r.raw(3),
                ol6hBrsX10 = r.raw(4),
                mbsAdaptedIndicator = r.raw(3),
                glucoseTarget = r.u8(),
                iob = r.f32(),
                algorithmWarning = r.u8(),
                executedIndex = r.u8(),
                currentStepInsulinComputedX1000 = r.f32(),
                currentStepInsulinDeliveredX1000 = r.f32(),
                currentStepFIobUnits = r.f32(),
                openLoopStatus = r.u8(),
            )
        }
    }
}

private val BACKFILL_SIZES: Map<IletHistoricalRecordType, Int> = mapOf(
    IletHistoricalRecordType.GLUCOSE to ILET_GLUCOSE_BACKFILL_BYTES,
    IletHistoricalRecordType.INSULIN to ILET_INSULIN_BACKFILL_BYTES,
    IletHistoricalRecordType.ALARM to ILET_ALARM_BACKFILL_BYTES,
    IletHistoricalRecordType.ALGORITHM_DATA to ILET_ALGO_STEP_BYTES,
)

internal fun backfillSize(type: IletHistoricalRecordType): Int? = BACKFILL_SIZES[type]

/** Parse one backfill record body; raises on an unsupported type. */
fun parseBackfillRecord(recordType: Int, payload: ByteArray): Any {
    val type = IletHistoricalRecordType.fromValue(recordType)
        ?: throw IletProtocolError("unsupported backfill record type: $recordType")
    if (type !in SUPPORTED_BACKFILL_TYPES) {
        throw IletProtocolError("unsupported backfill record type: ${type.name}")
    }
    return when (type) {
        IletHistoricalRecordType.GLUCOSE -> IletGlucoseBackFillV2.parse(payload)
        IletHistoricalRecordType.INSULIN -> IletInsulinBackFillV2.parse(payload)
        IletHistoricalRecordType.ALARM -> IletAlarmHistory.parse(payload)
        IletHistoricalRecordType.ALGORITHM_DATA -> IletAlgoStepRecord.parse(payload)
        else -> throw IletProtocolError("unsupported backfill record type: ${type.name}")
    }
}

// ================================================================ events

/** Unsolicited event (0x2513) emitted between flush request and reply. */
data class IletLogFlushedEvent(
    val logPartition: Int,
    val recordSequenceNumber: Long,
    val rtcTimeEpoch: Long,
) {
    companion object {
        fun parse(payload: ByteArray): IletLogFlushedEvent {
            val r = IletReader(payload)
            return IletLogFlushedEvent(r.u8(), r.u32(), r.u64())
        }
    }
}

/** Unsolicited EVT_SET_CGM_TYPE (0x2514). */
data class IletSetCgmTypeEvent(val cgmType: Int) {
    val cgm: IletCgmType? get() = IletCgmType.fromValue(cgmType)

    companion object {
        fun parse(payload: ByteArray): IletSetCgmTypeEvent = IletSetCgmTypeEvent(IletReader(payload).u8())
    }
}

// ================================================================ builders

object IletMessages {

    fun getDeviceInfo(): IletFrame = IletFrame(IletOpcode.CMD_GET_DEVICE_INFO.value, ByteArray(0))

    fun getTime(): IletFrame = IletFrame(IletOpcode.CMD_GET_TIME.value, ByteArray(0))

    fun getRtcTime(): IletFrame = IletFrame(IletOpcode.CMD_GET_RTC_TIME.value, ByteArray(0))

    fun recordFlush(recordType: IletRecordType): IletFrame =
        IletFrame(IletOpcode.CMD_RECORD_FLUSH.value, IletCodec.u16(recordType.value))

    fun recordReadSeq(recordType: IletRecordType): IletFrame =
        IletFrame(IletOpcode.CMD_RECORD_READ_SEQ.value, IletCodec.u16(recordType.value))

    fun recordRead(
        recordType: IletRecordType,
        sequenceNumber: Long,
        offset: Int,
        numberOfBytes: Int,
        variant: Int = 0,
    ): IletFrame {
        val payload = when (variant) {
            0 -> IletCodec.concat(
                IletCodec.u16(recordType.value),
                IletCodec.u64(sequenceNumber),
                IletCodec.u16(offset),
                IletCodec.u32(numberOfBytes.toLong()),
            )
            1 -> IletCodec.concat(
                IletCodec.u16(recordType.value),
                IletCodec.u64(sequenceNumber),
                IletCodec.u16(offset),
                IletCodec.u16(numberOfBytes),
            )
            2 -> IletCodec.concat(
                IletCodec.u16(recordType.value),
                IletCodec.u64(sequenceNumber),
                IletCodec.u8(offset),
                IletCodec.u8(minOf(numberOfBytes, 255)),
            )
            3 -> IletCodec.concat(
                IletCodec.u16(recordType.value),
                IletCodec.u64(sequenceNumber),
                IletCodec.u16(numberOfBytes),
                IletCodec.u16(offset),
            )
            else -> throw IletProtocolError("unknown variant $variant")
        }
        return IletFrame(IletOpcode.CMD_RECORD_READ.value, payload)
    }

    /**
     * Build CMD_GET_SEQUENCE_NUMBER_HISTORY (0x1335). Uses the backfill
     * numbering ([IletHistoricalRecordType]); the pump reports the full sequence
     * range for the tag. The Python reference annotates this as RecordType but
     * its `walk` command passes HistoricalRecordType.
     */
    fun getSequenceNumberHistory(recordType: IletHistoricalRecordType): IletFrame =
        IletFrame(IletOpcode.CMD_GET_SEQUENCE_NUMBER_HISTORY.value, IletCodec.u8(recordType.value))

    /**
     * Build CMD_GET_HISTORICAL_RECORD (0x2334). Uses the backfill numbering
     * ([IletHistoricalRecordType]), not the 0x2508/0x2509 numbering.
     */
    fun getHistoricalRecord(recordType: IletHistoricalRecordType, sequenceNumber: Long): IletFrame =
        IletFrame(
            IletOpcode.CMD_GET_HISTORICAL_RECORD.value,
            IletCodec.concat(IletCodec.u8(recordType.value), IletCodec.u32(sequenceNumber)),
        )

    fun backFillHistory(
        recordType: IletHistoricalRecordType,
        fromSeq: Long,
        toSeq: Long,
    ): IletFrame = IletFrame(
        IletOpcode.CMD_BACK_FILL_HISTORY.value,
        IletCodec.concat(IletCodec.u8(recordType.value), IletCodec.u32(fromSeq), IletCodec.u32(toSeq)),
    )

    fun patchBackFillHistory(
        recordType: IletHistoricalRecordType,
        fromSeq: Long,
        toSeq: Long,
    ): IletFrame = IletFrame(
        IletOpcode.CMD_PATCH_BACK_FILL_HISTORY.value,
        IletCodec.concat(IletCodec.u8(recordType.value), IletCodec.u32(fromSeq), IletCodec.u32(toSeq)),
    )

    fun getControlSequenceNumberRanges(recordType: IletRecordType): IletFrame =
        IletFrame(
            IletOpcode.CMD_GET_CONTROL_SEQUENCE_NUMBER_RANGES.value,
            IletCodec.u8(recordType.value),
        )

    fun startSelfTest(): IletFrame = IletFrame(IletOpcode.CMD_START_SELF_TEST.value, ByteArray(0))

    /** Convenience alias used by tests: an i16 little-endian encoder. */
    internal fun i16Bytes(v: Int): ByteArray = IletCodec.i16(v)
}
