package com.eveningoutpost.dexdrip.cgm.ilet.protocol

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Little-endian readers and writers for iLet payloads. Ported from
 * `ilet/codec.py`.
 *
 * [IletReader] is a cursor over a little-endian payload. Every accessor throws
 * [IletProtocolError] if the buffer is too short — this is the only guard
 * against a badly-shaped pump response; the parsers do not otherwise check
 * their field lengths.
 */
object IletCodec {

    fun u8(value: Int): ByteArray = byteArrayOf((value and 0xFF).toByte())

    fun u16(value: Int): ByteArray = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN)
        .putShort((value and 0xFFFF).toShort()).array()

    fun i16(value: Int): ByteArray = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN)
        .putShort(value.toShort()).array()

    fun u32(value: Long): ByteArray = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
        .putInt(value.toInt()).array()

    fun u64(value: Long): ByteArray = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
        .putLong(value).array()

    fun f32(value: Float): ByteArray = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
        .putFloat(value).array()

    fun readU8(data: ByteArray, offset: Int = 0): Int = data[offset].toInt() and 0xFF

    fun readU16(data: ByteArray, offset: Int = 0): Int =
        (readU8(data, offset) or (readU8(data, offset + 1) shl 8))

    fun readI16(data: ByteArray, offset: Int = 0): Int = readU16(data, offset).toShort().toInt()

    fun readU32(data: ByteArray, offset: Int = 0): Long =
        (readU16(data, offset).toLong() or (readU16(data, offset + 2).toLong() shl 16)) and 0xFFFFFFFFL

    fun readU64(data: ByteArray, offset: Int = 0): Long =
        readU32(data, offset) or (readU32(data, offset + 4) shl 32)

    fun readF32(data: ByteArray, offset: Int = 0): Float =
        ByteBuffer.wrap(data, offset, 4).order(ByteOrder.LITTLE_ENDIAN).float

    fun readBool(data: ByteArray, offset: Int = 0): Boolean = readU8(data, offset) != 0

    /** Pad [data] with zero bytes to [width], or truncate if it is longer. */
    fun fixed(data: ByteArray, width: Int): ByteArray {
        if (data.size >= width) return data.copyOfRange(0, width)
        val out = ByteArray(width)
        System.arraycopy(data, 0, out, 0, data.size)
        return out
    }

    /** ASCII-encode [text] and pad/truncate to [width]. */
    fun fixedAscii(text: String, width: Int): ByteArray = fixed(text.toByteArray(Charsets.US_ASCII), width)

    /** Read [width] ASCII bytes, trimming trailing NULs. */
    fun readFixedAscii(data: ByteArray, offset: Int, width: Int): String =
        String(data, offset, width, Charsets.US_ASCII).trimEnd('\u0000')

    /** Concatenate byte arrays. */
    fun concat(vararg parts: ByteArray): ByteArray {
        var total = 0
        for (p in parts) total += p.size
        val out = ByteArray(total)
        var pos = 0
        for (p in parts) {
            System.arraycopy(p, 0, out, pos, p.size)
            pos += p.size
        }
        return out
    }
}

class IletReader(private val data: ByteArray, private var offset: Int = 0) {

    val position: Int get() = offset
    val remaining: Int get() = data.size - offset

    private fun take(width: Int): ByteArray {
        if (remaining < width) {
            throw IletProtocolError(
                "payload truncated: need $width bytes at offset $offset, have $remaining"
            )
        }
        val chunk = data.copyOfRange(offset, offset + width)
        offset += width
        return chunk
    }

    fun u8(): Int = IletCodec.readU8(take(1))
    fun u16(): Int = IletCodec.readU16(take(2))
    fun i16(): Int = IletCodec.readI16(take(2))
    fun u32(): Long = IletCodec.readU32(take(4))
    fun u64(): Long = IletCodec.readU64(take(8))
    fun f32(): Float = IletCodec.readF32(take(4))
    fun boolean(): Boolean = u8() != 0
    fun raw(width: Int): ByteArray = take(width)
    fun ascii(width: Int): String = take(width).toString(Charsets.US_ASCII).trimEnd('\u0000')

    fun rest(): ByteArray {
        val chunk = data.copyOfRange(offset, data.size)
        offset = data.size
        return chunk
    }
}
