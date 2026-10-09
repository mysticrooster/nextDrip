package com.eveningoutpost.dexdrip.cgm.ilet.cloud

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Base64

/** Response-decoding coverage for the attest signing route. */
class IletCloudSignerResponseTest : RobolectricTestWithConfig() {

    private fun signedBlob(): ByteArray {
        // 96-byte signing input + 64-byte signature, like the real route.
        val blob = ByteArray(160)
        for (i in blob.indices) blob[i] = (i * 7).toByte()
        return blob
    }

    @Test
    fun acceptsRawBase64() {
        val blob = signedBlob()
        val body = Base64.getEncoder().encodeToString(blob)
        assertEquals(blob.toList(), IletSignResponse.decodeSignResponse(body)!!.toList())
    }

    @Test
    fun acceptsJsonStringWithUnicodeEscapes() {
        // The route returns a JSON string literal where '+' and '/' became \u002B / \u002F.
        val blob = signedBlob()
        val body = "\"" + Base64.getEncoder().encodeToString(blob)
            .replace("+", "\\u002B")
            .replace("/", "\\u002F") + "\""
        assertEquals(blob.toList(), IletSignResponse.decodeSignResponse(body)!!.toList())
    }

    @Test
    fun acceptsJsonObjectWithSignatureField() {
        val blob = signedBlob()
        val signature = Base64.getEncoder().encodeToString(blob)
        val body = "{\"signature\":\"$signature\"}"
        assertEquals(blob.toList(), IletSignResponse.decodeSignResponse(body)!!.toList())
    }

    @Test
    fun returnsNullForNonBase64Garbage() {
        assertNull(IletSignResponse.decodeSignResponse("not base64 at all!!!"))
    }

    @Test
    fun reportsErrorMessages() {
        try {
            IletSignResponse.decodeSignResponse("{\"message\":\"account not eligible\"}")
            throw AssertionError("expected IletCloudError")
        } catch (e: IletCloudError) {
            assertNotNull(e.message)
        }
    }

    @Test
    fun unescapesJsonEscapes() {
        assertEquals("a+b/c", IletSignResponse.unescapeJson("a\\u002Bb\\u002Fc"))
    }
}
