package com.eveningoutpost.dexdrip.cgm.ilet.protocol

/**
 * The single CRC-32 variant used on all four iLet wire layers:
 *
 *   width      32
 *   polynomial 0x04C11DB7
 *   init       0xC704DD7B
 *   reflection none (input or output)
 *   xorout     0
 *
 * It covers the frame CRC, the realtime payload CRC, the 0x2334 body checksum
 * and the 0x2333 sector header CRC. Ported from `ilet/codec.py`; one
 * implementation, four call sites.
 */
object IletCrc {

    private const val POLYNOMIAL: Int = 0x04C11DB7
    private const val INIT: Int = 0xC704DD7B.toInt()

    /** Compute the iLet CRC-32 over [data], returned as a 32-bit int. */
    fun frameCrc(data: ByteArray): Int = crc32(data, 0, data.size)

    /** Compute the iLet CRC-32 over a slice of [data]. */
    fun payloadCrc(data: ByteArray, offset: Int, length: Int): Int = crc32(data, offset, length)

    private fun crc32(data: ByteArray, offset: Int, length: Int): Int {
        var crc = INIT
        val end = offset + length
        for (i in offset until end) {
            crc = crc xor ((data[i].toInt() and 0xFF) shl 24)
            for (bit in 0 until 8) {
                crc = if (crc and 0x80000000.toInt() != 0) {
                    (crc shl 1) xor POLYNOMIAL
                } else {
                    crc shl 1
                }
            }
        }
        return crc
    }
}
