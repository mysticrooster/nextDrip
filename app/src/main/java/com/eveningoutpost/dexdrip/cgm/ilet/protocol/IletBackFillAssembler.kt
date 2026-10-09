package com.eveningoutpost.dexdrip.cgm.ilet.protocol

import java.io.ByteArrayOutputStream

/**
 * Reassembles the 0x2333 back-fill stream into whole 4096-byte sectors.
 *
 * The pump answers one request with a stream of 1036-byte frames; each frame
 * carries `sequenceNumber`, `bytesOffset`, `bytesInChunk` and `bytesTotal`.
 * A sector is complete when a frame ends at `bytesTotal`.
 */
object IletBackFillAssembler {

    fun assemble(frames: List<IletBackFillHistoryResponse>): List<ByteArray> {
        val sectors = ArrayList<ByteArray>()
        var currentSeq = -1L
        var buffer = ByteArrayOutputStream()
        for (frame in frames) {
            if (!frame.isOk) continue
            if (frame.sequenceNumber != currentSeq) {
                currentSeq = frame.sequenceNumber
                buffer = ByteArrayOutputStream()
            }
            buffer.write(frame.data)
            if (frame.bytesTotal > 0 && frame.bytesOffset + frame.bytesInChunk >= frame.bytesTotal) {
                sectors.add(buffer.toByteArray())
                currentSeq = -1L
            }
        }
        return sectors
    }
}
