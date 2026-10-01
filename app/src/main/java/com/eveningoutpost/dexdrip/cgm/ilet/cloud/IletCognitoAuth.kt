package com.eveningoutpost.dexdrip.cgm.ilet.cloud

import com.eveningoutpost.dexdrip.utilitymodels.OkHttpWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.math.BigInteger
import java.text.SimpleDateFormat
import java.util.Base64
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

open class IletAuthError(message: String) : Exception(message)

/** Raised when the account needs a TOTP code that has not been supplied yet. */
class IletMfaRequired(message: String) : IletAuthError(message)

/**
 * Cognito USER_SRP_AUTH for the Beta Bionics user pool.
 *
 * Ported from `pycognito`'s `aws_srp.py` (the reference the Python iLet client
 * uses). Self-contained: BigInteger + HMAC-SHA256, no AWS SDK. The password is
 * only held for the duration of `signIn` and never persisted.
 */
class IletCognitoAuth(
    private val poolId: String = DEFAULT_POOL_ID,
    private val clientId: String = DEFAULT_CLIENT_ID,
    private val region: String = DEFAULT_REGION,
) {

    private val endpoint: String get() = "https://cognito-idp.$region.amazonaws.com/"
    private val poolName: String get() = poolId.split("_")[1]

    suspend fun signIn(username: String, password: String, totp: String? = null): IletCognitoSession =
        withContext(Dispatchers.IO) {
            val srp = IletSrp.random()
            val initiate = post(
                "InitiateAuth",
                JSONObject()
                    .put("AuthFlow", "USER_SRP_AUTH")
                    .put("ClientId", clientId)
                    .put(
                        "AuthParameters",
                        JSONObject().put("USERNAME", username).put("SRP_A", srp.longToHex(srp.largeA)),
                    ),
            )
            val challengeName = initiate.optString("ChallengeName")
            if (challengeName != "PASSWORD_VERIFIER") {
                throw IletAuthError("unexpected challenge from Cognito: $challengeName")
            }
            val challenge = initiate.getJSONObject("ChallengeParameters")
            val userIdForSrp = challenge.getString("USER_ID_FOR_SRP")
            val internalUsername = challenge.optString("USERNAME", username)
            val saltHex = challenge.getString("SALT")
            val srpB = challenge.getString("SRP_B")
            val secretBlockB64 = challenge.getString("SECRET_BLOCK")

            val hkdf = srp.deriveKey(
                username = userIdForSrp,
                password = password,
                poolName = poolName,
                serverB = BigInteger(srpB, 16),
                saltHex = saltHex,
            )
            val timestamp = cognitoTimestamp(Date())
            val secretBlock = Base64.getDecoder().decode(secretBlockB64)
            val msg = poolName.toByteArray(Charsets.UTF_8) +
                userIdForSrp.toByteArray(Charsets.UTF_8) +
                secretBlock +
                timestamp.toByteArray(Charsets.UTF_8)
            val signature = Base64.getEncoder().encodeToString(hmac(hkdf, msg))

            var response = post(
                "RespondToAuthChallenge",
                JSONObject()
                    .put("ClientId", clientId)
                    .put("ChallengeName", "PASSWORD_VERIFIER")
                    .put(
                        "ChallengeResponses",
                        JSONObject()
                            .put("TIMESTAMP", timestamp)
                            .put("USERNAME", internalUsername)
                            .put("PASSWORD_CLAIM_SECRET_BLOCK", secretBlockB64)
                            .put("PASSWORD_CLAIM_SIGNATURE", signature),
                    ),
            )

            if (response.optString("ChallengeName") == "SOFTWARE_TOKEN_MFA") {
                if (totp.isNullOrBlank()) {
                    throw IletMfaRequired("this account requires a TOTP code")
                }
                val session = response.optString("Session")
                if (session.isEmpty()) throw IletAuthError("MFA challenge carried no session")
                response = post(
                    "RespondToAuthChallenge",
                    JSONObject()
                        .put("ClientId", clientId)
                        .put("ChallengeName", "SOFTWARE_TOKEN_MFA")
                        .put("Session", session)
                        .put(
                            "ChallengeResponses",
                            JSONObject()
                                .put("USERNAME", internalUsername)
                                .put("SOFTWARE_TOKEN_MFA_CODE", totp),
                        ),
                )
            }

            when (response.optString("ChallengeName")) {
                "", "null" -> Unit
                "NEW_PASSWORD_REQUIRED" -> throw IletAuthError("Cognito requires a password change")
                "SMS_MFA" -> throw IletAuthError("Cognito requires SMS MFA, which is not supported")
                else -> throw IletAuthError("unsupported challenge: ${response.optString("ChallengeName")}")
            }

            val result = response.optJSONObject("AuthenticationResult")
                ?: throw IletAuthError("Cognito returned no AuthenticationResult")
            IletCognitoSession(
                username = username,
                idToken = result.getString("IdToken"),
                accessToken = result.getString("AccessToken"),
                refreshToken = result.optString("RefreshToken"),
            )
        }

    suspend fun refresh(refreshToken: String): IletCognitoSession = withContext(Dispatchers.IO) {
        val response = post(
            "InitiateAuth",
            JSONObject()
                .put("AuthFlow", "REFRESH_TOKEN_AUTH")
                .put("ClientId", clientId)
                .put("AuthParameters", JSONObject().put("REFRESH_TOKEN", refreshToken)),
        )
        val result = response.optJSONObject("AuthenticationResult")
            ?: throw IletAuthError("refresh returned no AuthenticationResult")
        IletCognitoSession(
            username = "",
            idToken = result.getString("IdToken"),
            accessToken = result.getString("AccessToken"),
            refreshToken = result.optString("RefreshToken", refreshToken),
        )
    }

    private fun post(target: String, body: JSONObject): JSONObject {
        val request = Request.Builder()
            .url(endpoint)
            .header("X-Amz-Target", "AWSCognitoIdentityProviderService.$target")
            .header("Content-Type", "application/x-amz-json-1.1")
            .post(body.toString().toRequestBody(JSON))
            .build()
        OkHttpWrapper.getClient().newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IletAuthError("$target failed: HTTP ${response.code} ${text.take(200)}")
            }
            return JSONObject(text)
        }
    }

    companion object {
        const val DEFAULT_POOL_ID = "us-east-2_HNbbVuwO8"
        const val DEFAULT_CLIENT_ID = "9tk4bau9e8ucdptg9jcq6f12s"
        const val DEFAULT_REGION = "us-east-2"

        private val JSON = "application/x-amz-json-1.1".toMediaType()

        /** Cognito's timestamp text: `Www Mmm d HH:mm:ss UTC yyyy`, UTC. */
        fun cognitoTimestamp(date: Date): String {
            val format = SimpleDateFormat("EEE MMM d HH:mm:ss 'UTC' yyyy", Locale.US)
            format.timeZone = TimeZone.getTimeZone("UTC")
            return format.format(date)
        }

        internal fun hmac(key: ByteArray, message: ByteArray): ByteArray {
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(key, "HmacSHA256"))
            return mac.doFinal(message)
        }
    }
}

/**
 * The SRP-A math, ported field-for-field from `pycognito.aws_srp`. Holds the
 * ephemeral `a`/`A` for one authentication.
 */
internal class IletSrp internal constructor(private val smallA: BigInteger) {

    private val n: BigInteger = N
    private val g: BigInteger = BigInteger.valueOf(2)
    private val k: BigInteger = BigInteger(hexHash("00" + N_HEX + "0" + "2"), 16)
    val largeA: BigInteger = g.modPow(smallA, n)

    init {
        if (largeA.mod(n) == BigInteger.ZERO) throw IletAuthError("SRP safety check for A failed")
    }

    fun longToHex(value: BigInteger): String = value.toString(16)

    fun padHex(value: BigInteger): String = padHex(longToHex(value))

    fun padHex(hex: String): String {
        var hashStr = hex
        if (hashStr.length % 2 == 1) {
            hashStr = "0$hashStr"
        } else if (hashStr[0] in "89ABCDEFabcdef") {
            hashStr = "00$hashStr"
        }
        return hashStr
    }

    fun hashSha256(buf: ByteArray): String = sha256Hex(buf).padStart(64, '0')

    fun hexHash(hex: String): String = hashSha256(bytesFromHex(hex))

    fun calculateU(bigA: BigInteger, bigB: BigInteger): BigInteger =
        BigInteger(hexHash(padHex(bigA) + padHex(bigB)), 16)

    fun computeHkdf(ikm: ByteArray, salt: ByteArray): ByteArray {
        val prk = IletCognitoAuth.hmac(salt, ikm)
        val info = INFO_BITS + byteArrayOf(1)
        return IletCognitoAuth.hmac(prk, info).copyOf(16)
    }

    fun deriveKey(
        username: String,
        password: String,
        poolName: String,
        serverB: BigInteger,
        saltHex: String,
    ): ByteArray {
        val u = calculateU(largeA, serverB)
        if (u == BigInteger.ZERO) throw IletAuthError("SRP U cannot be zero")

        val usernamePassword = "$poolName$username:$password"
        val usernamePasswordHash = hashSha256(usernamePassword.toByteArray(Charsets.UTF_8))

        val x = BigInteger(hexHash(padHex(saltHex) + usernamePasswordHash), 16)
        val gModPowXn = g.modPow(x, n)
        val intValue2 = serverB.subtract(k.multiply(gModPowXn))
        val s = intValue2.modPow(smallA.add(u.multiply(x)), n)

        return computeHkdf(
            ikm = bytesFromHex(padHex(s)),
            salt = bytesFromHex(padHex(u)),
        )
    }

    private fun sha256Hex(buf: ByteArray): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256").digest(buf)
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun bytesFromHex(hex: String): ByteArray {
        val h = if (hex.length % 2 == 1) "0$hex" else hex
        val out = ByteArray(h.length / 2)
        for (i in out.indices) out[i] = h.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        return out
    }

    companion object {
        // RFC 5054 group 3072-bit prime, as used by amazon-cognito-identity-js.
        private const val N_HEX =
            "FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD1" +
                "29024E088A67CC74020BBEA63B139B22514A08798E3404DD" +
                "EF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245" +
                "E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7ED" +
                "EE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3D" +
                "C2007CB8A163BF0598DA48361C55D39A69163FA8FD24CF5F" +
                "83655D23DCA3AD961C62F356208552BB9ED529077096966D" +
                "670C354E4ABC9804F1746C08CA18217C32905E462E36CE3B" +
                "E39E772C180E86039B2783A2EC07A28FB5C55DF06F4C52C9" +
                "DE2BCBF6955817183995497CEA956AE515D2261898FA0510" +
                "15728E5A8AAAC42DAD33170D04507A33A85521ABDF1CBA64" +
                "ECFB850458DBEF0A8AEA71575D060C7DB3970F85A6E1E4C7" +
                "ABF5AE8CDB0933D71E8C94E04A25619DCEE3D2261AD2EE6B" +
                "F12FFA06D98A0864D87602733EC86A64521F2B18177B200C" +
                "BBE117577A615D6C770988C0BAD946E208E24FA074E5AB31" +
                "43DB5BFCE0FD108E4B82D120A93AD2CAFFFFFFFFFFFFFFFF"

        private val INFO_BITS = "Caldera Derived Key".toByteArray(Charsets.UTF_8)

        internal val N: BigInteger = BigInteger(N_HEX, 16)

        internal fun random(): IletSrp =
            IletSrp(BigInteger(1, ByteArray(128).also { java.security.SecureRandom().nextBytes(it) }).mod(N))
    }
}
