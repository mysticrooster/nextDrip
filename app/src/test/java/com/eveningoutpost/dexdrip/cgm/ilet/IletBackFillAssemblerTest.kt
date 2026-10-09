package com.eveningoutpost.dexdrip.cgm.ilet

import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletBackFillAssembler
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletBackFillHistoryResponse
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

/** Reassembly of the 0x2333 back-fill stream into whole sectors. */
class IletBackFillAssemblerTest {

    private fun frame(seq: Long, offset: Int, chunk: Int, total: Int, data: ByteArray) =
        IletBackFillHistoryResponse(
            status = 1,
            recordType = 1,
            sequenceNumber = seq,
            bytesInChunk = chunk,
            bytesOffset = offset,
            bytesTotal = total,
            data = data,
        )

    @Test
    fun assemblesChunksOfOneSector() {
        val a = ByteArray(4) { 1 }
        val b = ByteArray(2) { 2 }
        val sector = IletBackFillAssembler.assemble(
            listOf(
                frame(7, offset = 0, chunk = a.size, total = 6, data = a),
                frame(7, offset = 4, chunk = b.size, total = 6, data = b),
            )
        )
        assertEquals(1, sector.size)
        assertArrayEquals(byteArrayOf(1, 1, 1, 1, 2, 2), sector[0])
    }

    @Test
    fun assemblesTwoSectorsAndIgnoresErrors() {
        val s1 = ByteArray(3) { 9 }
        val s2 = ByteArray(2) { 8 }
        val sector = IletBackFillAssembler.assemble(
            listOf(
                frame(1, 0, 3, 3, s1),
                IletBackFillHistoryResponse(0, 1, 2, 0, 0, 5, ByteArray(0)),
                frame(2, 0, 2, 2, s2),
            )
        )
        assertEquals(2, sector.size)
        assertArrayEquals(byteArrayOf(9, 9, 9), sector[0])
        assertArrayEquals(byteArrayOf(8, 8), sector[1])
    }
}
