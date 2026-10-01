package com.eveningoutpost.dexdrip.cgm.ilet.cloud

import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletSigner
import com.eveningoutpost.dexdrip.utilitymodels.OkHttpWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
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
        val session = store.loadSession() ?: throw IletCloudError("not signed in: no Cognito session")
        val idToken = if (session.isExpired()) {
            val refreshed = auth.refresh(session.refreshToken)
            store.storeSession(
                session.copy(idToken = refreshed.idToken, accessToken = refreshed.accessToken)
            )
            refreshed.idToken
        } else {
            session.idToken
        }

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
            val text = response.body?.string().orEmpty().trim().trim('"')
            if (response.code == 401) {
                throw IletCloudError(
                    "401 from the signing route: the id token is missing, expired or not " +
                        "accepted for this account"
                )
            }
            if (!response.isSuccessful) {
                throw IletCloudError("${response.code} from $SIGN_PATH: ${text.take(200)}")
            }
            val decoded = try {
                Base64.getDecoder().decode(text)
            } catch (e: IllegalArgumentException) {
                throw IletCloudError("signing route returned something that is not base64")
            }
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
