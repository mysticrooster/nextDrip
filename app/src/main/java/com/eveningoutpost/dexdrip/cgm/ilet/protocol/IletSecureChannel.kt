package com.eveningoutpost.dexdrip.cgm.ilet.protocol

import org.bouncycastle.crypto.agreement.X25519Agreement
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.generators.HKDFBytesGenerator
import org.bouncycastle.crypto.generators.X25519KeyPairGenerator
import org.bouncycastle.crypto.params.HKDFParameters
import org.bouncycastle.crypto.params.X25519KeyGenerationParameters
import org.bouncycastle.crypto.params.X25519PrivateKeyParameters
import org.bouncycastle.crypto.params.X25519PublicKeyParameters
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

open class IletSecureChannelError(message: String) : Exception(message)

class IletKeyConfirmationError(message: String) : IletSecureChannelError(message)

class IletNotEstablished(message: String) : IletSecureChannelError(message)

/**
 * The credential: four values that travel together. The pump binds to
 * [appUuid]; regenerating it re-enrols the pump, so only clear the identity
 * with the explicit "forget identity" action.
 */
class IletChannelIdentity(
    val appUuid: UUID,
    val privateKey: ByteArray,
    val nonce: ByteArray,
    val cloudSignature: ByteArray,
) {
    init {
        require(privateKey.size == IletConstants.PRIVATE_KEY_BYTES) {
            "private_key must be ${IletConstants.PRIVATE_KEY_BYTES} bytes, got ${privateKey.size}"
        }
        require(nonce.size == IletConstants.NONCE_BYTES) {
            "nonce must be ${IletConstants.NONCE_BYTES} bytes, got ${nonce.size}"
        }
        require(cloudSignature.size == IletConstants.CLOUD_SIGNATURE_BYTES) {
            "cloud_signature must be ${IletConstants.CLOUD_SIGNATURE_BYTES} bytes, got ${cloudSignature.size}"
        }
    }

    val identity: String get() = appUuid.toString()

    /** Derive the X25519 public key from the stored private key. */
    val publicKey: ByteArray
        get() {
            val priv = X25519PrivateKeyParameters(privateKey, 0)
            val out = ByteArray(IletConstants.PUBLIC_KEY_BYTES)
            priv.generatePublicKey().encode(out, 0)
            return out
        }

    /** The 96-byte blob signed by the cloud: padded ASCII uuid || public key. */
    fun signingInput(): ByteArray =
        IletCodec.concat(IletCodec.fixedAscii(identity, IletConstants.IDENTITY_BYTES), publicKey)

    companion object {
        private val random = SecureRandom()

        fun generate(cloudSignature: ByteArray, appUuid: UUID = UUID.randomUUID()): IletChannelIdentity {
            val generator = X25519KeyPairGenerator()
            generator.init(X25519KeyGenerationParameters(random))
            val keyPair = generator.generateKeyPair()
            val priv = (keyPair.private as X25519PrivateKeyParameters).encoded
            val nonce = ByteArray(IletConstants.NONCE_BYTES).also { random.nextBytes(it) }
            return IletChannelIdentity(appUuid, priv, nonce, cloudSignature)
        }
    }
}

/** The pump's identity, parsed from the establish-key response payload. */
class IletPeerIdentity(
    val identity: String,
    val publicKey: ByteArray,
    val nonce: ByteArray,
    val dhKeyTag: ByteArray,
) {
    fun validate() {
        if (identity.isBlank()) throw IletSecureChannelError("pump returned a blank identity")
        if (publicKey.all { it == 0.toByte() }) {
            throw IletSecureChannelError("pump returned an empty or all-zero public key")
        }
        if (nonce.all { it == 0.toByte() }) {
            throw IletSecureChannelError("pump returned an empty or all-zero nonce")
        }
        if (dhKeyTag.all { it == 0.toByte() }) {
            throw IletSecureChannelError("pump returned an empty or all-zero MacTag")
        }
    }

    companion object {
        fun parse(payload: ByteArray): IletPeerIdentity {
            val r = IletReader(payload)
            return IletPeerIdentity(
                identity = r.ascii(IletConstants.IDENTITY_BYTES),
                publicKey = r.raw(IletConstants.PUBLIC_KEY_BYTES),
                nonce = r.raw(IletConstants.NONCE_BYTES),
                dhKeyTag = r.raw(IletConstants.DH_KEY_TAG_BYTES),
            )
        }
    }
}

/**
 * X25519 + HKDF-SHA256 + HMAC-SHA256 + AES-256-GCM secure channel, ported from
 * `ilet/crypto.py`. Holds only symmetric material; the [IletChannelIdentity]
 * supplies the X25519 private key.
 */
class IletSecureChannel(private val identity: IletChannelIdentity) {

    var peer: IletPeerIdentity? = null
        private set

    var counter: Int = 0
        private set

    private var sessionKey: ByteArray? = null
    private var established: Boolean = false

    val isEstablished: Boolean get() = established

    fun reset() {
        peer = null
        sessionKey = null
        established = false
        counter = 0
    }

    /** Build the cleartext establish-key frame (0x3006). */
    fun establishKeyFrame(): IletFrame {
        val payload = IletCodec.concat(
            IletCodec.fixedAscii(identity.identity, IletConstants.IDENTITY_BYTES),
            IletCodec.fixed(identity.publicKey, IletConstants.PUBLIC_KEY_BYTES),
            IletCodec.fixed(identity.cloudSignature, IletConstants.CLOUD_SIGNATURE_BYTES),
            IletCodec.fixed(identity.nonce, IletConstants.NONCE_BYTES),
        )
        return IletFrame(IletOpcode.CMD_SECURE_CHANNEL_ESTABLISH_KEY.value, payload)
    }

    /**
     * Derive the session key from the pump's public key and verify its KC_2_V
     * MacTag. Throws [IletKeyConfirmationError] when the tag does not match.
     */
    fun derive(peer: IletPeerIdentity, verifyTag: Boolean = true) {
        peer.validate()

        val agreement = X25519Agreement()
        agreement.init(X25519PrivateKeyParameters(identity.privateKey, 0))
        val shared = ByteArray(agreement.agreementSize)
        agreement.calculateAgreement(X25519PublicKeyParameters(peer.publicKey, 0), shared, 0)

        val key = deriveSessionKey(shared, identity.nonce)

        if (verifyTag) {
            val expected = macTag(
                key,
                IletConstants.KC_LABEL_PARTY_V,
                roleIdentity = peer.identity,
                otherIdentity = identity.identity,
                roleNonce = peer.nonce,
                otherNonce = identity.nonce,
            )
            if (!constantTimeEquals(expected, peer.dhKeyTag)) {
                throw IletKeyConfirmationError("the pump's KC_2_V MacTag does not match the key we derived")
            }
        }

        this.peer = peer
        this.sessionKey = key
        this.established = true
    }

    /** Build the cleartext confirm-key frame (0x3007) carrying KC_2_U. */
    fun confirmKeyFrame(): IletFrame {
        val key = sessionKey ?: throw IletNotEstablished("derive() must run before confirmKeyFrame()")
        val p = peer ?: throw IletNotEstablished("derive() must run before confirmKeyFrame()")
        val tag = macTag(
            key,
            IletConstants.KC_LABEL_PARTY_U,
            roleIdentity = identity.identity,
            otherIdentity = p.identity,
            roleNonce = identity.nonce,
            otherNonce = p.nonce,
        )
        return IletFrame(IletOpcode.CMD_SECURE_CHANNEL_CONFIRM_KEY.value, tag)
    }

    /** Overwrite the send counter with the pump's `messageCounterU`. */
    fun adoptCounter(messageCounterU: Int) {
        counter = messageCounterU
    }

    /** Encrypt (or wrap cleartext) a frame, advancing the send counter. */
    fun seal(frame: IletFrame): IletBlePacket {
        val packet: IletBlePacket
        if (frame.opcode in CLEARTEXT_OPCODES.map { it.value }) {
            packet = IletBlePacket.cleartext(frame)
        } else {
            if (!established) {
                throw IletNotEstablished(
                    "${frame.opcodeName} needs an established channel; run the handshake first"
                )
            }
            packet = encrypt(frame)
        }
        counter += if (packet.isEncrypted) IletConstants.COUNTER_STEP_ENCRYPTED else IletConstants.COUNTER_STEP_CLEARTEXT
        return packet
    }

    /** Open a packet into a frame. Cleartext CRCs are verified only on request. */
    fun open(packet: IletBlePacket, verifyCrc: Boolean = false): IletFrame {
        if (!packet.isEncrypted) {
            if (verifyCrc) packet.verifyCrc()
            return packet.toFrame()
        }
        if (!established) {
            throw IletNotEstablished("received an encrypted packet before the handshake")
        }
        return decrypt(packet)
    }

    private fun encrypt(frame: IletFrame): IletBlePacket {
        val key = sessionKey ?: throw IletNotEstablished("channel not established")
        val raw = frame.toBytes()
        val header = raw.copyOfRange(0, IletConstants.HEADER_BYTES)
        val payload = raw.copyOfRange(IletConstants.HEADER_BYTES, raw.size)
        val counterBytes = IletCodec.u32(counter.toLong() and 0xFFFFFFFFL)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, gcmNonce(counterBytes)))
        cipher.updateAAD(header)
        val sealed = cipher.doFinal(payload)
        val ciphertext = sealed.copyOfRange(0, sealed.size - IletConstants.GCM_TAG_BYTES)
        val tag = sealed.copyOfRange(sealed.size - IletConstants.GCM_TAG_BYTES, sealed.size)
        return IletBlePacket(IletCodec.concat(header, ciphertext), IletCodec.concat(counterBytes, tag))
    }

    private fun decrypt(packet: IletBlePacket): IletFrame {
        val key = sessionKey ?: throw IletNotEstablished("channel not established")
        val counterBytes = packet.oob.copyOfRange(0, 4)
        val tag = packet.oob.copyOfRange(4, minOf(4 + IletConstants.GCM_TAG_BYTES, packet.oob.size))
        if (tag.size != IletConstants.GCM_TAG_BYTES) {
            throw IletProtocolError(
                "encrypted packet carries a ${tag.size}-byte tag, expected ${IletConstants.GCM_TAG_BYTES}"
            )
        }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, gcmNonce(counterBytes)))
        cipher.updateAAD(packet.header)
        val payload = try {
            cipher.doFinal(IletCodec.concat(packet.body, tag))
        } catch (e: Exception) {
            throw IletSecureChannelError(
                "AES-GCM authentication failed. Either the session key is wrong or the message " +
                    "counter is out of step with the pump."
            )
        }
        return IletFrame.fromBytes(IletCodec.concat(packet.header, payload))
    }

    private fun gcmNonce(counter: ByteArray): ByteArray = IletCodec.concat(counter, ByteArray(8))

    companion object {
        private fun deriveSessionKey(shared: ByteArray, salt: ByteArray): ByteArray {
            val hkdf = HKDFBytesGenerator(SHA256Digest())
            hkdf.init(HKDFParameters(shared, salt, ByteArray(0)))
            val out = ByteArray(IletConstants.SESSION_KEY_BYTES)
            hkdf.generateBytes(out, 0, out.size)
            return out
        }

        /**
         * Plain HMAC-SHA256 over
         * `label || role_id || other_id || role_nonce || other_nonce`.
         * Not Tink-expanded.
         */
        fun macTag(
            sessionKey: ByteArray,
            label: ByteArray,
            roleIdentity: String,
            otherIdentity: String,
            roleNonce: ByteArray,
            otherNonce: ByteArray,
        ): ByteArray {
            val message = IletCodec.concat(
                label,
                roleIdentity.toByteArray(Charsets.US_ASCII),
                otherIdentity.toByteArray(Charsets.US_ASCII),
                roleNonce,
                otherNonce,
            )
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(sessionKey, "HmacSHA256"))
            return mac.doFinal(message)
        }

        fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
            if (a.size != b.size) return false
            var result = 0
            for (i in a.indices) result = result or (a[i].toInt() xor b[i].toInt())
            return result == 0
        }
    }
}
