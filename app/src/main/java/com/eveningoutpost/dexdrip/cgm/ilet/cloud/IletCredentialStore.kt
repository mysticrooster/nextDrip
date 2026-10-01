package com.eveningoutpost.dexdrip.cgm.ilet.cloud

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletChannelIdentity
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletIdentityStore
import org.json.JSONObject
import java.io.File
import java.security.KeyStore
import java.util.Base64
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Seals and opens the credential blob. Injected so tests need no AndroidKeyStore. */
interface IletSecretBox {
    fun seal(plain: ByteArray): ByteArray
    fun open(sealed: ByteArray): ByteArray
}

/**
 * AES-256-GCM key held in the AndroidKeyStore. The key never leaves the
 * keystore; only the ciphertext is written to disk.
 */
class IletKeystoreBox(private val alias: String = "ilet_credential_key_v1") : IletSecretBox {

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getEntry(alias, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    override fun seal(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val iv = cipher.iv
        val ct = cipher.doFinal(plain)
        return ByteArray(1) { iv.size.toByte() } + iv + ct
    }

    override fun open(sealed: ByteArray): ByteArray {
        val ivLength = sealed[0].toInt() and 0xFF
        val iv = sealed.copyOfRange(1, 1 + ivLength)
        val ct = sealed.copyOfRange(1 + ivLength, sealed.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
        return cipher.doFinal(ct)
    }
}

/** The Cognito tokens plus the username that owns them. */
data class IletCognitoSession(
    val username: String,
    val idToken: String,
    val accessToken: String,
    val refreshToken: String,
    val sub: String = "",
) {
    /** Expiry (epoch seconds) from the id token's `exp` claim, or 0 when unreadable. */
    val expiresAtEpochSeconds: Long
        get() = try {
            val payload = idToken.split(".")[1]
            val json = String(Base64.getUrlDecoder().decode(padBase64(payload)))
            JSONObject(json).optLong("exp", 0L)
        } catch (e: Exception) {
            0L
        }

    fun isExpired(nowEpochSeconds: Long = System.currentTimeMillis() / 1000): Boolean {
        val exp = expiresAtEpochSeconds
        return exp <= 0L || nowEpochSeconds >= exp
    }

    private fun padBase64(value: String): String = value + "=".repeat((4 - value.length % 4) % 4)
}

/**
 * Keystore-wrapped file store for the iLet credential and Cognito session.
 *
 * Semantics mirror the Python cache: `clearIdentity()` forgets the key material
 * but keeps the app UUID (which the pump binds to); `clearAll()` wipes
 * everything. Never logs key material.
 */
class IletCredentialStore(
    context: Context,
    private val box: IletSecretBox = IletKeystoreBox(),
) : IletIdentityStore {

    private val file = File(context.filesDir, "ilet_credentials.bin")
    private var data: JSONObject = load()

    private fun load(): JSONObject {
        if (!file.exists()) return JSONObject()
        return try {
            val plain = box.open(file.readBytes())
            val json = JSONObject(String(plain, Charsets.UTF_8))
            if (json.optInt("schema", 0) != SCHEMA) JSONObject() else json
        } catch (e: Exception) {
            // Unreadable or from an older schema: start fresh. The identity can
            // be re-enrolled, so losing it is recoverable.
            JSONObject()
        }
    }

    @Synchronized
    private fun save() {
        data.put("schema", SCHEMA)
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeBytes(box.seal(data.toString().toByteArray(Charsets.UTF_8)))
        if (!tmp.renameTo(file)) {
            file.delete()
            tmp.renameTo(file)
        }
    }

    // ------------------------------------------------------------ identity

    @Synchronized
    fun hasIdentity(): Boolean =
        !data.optString("private_key").isEmpty() &&
            !data.optString("nonce").isEmpty() &&
            !data.optString("cloud_signature").isEmpty()

    @Synchronized
    override fun appUuid(): UUID {
        val raw = data.optString("app_uuid")
        if (raw.isNotEmpty()) return UUID.fromString(raw)
        val generated = UUID.randomUUID()
        data.put("app_uuid", generated.toString())
        save()
        return generated
    }

    @Synchronized
    override fun loadIdentity(): IletChannelIdentity? {
        if (!hasIdentity()) return null
        return try {
            IletChannelIdentity(
                appUuid = UUID.fromString(data.getString("app_uuid")),
                privateKey = decode(data.getString("private_key")),
                nonce = decode(data.getString("nonce")),
                cloudSignature = decode(data.getString("cloud_signature")),
            )
        } catch (e: Exception) {
            clearIdentity()
            null
        }
    }

    @Synchronized
    override fun storeIdentity(identity: IletChannelIdentity) {
        data.put("app_uuid", identity.appUuid.toString())
        data.put("private_key", encode(identity.privateKey))
        data.put("nonce", encode(identity.nonce))
        data.put("cloud_signature", encode(identity.cloudSignature))
        save()
    }

    /** Forget the key material but keep the app UUID the pump is bound to. */
    @Synchronized
    override fun clearIdentity() {
        data.remove("private_key")
        data.remove("nonce")
        data.remove("cloud_signature")
        save()
    }

    // ------------------------------------------------------------ session

    @Synchronized
    fun hasSession(): Boolean = !(data.optJSONObject("session")?.optString("refresh_token").isNullOrEmpty())

    @Synchronized
    fun loadSession(): IletCognitoSession? {
        val session = data.optJSONObject("session") ?: return null
        val refresh = session.optString("refresh_token")
        if (refresh.isEmpty()) return null
        return IletCognitoSession(
            username = session.optString("username"),
            idToken = session.optString("id_token"),
            accessToken = session.optString("access_token"),
            refreshToken = refresh,
            sub = session.optString("sub"),
        )
    }

    @Synchronized
    fun storeSession(session: IletCognitoSession) {
        val json = JSONObject()
            .put("username", session.username)
            .put("id_token", session.idToken)
            .put("access_token", session.accessToken)
            .put("refresh_token", session.refreshToken)
            .put("sub", session.sub)
        data.put("session", json)
        save()
    }

    @Synchronized
    fun clearSession() {
        data.remove("session")
        save()
    }

    @Synchronized
    fun clearAll() {
        data = JSONObject()
        save()
    }

    @Synchronized
    fun summary(): Map<String, Any?> = mapOf(
        "app_uuid" to data.optString("app_uuid").ifEmpty { null },
        "identity" to hasIdentity(),
        "session" to hasSession(),
        "session_user" to data.optJSONObject("session")?.optString("username"),
    )

    private fun encode(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)

    private fun decode(text: String): ByteArray = Base64.getDecoder().decode(text)

    companion object {
        private const val SCHEMA = 1

        @Volatile
        private var instance: IletCredentialStore? = null

        fun getInstance(context: Context): IletCredentialStore =
            instance ?: synchronized(this) {
                instance ?: IletCredentialStore(context.applicationContext).also { instance = it }
            }
    }
}
