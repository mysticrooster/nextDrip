package com.eveningoutpost.dexdrip.cgm.ilet

import com.eveningoutpost.dexdrip.cgm.ilet.protocol.CLEARTEXT_OPCODES
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.EVENT_OPCODES
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletBlePacket
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletChannelIdentity
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletChecksumError
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletCodec
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletCrc
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletFrame
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletKeyConfirmationError
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletMessages
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletOpcode
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletPeerIdentity
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletProtocolError
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletRecordType
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletSecureChannel
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletHistoricalRecordType
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.ILET_ALARM_BACKFILL_BYTES
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.ILET_ALGO_STEP_BYTES
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.ILET_CONTROL_RANGE_RESPONSE_BYTES
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.ILET_DEVICE_INFO_MIN_BYTES
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.ILET_GLUCOSE_ACTIVE_MARKER
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.ILET_GLUCOSE_BACKFILL_BYTES
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.ILET_GLUCOSE_HIST_BYTES
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.ILET_INSULIN_BACKFILL_BYTES
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.ILET_PUMP_EPOCH_REFERENCE
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.ILET_REAL_TIME_PAYLOAD_BYTES
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletAlarmHistory
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletAlgoStepRecord
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletBackFillHistoryResponse
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletBlePumpStatus
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletControlSequenceNumberRangesResponse
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletDeviceInfoResponse
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletGetHistoricalRecordResponse
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletGetTimeResponse
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletGlucoseBackFillV2
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletHistoricalGlucoseRecord
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletInsulinBackFillV2
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletPatchBackFillHistoryResponse
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletRealTimeData
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletRecordReadResponse
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletSetCgmTypeEvent
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.REALTIME_OPCODES
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.parseHistoricalBody
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.splitRealtimeNotification
import org.bouncycastle.crypto.agreement.X25519Agreement
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.generators.HKDFBytesGenerator
import org.bouncycastle.crypto.generators.X25519KeyPairGenerator
import org.bouncycastle.crypto.params.HKDFParameters
import org.bouncycastle.crypto.params.X25519KeyGenerationParameters
import org.bouncycastle.crypto.params.X25519PrivateKeyParameters
import org.bouncycastle.crypto.params.X25519PublicKeyParameters
import org.junit.Test
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Ported from the Python client's `test_offline.py`. These are the authoritative
 * offline vectors: captured CRC pairs, the establish-key peer layout, the
 * the synthetic 180-byte algo record and every historical-body guard.
 */
class IletProtocolTest {

    // ================================================================ framing

    @Test
    fun framing() {
        val raw = IletFrame(IletOpcode.CMD_GET_TIME.value, ByteArray(0)).toBytes()
        check(raw.size == 4) { "empty payload frames to 4 bytes, got ${raw.size}" }
        check(IletCodec.readU16(raw, 2) == 4) { "length field counts the header" }
        check(IletFrame.fromBytes(raw) == IletFrame(IletOpcode.CMD_GET_TIME.value, ByteArray(0))) {
            "round trip"
        }

        val payload = ByteArray(16) { it.toByte() }
        val withPayload = IletFrame(IletOpcode.CMD_RECORD_READ.value, payload).toBytes()
        check(IletCodec.readU16(withPayload, 2) == 20) { "length field includes the payload" }

        val packet = IletBlePacket.cleartext(IletFrame(IletOpcode.CMD_GET_TIME.value, ByteArray(0)))
        check(packet.oob.size == 4) { "cleartext OOB is 4 bytes" }
        check(!packet.isEncrypted) { "cleartext is not flagged encrypted" }
        packet.verifyCrc()

        val tampered = IletBlePacket(packet.frame, ByteArray(4))
        try {
            tampered.verifyCrc()
            throw AssertionError("a bad CRC must be rejected")
        } catch (expected: IletChecksumError) {
            // expected
        }
    }

    @Test
    fun recordReadRequestLayout() {
        val frame = IletMessages.recordRead(IletRecordType.REPORT, 0x1122334455667788L, 0x0040, 1024)
        val expected = IletCodec.concat(
            IletCodec.u16(4),
            IletCodec.u64(0x1122334455667788L),
            IletCodec.u16(0x40),
            IletCodec.u32(1024),
        )
        check(frame.payload.size == 16) { "payload is 16 bytes, got ${frame.payload.size}" }
        check(frame.payload.contentEquals(expected)) { "field order and widths" }
    }

    @Test
    fun recordReadResponseLayout() {
        val body = ByteArray(1024)
        val payload = IletCodec.concat(
            IletCodec.u16(1), IletCodec.u16(1), IletCodec.u64(100), IletCodec.u16(0),
            IletCodec.u32(1024), body,
        )
        check(payload.size == 18 + 1024) { "full frame is 18 + 1024, got ${payload.size}" }
        val r = IletRecordReadResponse.parse(payload)
        check(r.status == 1)
        check(r.isOk)
        check(r.recordType == 1)
        check(r.sequenceNumber == 100L)
        check(r.offset == 0)
        check(r.numberOfBytes == 1024L)
        check(r.data.size == 1024)
    }

    // ================================================================ CRC

    @Test
    fun crcAgainstCapturedTraffic() {
        val vectors = listOf(
            "07400d00000000000000000000" to "d49c88e8",
            "08400d0007ffffffffffffffff" to "f2ef9358",
        )
        for ((frameHex, oobHex) in vectors) {
            val frame = hex(frameHex)
            val want = IletCodec.readU32(hex(oobHex), 0)
            val got = IletCrc.frameCrc(frame).toLong() and 0xFFFFFFFFL
            check(got == want) {
                String.format("0x%04X frame CRC expected 0x%08X got 0x%08X", IletCodec.readU16(frame), want, got)
            }
        }
    }

    // ================================================================ realtime

    @Test
    fun realtimeSplit() {
        val payload = ByteArray(ILET_REAL_TIME_PAYLOAD_BYTES)
        val framed = IletCodec.concat(
            IletCodec.u16(IletOpcode.REAL_TIME_DATA.value),
            IletCodec.u16(4 + payload.size),
            payload,
            ByteArray(4),
        )
        val framedPacket = splitRealtimeNotification(framed)
        check(framedPacket.frame.size == 4 + payload.size) { "framed form keeps its payload" }

        val packet = splitRealtimeNotification(payload)
        check(packet.opcode == IletOpcode.REAL_TIME_DATA.value) { "headerless form gets a header" }
        check(IletCodec.readU8(packet.frame, 2) == payload.size + 4 && IletCodec.readU8(packet.frame, 3) == 0) {
            "synthesised length is payload + 4"
        }
    }

    @Test
    fun realtimeParse() {
        val parts = IletCodec.concat(
            IletCodec.u8(1),
            IletCodec.u64(1_700_000_000L),
            IletCodec.u64(3600),
            IletCodec.concat(
                IletCodec.u64(111), IletCodec.u64(222), IletCodec.u64(333), IletCodec.u32(444),
                IletCodec.u8(4), IletCodec.i16(120), IletCodec.i16(125), IletCodec.u8(1),
                IletCodec.u8(2), IletCodec.u8(3), IletCodec.u8(4), IletCodec.u64(555),
            ),
            IletCodec.concat(
                IletCodec.u64(666), IletCodec.u64(777), IletCodec.u32(888), IletCodec.u16(2500),
                IletCodec.u8(1), IletCodec.u8(2),
            ),
            IletCodec.u8(87),
            IletCodec.u8(1),
            IletCodec.u32(1800),
            IletCodec.u8(0),
            IletCodec.f32(3592.53f),
            IletCodec.u8(1),
            IletCodec.u8(2),
            IletCodec.u64(0),
            IletCodec.u64(0),
            IletCodec.u8(0),
        )
        check(parts.size == ILET_REAL_TIME_PAYLOAD_BYTES - 4) { "body is ${ILET_REAL_TIME_PAYLOAD_BYTES - 4} bytes" }
        val crc = IletCrc.payloadCrc(parts, 0, parts.size)
        val payload = IletCodec.concat(parts, IletCodec.u32(crc.toLong() and 0xFFFFFFFFL))
        check(payload.size == ILET_REAL_TIME_PAYLOAD_BYTES)

        val s = IletRealTimeData.parse(payload)
        check(s.glucose.readingMgDl == 120)
        check(s.glucose.predictedReading == 125)
        check(s.glucose.cgm!!.value == 4)
        check(Math.abs(s.insulin.totalDoseUnits - 2.5) < 1e-9)
        check(s.batteryPct == 87)
        check(s.isCharging)
        check(Math.abs(s.cartridgeRemainingUnits - 180.0) < 1e-9)
        check(Math.abs(s.iobUnits - 3.59253) < 1e-5)
        check(!s.isInsulinPaused)
        check(s.crc32 == (crc.toLong() and 0xFFFFFFFFL))
    }

    @Test
    fun realtimeRejectsBadCrc() {
        val head = IletCodec.concat(IletCodec.u8(1), IletCodec.u64(0), IletCodec.u64(0))
        val body = ByteArray(ILET_REAL_TIME_PAYLOAD_BYTES - 4)
        System.arraycopy(head, 0, body, 0, head.size)
        val crc = IletCrc.payloadCrc(body, 0, body.size)
        val payload = IletCodec.concat(body, IletCodec.u32(crc.toLong() and 0xFFFFFFFFL))
        payload[10] = (payload[10].toInt() xor 0x01).toByte()
        try {
            IletRealTimeData.parse(payload)
            throw AssertionError("bad CRC must be rejected")
        } catch (expected: IletProtocolError) {
            // expected
        }
    }

    // ================================================================ secure channel

    @Test
    fun secureChannel() {
        val random = SecureRandom()
        val pumpGenerator = X25519KeyPairGenerator()
        pumpGenerator.init(X25519KeyGenerationParameters(random))
        val pumpPair = pumpGenerator.generateKeyPair()
        val pumpPrivate = pumpPair.private as X25519PrivateKeyParameters
        val pumpPublic = (pumpPair.public as X25519PublicKeyParameters).encoded
        val pumpNonce = ByteArray(32).also { random.nextBytes(it) }
        val pumpIdentity = UUID.randomUUID().toString()

        val ident = IletChannelIdentity.generate(cloudSignature = ByteArray(64))
        val channel = IletSecureChannel(ident)

        val establish = channel.establishKeyFrame()
        check(establish.payload.size == 192) { "establish payload is 192 bytes, got ${establish.payload.size}" }
        check(
            establish.payload.copyOfRange(64 + 32, 64 + 96).contentEquals(ByteArray(64))
        ) { "cloud signature precedes the nonce on the wire" }

        val agreement = X25519Agreement()
        agreement.init(pumpPrivate)
        val shared = ByteArray(agreement.agreementSize)
        agreement.calculateAgreement(X25519PublicKeyParameters(ident.publicKey, 0), shared, 0)
        val hkdf = HKDFBytesGenerator(SHA256Digest())
        hkdf.init(HKDFParameters(shared, ident.nonce, ByteArray(0)))
        val sessionKey = ByteArray(32)
        hkdf.generateBytes(sessionKey, 0, 32)

        val tagV = hmac(
            sessionKey,
            concat(
                "KC_2_V".toByteArray(),
                pumpIdentity.toByteArray(),
                ident.identity.toByteArray(),
                pumpNonce,
                ident.nonce,
            ),
        )
        val peer = IletPeerIdentity(pumpIdentity, pumpPublic, pumpNonce, tagV)
        channel.derive(peer)
        check(channel.isEstablished)

        val confirm = channel.confirmKeyFrame()
        val expectedU = hmac(
            sessionKey,
            concat(
                "KC_2_U".toByteArray(),
                ident.identity.toByteArray(),
                pumpIdentity.toByteArray(),
                ident.nonce,
                pumpNonce,
            ),
        )
        check(confirm.payload.contentEquals(expectedU)) { "KC_2_U tag matches" }

        val bad = IletSecureChannel(ident)
        try {
            bad.derive(IletPeerIdentity(pumpIdentity, pumpPublic, pumpNonce, tagV.copyOf().also { it[31] = 1 }))
            throw AssertionError("a wrong KC_2_V tag must be refused")
        } catch (expected: IletKeyConfirmationError) {
            // expected
        }

        channel.adoptCounter(7)
        val frame = IletFrame(IletOpcode.CMD_GET_TIME.value, ByteArray(0))
        val sealed = channel.seal(frame)
        check(sealed.oob.size == 20) { "encrypted OOB is 20 bytes, got ${sealed.oob.size}" }
        check(sealed.oob.copyOfRange(0, 4).contentEquals(IletCodec.u32(7))) { "counter is little-endian in the OOB" }
        check(channel.counter == 9) { "counter advanced by 2, got ${channel.counter}" }
        check(sealed.header.contentEquals(frame.toBytes().copyOfRange(0, 4))) { "header stays in the clear" }

        val peerChannel = IletSecureChannel(ident)
        peerChannel.derive(peer)
        val opened = peerChannel.open(sealed)
        check(opened == frame) { "round trip" }

        val broken = IletBlePacket(byteArrayOf(0xFF.toByte()) + sealed.frame.copyOfRange(1, sealed.frame.size), sealed.oob)
        try {
            peerChannel.open(broken)
            throw AssertionError("header tampering must be detected")
        } catch (expected: Exception) {
            // expected
        }
    }

    @Test
    fun peerIdentityFromCapture() {
        // The identity is a padded 64-byte ASCII field; a synthetic value exercises
        // the offset and trailing-NUL trimming without embedding a real pump serial.
        val identityText = "ILET001"
        val identity = identityText.toByteArray().copyOf(64)
        val pubkey = hex("57c69b2502f53e1ab1e16fad6329af34acfd627fbf5b48411ccdafa8915f5e78")
        val nonce = hex("4c79a9cd0d8eccfde98d25825b2eecdfa15f98e448f6ba0beccb76b96c347fdb")
        val tag = hex("04fda68764c8d9aa16e052babaac085c10f042d56b90c782b9e0142b014bf450")
        val payload = concat(identity, pubkey, nonce, tag)
        check(payload.size == 160) { "peer payload is 160 bytes, got ${payload.size}" }

        val peer = IletPeerIdentity.parse(payload)
        check(peer.identity == identityText) { "capture peer identity" }
        check(peer.publicKey.contentEquals(pubkey))
        check(peer.nonce.contentEquals(nonce))
        check(peer.dhKeyTag.contentEquals(tag))
        peer.validate()
    }

    // ================================================================ info / time

    @Test
    fun deviceInfoLayout() {
        val serial = "BIONIC-0042".toByteArray().copyOf(32)
        val model = "iLet-2024".toByteArray().copyOf(32)
        val algo = "v1.4.7".toByteArray().copyOf(15)
        val versions = ByteArray(18) { (it + 1).toByte() }
        val prefix = concat(serial, model, algo, versions)
        check(prefix.size == ILET_DEVICE_INFO_MIN_BYTES) { "prefix is the minimum size" }

        val info = IletDeviceInfoResponse.parse(prefix)
        check(info.deviceSerial == "BIONIC-0042")
        check(info.modelId == "iLet-2024")
        check(info.algoVersion == "v1.4.7")
        check(info.hostVersion == "1.2.3")
        check(info.motorVersion == "4.5.6")
        check(info.bleVersion == "7.8.9")
        check(info.dfuVersion == "16.17.18")
        check(info.trailing.isEmpty()) { "no trailing bytes in the prefix payload" }

        val tail = ByteArray(76) { it.toByte() }
        val info2 = IletDeviceInfoResponse.parse(prefix + tail)
        check(info2.trailing.contentEquals(tail)) { "trailing captured verbatim" }
        check(info2.hostVersion == "1.2.3")

        try {
            IletDeviceInfoResponse.parse(prefix.copyOf(prefix.size - 1))
            throw AssertionError("truncated prefix must be rejected")
        } catch (expected: IletProtocolError) {
            // expected
        }
    }

    @Test
    fun getTimeResponse() {
        val payload = concat(IletCodec.u64(1_700_002_000L), IletCodec.u64(1_700_000_000L), IletCodec.u64(900_000_000L))
        val r = IletGetTimeResponse.parse(payload)
        check(r.localTimeEpoch == 1_700_002_000L)
        check(r.rtcTimeEpoch == 900_000_000L)
        check(r.clockOffset == 1_700_002_000L - 900_000_000L)
    }

    @Test
    fun setCgmTypeEvent() {
        val evt = IletSetCgmTypeEvent.parse(byteArrayOf(4))
        check(evt.cgm!!.value == 4)
        val unknown = IletSetCgmTypeEvent.parse(byteArrayOf(0xFE.toByte()))
        check(unknown.cgm == null && unknown.cgmType == 0xFE) { "unknown passthrough" }
    }

    @Test
    fun opcodeAndEventClassification() {
        check(IletOpcode.CMD_SET_TIME.value == 0x1511)
        check(IletOpcode.CMD_GET_RTC_TIME.value == 0x160C)
        check(IletOpcode.REAL_TIME_CONTROL_PUMP_DATA.value == 0x1515)
        check(IletOpcode.EVT_SET_CGM_TYPE.value == 0x2514)
        check(IletOpcode.CMD_PATCH_BACK_FILL_HISTORY.value == 0x1380)
        check(IletOpcode.RSP_GET_CONTROL_SEQUENCE_NUMBER_RANGES.value == 0x2381)

        check(IletOpcode.RSP_BACK_FILL_HISTORY in EVENT_OPCODES)
        check(IletOpcode.RSP_PATCH_BACK_FILL_HISTORY in EVENT_OPCODES)
        check(IletOpcode.EVT_SET_CGM_TYPE in EVENT_OPCODES)
        check(IletOpcode.RSP_RECORD_READ !in EVENT_OPCODES)
        check(IletOpcode.REAL_TIME_DATA in REALTIME_OPCODES)
        check(IletOpcode.REAL_TIME_CONTROL_PUMP_DATA in REALTIME_OPCODES)
        check(IletOpcode.CMD_SECURE_CHANNEL_ESTABLISH_KEY in CLEARTEXT_OPCODES)
        check(IletOpcode.CMD_SECURE_CHANNEL_CONFIRM_KEY in CLEARTEXT_OPCODES)
        check(IletOpcode.CMD_GET_TIME !in CLEARTEXT_OPCODES)
    }

    @Test
    fun recordTypeEnumsAreSeparate() {
        check(IletRecordType.REPORT.value == 4)
        check(IletHistoricalRecordType.ALARM.value == 4)
        check(IletHistoricalRecordType.GLUCOSE.value == 1)
        check(IletRecordType.ENGINEERING.value == 1)
        check(IletRecordType.fromValue(5) == null)
        check(IletHistoricalRecordType.fromValue(5) == IletHistoricalRecordType.ALGORITHM_DATA)
    }

    @Test
    fun sequenceNumberHistoryUsesHistoricalNumbering() {
        // 0x2335 addresses partitions by HistoricalRecordType, not RecordType.
        check(IletMessages.getSequenceNumberHistory(IletHistoricalRecordType.GLUCOSE).payload.contentEquals(byteArrayOf(1)))
        check(IletMessages.getSequenceNumberHistory(IletHistoricalRecordType.INSULIN).payload.contentEquals(byteArrayOf(2)))
        check(IletMessages.getSequenceNumberHistory(IletHistoricalRecordType.ALARM).payload.contentEquals(byteArrayOf(4)))
    }

    // ================================================================ control ranges

    @Test
    fun controlRangesLayout() {
        val values = longArrayOf(10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120)
        val payload = IletCodec.concat(*values.map { IletCodec.u32(it) }.toTypedArray())
        check(payload.size == ILET_CONTROL_RANGE_RESPONSE_BYTES)
        val r = IletControlSequenceNumberRangesResponse.parse(payload)
        check(r.glucoseMin == 10L && r.glucoseMax == 20L)
        check(r.insulinMin == 30L && r.insulinMax == 40L)
        check(r.eventMin == 50L && r.eventMax == 60L)
        check(r.alarmMin == 70L && r.alarmMax == 80L)
        check(r.algoStepMin == 90L && r.algoStepMax == 100L)
        check(r.engMin == 110L && r.engMax == 120L)
    }

    // ================================================================ backfill records

    @Test
    fun glucoseBackfillLayout() {
        val payload = concat(
            byteArrayOf(1, 3),
            IletCodec.u32(42),
            IletCodec.u64(1_700_000_000L),
            IletCodec.u64(1_700_007_200L),
            IletCodec.i16(-360),
            IletCodec.u16(ILET_GLUCOSE_BACKFILL_BYTES),
            IletCodec.u64(1_700_007_000L),
            byteArrayOf(4),
            IletCodec.i16(120),
            IletCodec.i16(125),
            byteArrayOf(1, 2, 3, 4),
            IletCodec.u64(1_700_010_000L),
        )
        check(payload.size == ILET_GLUCOSE_BACKFILL_BYTES) { "payload is 51 bytes, got ${payload.size}" }
        val g = IletGlucoseBackFillV2.parse(payload)
        check(g.recordType == 1)
        check(g.sequenceNumber == 42L)
        check(g.recordTimestampRtc == 1_700_000_000L)
        check(g.timeZoneOffset == -360)
        check(g.cgmType == 4)
        check(g.cgm!!.value == 4)
        check(g.readingMgDl == 120)
        check(g.predictedReading == 125)
        check(g.warmUpEndTime == 1_700_010_000L)
    }

    @Test
    fun insulinBackfillLayout() {
        val payload = concat(
            byteArrayOf(2, 3),
            IletCodec.u32(7),
            IletCodec.u64(1_700_000_000L),
            IletCodec.u64(1_700_007_200L),
            IletCodec.i16(-360),
            IletCodec.u16(ILET_INSULIN_BACKFILL_BYTES),
            IletCodec.u16(2500),
            byteArrayOf(1, 2),
        )
        check(payload.size == ILET_INSULIN_BACKFILL_BYTES)
        val i = IletInsulinBackFillV2.parse(payload)
        check(i.recordType == 2)
        check(Math.abs(i.totalDoseUnits - 2.5) < 1e-9)
        check(i.mealType == 1 && i.mealSize == 2)
    }

    @Test
    fun alarmHistoryLayout() {
        val payload = concat(
            byteArrayOf(4, 1),
            IletCodec.u32(99),
            IletCodec.u64(1_700_000_000L),
            IletCodec.u64(1_700_007_200L),
            IletCodec.i16(-360),
            IletCodec.u16(ILET_ALARM_BACKFILL_BYTES),
            IletCodec.u16(0x0102),
            byteArrayOf(7),
            IletCodec.u64(1_700_009_999L),
            byteArrayOf(0),
        )
        check(payload.size == ILET_ALARM_BACKFILL_BYTES)
        val a = IletAlarmHistory.parse(payload)
        check(a.recordType == 4)
        check(a.alertEnum == 0x0102)
        check(a.alertLevel == 7)
        check(a.alertTime == 1_700_009_999L)
    }

    @Test
    fun patchBackfillRecords() {
        val header = concat(byteArrayOf(1, 1), IletCodec.u32(100), IletCodec.u32(102))
        val body = concat(gRecord(100), gRecord(101), gRecord(102))
        val r = IletPatchBackFillHistoryResponse.parse(header + body)
        check(r.status == 1)
        check(r.historicalType == IletHistoricalRecordType.GLUCOSE)
        check(r.fromSequenceNumber == 100L && r.toSequenceNumber == 102L)
        val records = r.records()
        check(records.size == 3) { "three records parsed" }
        check((records[0] as IletGlucoseBackFillV2).sequenceNumber == 100L)
        check((records[2] as IletGlucoseBackFillV2).sequenceNumber == 102L)
    }

    @Test
    fun patchBackfillStopsOnTypeMismatch() {
        val header = concat(byteArrayOf(1, 1), IletCodec.u32(0), IletCodec.u32(2))
        val body = concat(gRecord(0, tag = 1), gRecord(1, tag = 99))
        val records = IletPatchBackFillHistoryResponse.parse(header + body).records()
        check(records.size == 1) { "run stops at mismatched tag, got ${records.size}" }
    }

    // ================================================================ 0x2334 bodies

    @Test
    fun historicalGlucoseLayout() {
        val body = glucoseHistBody(42, 120, 125)
        check(body.size == ILET_GLUCOSE_HIST_BYTES)
        val payload = concat(byteArrayOf(1, 1), IletCodec.u16(42), body)
        val r = IletGetHistoricalRecordResponse.parse(payload)
        check(r.status == 1 && r.isOk)
        check(r.historicalType == IletHistoricalRecordType.GLUCOSE)
        check(r.sequence == 42)
        check(r.body.size == ILET_GLUCOSE_HIST_BYTES)

        val rec = r.record() as IletHistoricalGlucoseRecord
        check(rec.sequenceNumber == 42L)
        check(rec.readingMgDl == 120)
        check(rec.predictedReading == 125)
        check(rec.cgmActive)
        check(rec.hasExpectedEpoch(ILET_PUMP_EPOCH_REFERENCE))
    }

    @Test
    fun historicalGlucoseReadingAbove255() {
        val body = glucoseHistBody(7, 312, 305)
        val rec = IletGetHistoricalRecordResponse.parse(concat(byteArrayOf(1, 1), IletCodec.u16(7), body))
            .record() as IletHistoricalGlucoseRecord
        check(rec.readingMgDl == 312) { "reading carries the high byte, got ${rec.readingMgDl}" }
        check(rec.predictedReading == 305) { "prediction carries the high byte, got ${rec.predictedReading}" }
    }

    @Test
    fun historicalRecordGuards() {
        try {
            IletGetHistoricalRecordResponse.parse(byteArrayOf(1, 1))
            throw AssertionError("short envelope must be rejected")
        } catch (expected: IletProtocolError) {
            // expected
        }

        val badMagic = ByteArray(ILET_GLUCOSE_HIST_BYTES)
        System.arraycopy(IletCodec.u16(ILET_GLUCOSE_HIST_BYTES + 20), 0, badMagic, 2, 2)
        System.arraycopy(hex("deadbeef"), 0, badMagic, 4, 4)
        val r = IletGetHistoricalRecordResponse.parse(concat(byteArrayOf(1, 1), IletCodec.u16(1), badMagic))
        check(r.record() == null) { "record() returns null on magic mismatch" }

        val warmup = glucoseHistBody(6, -1, -1, active = false)
        val warm = IletGetHistoricalRecordResponse.parse(concat(byteArrayOf(1, 1), IletCodec.u16(6), warmup))
            .record() as IletHistoricalGlucoseRecord
        check(warm.readingMgDl == -1)
        check(!warm.cgmActive) { "warmup has no live marker" }

        val corrupted = glucoseHistBody(42, 120, 125)
        corrupted[65] = (corrupted[65].toInt() xor 0xFF).toByte()
        val bad = IletGetHistoricalRecordResponse.parse(concat(byteArrayOf(1, 1), IletCodec.u16(42), corrupted))
        try {
            bad.record()
            throw AssertionError("bad checksum must be rejected")
        } catch (expected: IletProtocolError) {
            // expected
        }
    }

    @Test
    fun insulinHistoricalBody() {
        val pre = concat(
            ByteArray(2),
            IletCodec.u16(ILET_GLUCOSE_HIST_BYTES + 20),
            hex("cefa9910"),
            IletCodec.u16(1),
            IletCodec.u16(24),
            IletCodec.u64(1_700_000_000L),
            IletCodec.u64(1_700_007_200L),
            IletCodec.u64(ILET_PUMP_EPOCH_REFERENCE),
            IletCodec.u32(9),
            IletCodec.u16(2000),
            byteArrayOf(1, 2),
        )
        val checksum = IletCrc.payloadCrc(pre, 4, pre.size - 4)
        val body = IletCodec.fixed(concat(pre, IletCodec.u32(checksum.toLong() and 0xFFFFFFFFL)), ILET_GLUCOSE_HIST_BYTES)
        val rec = parseHistoricalBody(IletHistoricalRecordType.INSULIN.value, body)
        check(rec is com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletHistoricalInsulinRecord)
        val insulin = rec as com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletHistoricalInsulinRecord
        check(insulin.sequenceNumber == 9L)
        check(Math.abs(insulin.totalDoseUnits - 2.0) < 1e-9)
    }

    @Test
    fun pumpStatusLayout() {
        val payload = concat(
            IletCodec.u32(1_700_000_000L),
            IletCodec.i16(2500),
            IletCodec.f32(2.35f),
            byteArrayOf(1),
        )
        check(payload.size == 11)
        val s = IletBlePumpStatus.parse(payload)
        check(s.requestTime == 1_700_000_000L)
        check(s.unitsRequestedX1000 == 2500)
        check(Math.abs(s.unitsRequested - 2.5) < 1e-9)
        // The wire field is a float already in units; the reference property
        // divides by 1000, so assert on the raw parsed field.
        check(Math.abs(s.unitsDeliveredX1000 - 2.35f) < 1e-5)
        check(s.available == 1)
    }

    @Test
    fun algoStepRoundTrip() {
        fun pumpStatus(reqTime: Long, reqUnits: Int, delUnits: Float, avail: Int): ByteArray =
            concat(IletCodec.u32(reqTime), IletCodec.i16(reqUnits), IletCodec.f32(delUnits), byteArrayOf(avail.toByte()))

        val cgmRaw = IletCodec.concat(*(0 until 10).map { IletCodec.i16(it) }.toTypedArray())
        val payload = concat(
            byteArrayOf(5, 3),
            IletCodec.u32(99),
            IletCodec.u64(1_700_000_000L),
            IletCodec.u64(1_700_007_200L),
            IletCodec.i16(-360),
            IletCodec.u16(ILET_ALGO_STEP_BYTES),
            IletCodec.u64(1_700_003_600L),
            IletCodec.u32(42),
            pumpStatus(1_700_000_000L, 2500, 2.35f, 1),
            pumpStatus(1_700_000_100L, 500, 0.5f, 1),
            byteArrayOf(4),
            IletCodec.i16(120),
            IletCodec.i16(125),
            cgmRaw,
            byteArrayOf(10),
            IletCodec.i16(100),
            IletCodec.i16(2),
            IletCodec.i16(5),
            byteArrayOf(70, 1, 100, 50),
            byteArrayOf(50, 10, 1, 100, 200.toByte(), 20, 5, 30),
            byteArrayOf(0, 0, 0, 0), // dynamic_sp, g_basal, d_type, pbh
            IletCodec.f32(0.5f),
            IletCodec.f32(1.2f),
            IletCodec.f32(100f),
            IletCodec.f32(110f),
            IletCodec.f32(120f),
            IletCodec.u32(60),
            IletCodec.i16(100), IletCodec.i16(200), IletCodec.i16(30), IletCodec.i16(400),
            IletCodec.i16(500), IletCodec.i16(30), IletCodec.i16(40), IletCodec.i16(7),
            byteArrayOf(10, 20),
            byteArrayOf(1, 2, 3),
            byteArrayOf(4, 5, 6, 7),
            byteArrayOf(8, 9, 10),
            byteArrayOf(100),
            IletCodec.f32(3.14f),
            byteArrayOf(0, 5),
            IletCodec.f32(1.1f),
            IletCodec.f32(1.2f),
            IletCodec.f32(1.3f),
            byteArrayOf(0),
        )
        check(payload.size == ILET_ALGO_STEP_BYTES) { "synthetic payload is 180 bytes, got ${payload.size}" }
        val r = IletAlgoStepRecord.parse(payload)
        check(r.recordType == 5)
        check(r.sequenceNumber == 99L)
        check(Math.abs(r.insulinPumpStatus.unitsRequested - 2.5) < 1e-9)
        check(Math.abs(r.glucagonPumpStatus.unitsDeliveredX1000 - 0.5f) < 1e-9)
        check(r.cgmType == 4)
        check(r.cgmValue == 120)
        check(r.cgmValuesRaw == (0 until 10).toList())
        check(r.setPoint == 50)
        check(Math.abs(r.openLoopIsf - 0.5f) < 1e-9)
        check(Math.abs(r.iob - 3.14f) < 1e-9)
        check(r.openLoopStatus == 0)
    }

    @Test
    fun backFillHistoryFixedSize() {
        val payload = ByteArray(1036)
        payload[0] = 1
        payload[1] = 1
        System.arraycopy(IletCodec.u32(5), 0, payload, 2, 4)
        System.arraycopy(IletCodec.u16(51), 0, payload, 6, 2)
        System.arraycopy(IletCodec.u16(0), 0, payload, 8, 2)
        System.arraycopy(IletCodec.u16(51), 0, payload, 10, 2)
        val r = IletBackFillHistoryResponse.parse(payload)
        check(r.status == 1 && r.isOk)
        check(r.historicalType == IletHistoricalRecordType.GLUCOSE)
        check(r.bytesInChunk == 51 && r.data.size == 51)
    }

    // ================================================================ helpers

    private fun gRecord(seq: Long, tag: Int = 1): ByteArray = concat(
        byteArrayOf(tag.toByte(), 3),
        IletCodec.u32(seq),
        IletCodec.u64(0),
        IletCodec.u64(0),
        IletCodec.i16(0),
        IletCodec.u16(ILET_GLUCOSE_BACKFILL_BYTES),
        IletCodec.u64(0),
        byteArrayOf(4),
        IletCodec.i16(100),
        IletCodec.i16(105),
        byteArrayOf(1, 2, 3, 4),
        IletCodec.u64(0),
    )

    private fun glucoseHistBody(seq: Long, reading: Int, predicted: Int, active: Boolean = true): ByteArray {
        val head = concat(
            ByteArray(2),
            IletCodec.u16(ILET_GLUCOSE_HIST_BYTES + 20),
            hex("cefa9810"),
            IletCodec.u16(1),
            IletCodec.u16(45),
            IletCodec.u64(1_700_000_000L),
            IletCodec.u64(1_700_000_300L),
            IletCodec.u64(0),
            IletCodec.u64(ILET_PUMP_EPOCH_REFERENCE),
            IletCodec.u32(seq),
            ByteArray(1),
        )
        val readingBlock = concat(
            IletCodec.i16(reading),
            IletCodec.i16(predicted),
            byteArrayOf(3, 2, 2, 2),
        )
        val marker = if (active) ILET_GLUCOSE_ACTIVE_MARKER else ByteArray(4)
        val preChecksum = concat(head, readingBlock, marker, ByteArray(4))
        val checksum = IletCrc.payloadCrc(preChecksum, 4, preChecksum.size - 4)
        return IletCodec.fixed(
            concat(preChecksum, IletCodec.u32(checksum.toLong() and 0xFFFFFFFFL)),
            ILET_GLUCOSE_HIST_BYTES,
        )
    }

    private fun hmac(key: ByteArray, message: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(message)
    }

    private fun concat(vararg parts: ByteArray): ByteArray = IletCodec.concat(*parts)

    private fun hex(value: String): ByteArray {
        val clean = value.replace(" ", "")
        val out = ByteArray(clean.length / 2)
        for (i in out.indices) out[i] = clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        return out
    }
}
