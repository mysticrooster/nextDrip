package com.eveningoutpost.dexdrip.cgm.ilet.protocol

import java.util.UUID

/**
 * Wire-level constants for the Beta Bionics iLet pump.
 *
 * Values are ported verbatim from the verified Python client
 * (`ilet/constants.py`) and, where noted there, cross-checked against the
 * decompiled Android pump app. This file is pure JVM: no Android imports.
 */
object IletConstants {

    // ---------------------------------------------------------------- GATT

    val SERVICE_UUID: UUID = UUID.fromString("A0090101-0605-0403-0201-F0E0D0C0B0A0")

    val CHAR_TRIGGER: UUID = UUID.fromString("A0090106-0605-0403-0201-F0E0D0C0B0A0")
    val CHAR_RESP_BODY: UUID = UUID.fromString("A0090102-0605-0403-0201-F0E0D0C0B0A0")
    val CHAR_RESP_OOB: UUID = UUID.fromString("A0090103-0605-0403-0201-F0E0D0C0B0A0")
    val CHAR_CMD_FRAME: UUID = UUID.fromString("A0090104-0605-0403-0201-F0E0D0C0B0A0")
    val CHAR_CMD_OOB: UUID = UUID.fromString("A0090105-0605-0403-0201-F0E0D0C0B0A0")
    val CHAR_REALTIME: UUID = UUID.fromString("A0090107-0605-0403-0201-F0E0D0C0B0A0")

    val SERVICE_DFU: UUID = UUID.fromString("0000FE59-0000-1000-8000-00805F9B34FB")

    // ---------------------------------------------------------------- timing

    const val TIMEOUT_DEFAULT: Long = 10_000
    const val TIMEOUT_SHORT: Long = 1_000
    const val TIMEOUT_LONG: Long = 40_000

    /** Floor between a frame write and its OOB write on Write-Without-Response. */
    const val WRITE_ORDERING_FLOOR_MS: Long = 50

    // ---------------------------------------------------------------- framing

    const val HEADER_BYTES: Int = 4
    const val MAX_CHUNK_BYTES: Int = 1024
    const val GCM_TAG_BYTES: Int = 16
    const val OOB_CRC_BYTES: Int = 4

    // ---------------------------------------------------------------- sizes

    const val IDENTITY_BYTES: Int = 64
    const val PUBLIC_KEY_BYTES: Int = 32
    const val PRIVATE_KEY_BYTES: Int = 32
    const val NONCE_BYTES: Int = 32
    const val CLOUD_SIGNATURE_BYTES: Int = 64
    const val DH_KEY_TAG_BYTES: Int = 32
    const val SESSION_KEY_BYTES: Int = 32

    val KC_LABEL_PARTY_U: ByteArray = "KC_2_U".toByteArray(Charsets.US_ASCII)
    val KC_LABEL_PARTY_V: ByteArray = "KC_2_V".toByteArray(Charsets.US_ASCII)

    /** The counter advances by two for an encrypted seal and one for cleartext. */
    const val COUNTER_STEP_ENCRYPTED: Int = 2
    const val COUNTER_STEP_CLEARTEXT: Int = 1
}

/** Wire opcodes. `CMD_*` host -> pump, `RSP_*` pump -> host, `EVT_*` unsolicited. */
enum class IletOpcode(val value: Int) {
    // secure channel handshake
    CMD_SECURE_CHANNEL_ESTABLISH_KEY(0x3006),
    RSP_SECURE_CHANNEL_ESTABLISH_KEY(0x4006),
    CMD_SECURE_CHANNEL_CONFIRM_KEY(0x3007),
    RSP_SECURE_CHANNEL_CONFIRM_KEY(0x4007),
    RSP_SECURE_CHANNEL_STATUS(0x4008),

    // info and time
    CMD_GET_DEVICE_INFO(0x1321),
    RSP_GET_DEVICE_INFO(0x2321),
    CMD_GET_TIME(0x1510),
    RSP_GET_TIME(0x2510),
    CMD_SET_TIME(0x1511),
    RSP_SET_TIME(0x2511),
    CMD_GET_RTC_TIME(0x160C),
    RSP_GET_RTC_TIME(0x260C),

    // real-time streams (arrive on CHAR_REALTIME)
    REAL_TIME_DATA(0x1506),
    REAL_TIME_CONTROL_PUMP_DATA(0x1515),

    // 0x2508 / 0x2509 partition reading (RecordType addressing)
    CMD_RECORD_FLUSH(0x1507),
    RSP_RECORD_FLUSH(0x2507),
    CMD_RECORD_READ_SEQ(0x1508),
    RSP_RECORD_READ_SEQ(0x2508),
    CMD_RECORD_READ(0x1509),
    RSP_RECORD_READ(0x2509),

    // 0x1333 / 0x1334 / 0x1335 backfill (HistoricalRecordType addressing)
    CMD_BACK_FILL_HISTORY(0x1333),
    RSP_BACK_FILL_HISTORY(0x2333),
    CMD_GET_HISTORICAL_RECORD(0x1334),
    RSP_GET_HISTORICAL_RECORD(0x2334),
    CMD_GET_SEQUENCE_NUMBER_HISTORY(0x1335),
    RSP_GET_SEQUENCE_NUMBER_HISTORY(0x2335),

    // 0x2380 / 0x2381 control-plane backfill
    CMD_PATCH_BACK_FILL_HISTORY(0x1380),
    RSP_PATCH_BACK_FILL_HISTORY(0x2380),
    CMD_GET_CONTROL_SEQUENCE_NUMBER_RANGES(0x1381),
    RSP_GET_CONTROL_SEQUENCE_NUMBER_RANGES(0x2381),

    // unsolicited events
    EVT_LOG_FLUSHED(0x2513),
    EVT_SET_CGM_TYPE(0x2514),

    // self test
    CMD_START_SELF_TEST(0x300E),
    RSP_SELF_TEST_INDICATION(0x400E),

    // errors
    RSP_INVALID_COMMAND(0x4400),
    ERROR(0xFFFF);

    companion object {
        private val byValue: Map<Int, IletOpcode> = values().associateBy { it.value }

        fun fromValue(value: Int): IletOpcode? = byValue[value]

        /** Human readable name, falling back to a hex literal for unknown opcodes. */
        fun nameOf(value: Int): String =
            byValue[value]?.name ?: String.format("0x%04X", value)
    }
}

/**
 * Opcodes whose frames are transmitted without encryption. Only the two
 * handshake commands qualify; everything else rides the AES-GCM channel.
 */
val CLEARTEXT_OPCODES: Set<IletOpcode> = setOf(
    IletOpcode.CMD_SECURE_CHANNEL_ESTABLISH_KEY,
    IletOpcode.CMD_SECURE_CHANNEL_CONFIRM_KEY,
)

/**
 * Opcodes the pump may emit without a matching request, or as a stream after
 * a single request. `RSP_BACK_FILL_HISTORY` and `RSP_PATCH_BACK_FILL_HISTORY`
 * belong here because they stream rather than answer once.
 */
val EVENT_OPCODES: Set<IletOpcode> = setOf(
    IletOpcode.EVT_LOG_FLUSHED,
    IletOpcode.EVT_SET_CGM_TYPE,
    IletOpcode.RSP_BACK_FILL_HISTORY,
    IletOpcode.RSP_PATCH_BACK_FILL_HISTORY,
    IletOpcode.RSP_SELF_TEST_INDICATION,
)

/** Streaming real-time channels, neither request-driven nor one-shot. */
val REALTIME_OPCODES: Set<IletOpcode> = setOf(
    IletOpcode.REAL_TIME_DATA,
    IletOpcode.REAL_TIME_CONTROL_PUMP_DATA,
)

/**
 * Opcodes for which the pump never sends a reply. Kept empty deliberately:
 * 0x2509 is unsupported on the sampled firmware, not merely response-less.
 */
val NO_RESPONSE_OPCODES: Set<IletOpcode> = emptySet()

/**
 * Partition identifiers for the 0x2508 / 0x2509 command family. Do not confuse
 * with [IletHistoricalRecordType], which shares the number 4 with REPORT but
 * means ALARM.
 */
enum class IletRecordType(val value: Int) {
    ENGINEERING(1),
    CLINICAL(2),
    MOTOR(3),
    REPORT(4);

    companion object {
        fun fromValue(value: Int): IletRecordType? = values().firstOrNull { it.value == value }
    }
}

/**
 * Record-type tags for the 0x1333 / 0x1380 backfill family. Different numbering
 * from [IletRecordType]; mixing them returns nonsense, not errors.
 */
enum class IletHistoricalRecordType(val value: Int) {
    GLUCOSE(1),
    INSULIN(2),
    EVENT(3), // unsupported: no APK parser
    ALARM(4),
    ALGORITHM_DATA(5),
    ENGINEERING_DATA(6); // unsupported: no APK parser

    companion object {
        fun fromValue(value: Int): IletHistoricalRecordType? =
            values().firstOrNull { it.value == value }
    }
}

/** Types this client can parse. Matches the supported arms of the APK switch. */
val SUPPORTED_BACKFILL_TYPES: Set<IletHistoricalRecordType> = setOf(
    IletHistoricalRecordType.GLUCOSE,
    IletHistoricalRecordType.INSULIN,
    IletHistoricalRecordType.ALARM,
    IletHistoricalRecordType.ALGORITHM_DATA,
)

enum class IletRecordStatus(val value: Int) {
    SUCCESS(0),
    FLUSH_FAILED(1),
    READ_SEQ_FAILED(2),
    READ_FAILED(3);

    companion object {
        fun fromValue(value: Int): IletRecordStatus? = values().firstOrNull { it.value == value }
    }
}

enum class IletSecureChannelStatus(val value: Int) {
    SECURED(0),
    FAILURE_INVALID_NONCE(1),
    FAILURE_INVALID_PUBLIC_KEY(2),
    FAILURE_INVALID_IDENTITY(3),
    FAILURE_INVALID_TAG(4),
    FAILURE_MISSING_SYMMETRIC_KEY(5),
    FAILURE_DECRYPTION_ERROR(6),
    FAILURE_TIMEOUT(7),
    FAILURE_PROTOCOL_VIOLATION(8);

    companion object {
        fun fromValue(value: Int): IletSecureChannelStatus? =
            values().firstOrNull { it.value == value }

        fun nameOf(value: Int): String =
            fromValue(value)?.name ?: String.format("unknown status 0x%02X", value)
    }
}

enum class IletCgmType(val value: Int) {
    UNKNOWN(0),
    ABBOTT(1),
    DEXCOM_G6(2),
    SENSEONICS_EVERSENSE(3),
    DEXCOM_G7(4);

    companion object {
        fun fromValue(value: Int): IletCgmType? = values().firstOrNull { it.value == value }
    }
}
