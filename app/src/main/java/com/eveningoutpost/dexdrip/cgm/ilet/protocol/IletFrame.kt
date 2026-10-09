package com.eveningoutpost.dexdrip.cgm.ilet.protocol

/** Any protocol-level failure that is not a checksum failure. */
open class IletProtocolError(message: String) : Exception(message)

/** Raised when a CRC does not match. */
class IletChecksumError(message: String) : IletProtocolError(message)

/**
 * One command/response frame: opcode, then a u16 length that counts the 4-byte
 * header, then the payload. Ported from `ilet/protocol.py`.
 */
data class IletFrame(val opcode: Int, val payload: ByteArray) {

    val opcodeName: String get() = IletOpcode.nameOf(opcode)

    fun toBytes(): ByteArray =
        IletCodec.concat(IletCodec.u16(opcode), IletCodec.u16(IletConstants.HEADER_BYTES + payload.size), payload)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IletFrame) return false
        return opcode == other.opcode && payload.contentEquals(other.payload)
    }

    override fun hashCode(): Int = 31 * opcode + payload.contentHashCode()

    override fun toString(): String = "IletFrame($opcodeName, ${payload.size}B)"

    companion object {
        fun fromBytes(data: ByteArray): IletFrame {
            if (data.size < IletConstants.HEADER_BYTES) {
                throw IletProtocolError("frame shorter than a header: ${data.size} bytes")
            }
            val opcode = IletCodec.readU16(data, 0)
            val declared = IletCodec.readU16(data, 2)
            if (declared != data.size) {
                throw IletProtocolError(
                    "length mismatch on ${IletOpcode.nameOf(opcode)}: header says $declared, " +
                        "buffer is ${data.size}"
                )
            }
            return IletFrame(opcode, data.copyOfRange(IletConstants.HEADER_BYTES, data.size))
        }
    }
}

/**
 * A frame plus its out-of-band trailer. A cleartext packet carries a 4-byte
 * CRC there; an encrypted packet carries a 4-byte counter and a 16-byte GCM tag.
 */
data class IletBlePacket(val frame: ByteArray, val oob: ByteArray) {

    val isEncrypted: Boolean get() = oob.size > IletConstants.OOB_CRC_BYTES

    val opcode: Int get() = IletCodec.readU16(frame, 0)

    val header: ByteArray get() = frame.copyOfRange(0, IletConstants.HEADER_BYTES)

    val body: ByteArray get() = frame.copyOfRange(IletConstants.HEADER_BYTES, frame.size)

    fun crcReport(): String {
        if (isEncrypted) return "encrypted, no CRC"
        val expected = IletCrc.frameCrc(frame)
        val actual = IletCodec.readU32(oob, 0)
        val verdict = if (expected.toLong() and 0xFFFFFFFFL == actual) "match" else "MISMATCH"
        return String.format("computed 0x%08X received 0x%08X %s", expected, actual, verdict)
    }

    fun verifyCrc() {
        if (isEncrypted) throw IletProtocolError("verifyCrc called on an encrypted packet")
        val expected = IletCrc.frameCrc(frame)
        val actual = IletCodec.readU32(oob, 0)
        if (expected.toLong() and 0xFFFFFFFFL != actual) {
            throw IletChecksumError(
                String.format(
                    "CRC mismatch: computed 0x%08X, received 0x%08X. If this fires on every " +
                        "frame, the CRC variant in IletCrc is wrong.",
                    expected,
                    actual
                )
            )
        }
    }

    fun toFrame(): IletFrame = IletFrame.fromBytes(frame)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IletBlePacket) return false
        return frame.contentEquals(other.frame) && oob.contentEquals(other.oob)
    }

    override fun hashCode(): Int = 31 * frame.contentHashCode() + oob.contentHashCode()

    override fun toString(): String {
        val kind = if (isEncrypted) "enc" else "clr"
        return "IletBlePacket($kind, frame=${frame.size}B, oob=${oob.size}B)"
    }

    companion object {
        fun cleartext(frame: IletFrame): IletBlePacket {
            val raw = frame.toBytes()
            return IletBlePacket(raw, IletCodec.u32(IletCrc.frameCrc(raw).toLong() and 0xFFFFFFFFL))
        }
    }
}

/**
 * Split a notification arriving on the real-time characteristic. The pump sends
 * either a fully framed buffer, or a headerless payload that we re-frame.
 */
fun splitRealtimeNotification(data: ByteArray): IletBlePacket {
    if (data.size < 2) {
        throw IletProtocolError("real-time notification too short: ${data.size} bytes")
    }
    if (IletCodec.readU16(data, 0) == IletOpcode.REAL_TIME_DATA.value) {
        val declared = IletCodec.readU16(data, 2)
        if (declared > data.size) {
            throw IletProtocolError("real-time frame claims $declared bytes, got ${data.size}")
        }
        return IletBlePacket(data.copyOfRange(0, declared), data.copyOfRange(declared, data.size))
    }
    val total = data.size + IletConstants.HEADER_BYTES
    if (total > 0xFF) {
        throw IletProtocolError(
            "headerless real-time payload of ${data.size} bytes overflows the single-byte " +
                "length field the protocol uses on this path"
        )
    }
    val header = IletCodec.concat(
        IletCodec.u16(IletOpcode.REAL_TIME_DATA.value),
        byteArrayOf(total.toByte(), 0x00)
    )
    return IletBlePacket(IletCodec.concat(header, data), ByteArray(IletConstants.OOB_CRC_BYTES))
}
