package com.eveningoutpost.dexdrip.cgm.ilet.cloud

import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletSigner
import com.eveningoutpost.dexdrip.utilitymodels.OkHttpWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import java.util.Base64

open class IletCloudError(message: String) : Exception(message)

/**
 * Signs the 96-byte enrolment blob through Beta Bionics' attest route.
 *
 * Ported from `ilet/cloud.py`: `GET {base}/1/attest/sign?data=<b64>` with the
 * Cognito id token as a bearer. The route echoes the input and appends a
 * signature; we take the last 64 bytes.
 */
class IletCloudSigner(
    private val store: IletCredentialStore,
    private val auth: IletCognitoAuth = IletCognitoAuth(),
    private val baseUrl: String = DEFAULT_BASE_URL,
) : IletSigner {

    override suspend fun sign(data: ByteArray): ByteArray = withContext(Dispatchers.IO) {
        // Refresh the session first if needed (and clear it if Cognito rejects the refresh).
        if (!IletSessionKeeper(store, auth).refreshIfNeeded()) {
            throw IletCloudError("not signed in: no valid Cognito session")
        }
        val idToken = store.loadSession()?.idToken
            ?: throw IletCloudError("not signed in: no Cognito session")

        val payload = Base64.getEncoder().encodeToString(data)
        val url = "$baseUrl/$SIGN_PATH"
            .toHttpUrl()
            .newBuilder()
            .addQueryParameter("data", payload)
            .build()
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $idToken")
            .get()
            .build()

        OkHttpWrapper.getClient().newCall(request).execute().use { response ->
            val contentType = response.body?.contentType()?.toString().orEmpty()
            val text = response.body?.string().orEmpty().trim()
            if (response.code == 401) {
                throw IletCloudError(
                    "401 from the signing route: the id token is missing, expired or not " +
                        "accepted for this account"
                )
            }
            if (!response.isSuccessful) {
                throw IletCloudError("${response.code} from $SIGN_PATH: ${text.take(200)}")
            }
            val decoded = IletSignResponse.decodeSignResponse(text)
                ?: throw IletCloudError(
                    "signing route returned something that is not base64 " +
                        "(content-type=$contentType): ${text.take(200)}"
                )
            if (decoded.size < SIGNATURE_BYTES) {
                throw IletCloudError("signing route returned ${decoded.size} bytes, need at least $SIGNATURE_BYTES")
            }
            decoded.copyOfRange(decoded.size - SIGNATURE_BYTES, decoded.size)
        }
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://us-apps.betabionicsapi.com"
        const val SIGN_PATH = "1/attest/sign"
        const val SIGNATURE_BYTES = 64
    }
}

/**
 * Response decoding for the attest signing route. Kept separate so it can be
 * unit-tested without a network or Android context.
 */
internal object IletSignResponse {

    private val JSON_BASE64_KEYS = listOf("signature", "data", "signed", "result", "attestation")

    /**
     * The signing route is expected to return the base64 of the signed blob, but
     * a misconfigured or rejected account can return a JSON error body instead.
     * Accept either a raw base64 string or a JSON object carrying the payload in
     * a known field, and return null when neither decodes.
     */
    fun decodeSignResponse(text: String): ByteArray? {
        if (text.isEmpty()) return null
        if (text.startsWith("{")) {
            try {
                val json = JSONObject(text)
                for (key in JSON_BASE64_KEYS) {
                    val value = json.optString(key, "")
                    if (value.isNotEmpty()) return base64(value) ?: continue
                }
                val message = json.optString("message", "").ifEmpty { json.optString("error", "") }
                if (message.isNotEmpty()) throw IletCloudError("signing route rejected the request: $message")
            } catch (e: JSONException) {
                // fall through to the raw/base64 path
            }
        }
        // The route may hand the blob back as a JSON string literal, in which case
        // base64 characters have been \uXXXX-escaped (e.g. \u002B for '+').
        return base64(unescapeJson(text))
    }

    /** Decode JSON string escapes (\\uXXXX, \\/, \\", \\n, \\r, \\t). */
    fun unescapeJson(value: String): String {
        if (!value.contains('\\')) return value
        val sb = StringBuilder(value.length)
        var i = 0
        while (i < value.length) {
            val c = value[i]
            if (c != '\\' || i == value.length - 1) {
                sb.append(c)
                i++
                continue
            }
            when (val n = value[i + 1]) {
                'u' -> {
                    if (i + 5 < value.length) {
                        val code = value.substring(i + 2, i + 6).toIntOrNull(16)
                        if (code != null) {
                            sb.append(code.toChar())
                            i += 6
                            continue
                        }
                    }
                    sb.append(c)
                    i++
                }
                '/', '"', '\\' -> {
                    sb.append(n)
                    i += 2
                }
                'n' -> {
                    sb.append('\n')
                    i += 2
                }
                'r' -> {
                    sb.append('\r')
                    i += 2
                }
                't' -> {
                    sb.append('\t')
                    i += 2
                }
                else -> {
                    sb.append(c)
                    i++
                }
            }
        }
        return sb.toString()
    }

    /** Standard or URL-safe base64, tolerating stripped padding. */
    fun base64(value: String): ByteArray? {
        val cleaned = value.trim().trim('"')
        if (cleaned.isEmpty()) return null
        val padded = cleaned + "=".repeat((4 - cleaned.length % 4) % 4)
        return try {
            Base64.getDecoder().decode(padded)
        } catch (e: IllegalArgumentException) {
            try {
                Base64.getUrlDecoder().decode(padded)
            } catch (e2: IllegalArgumentException) {
                null
            }
        }
    }

}
