package com.eveningoutpost.dexdrip.cgm.ilet.protocol

import kotlinx.coroutines.flow.Flow

/** Raised for transport-level failures (link drop, timeout, wedged GATT). */
class IletTransportError(message: String) : Exception(message)

/**
 * Transport contract the iLet client talks to. The Android implementation lives
 * in `cgm/ilet/ble/IletTransport`, but keeping the interface pure-JVM lets the
 * client and handshake be tested without a device.
 */
interface IletTransport {

    val isConnected: Boolean

    suspend fun connect()

    suspend fun disconnect()

    /** Write the 0-byte trigger that opens the establish-key window. */
    suspend fun writeTrigger()

    /**
     * Write the frame and its OOB trailer. With `expectResponse = false` the
     * implementation must keep the frame ahead of the OOB on the ATT layer
     * (a short floor after the frame write) or the pump drops the link.
     */
    suspend fun send(packet: IletBlePacket, expectResponse: Boolean = true)

    /** Await the next framed response; throws [IletTransportError] on timeout. */
    suspend fun nextResponse(timeoutMs: Long): IletBlePacket

    /** Completed packets from the real-time characteristic. */
    fun realtime(): Flow<IletBlePacket>

    /** One direct GATT read of the real-time characteristic, if supported. */
    suspend fun readRealtimeOnce(): IletBlePacket
}

/**
 * Persistent credential storage. Implemented by `cloud/IletCredentialStore`.
 * The identity is a medical-device key and must never be logged.
 */
interface IletIdentityStore {
    fun loadIdentity(): IletChannelIdentity?
    fun storeIdentity(identity: IletChannelIdentity)
    fun clearIdentity()
    fun appUuid(): java.util.UUID
}

/** Signs the 96-byte enrolment blob. Implemented by `cloud/IletCloudSigner`. */
interface IletSigner {
    suspend fun sign(data: ByteArray): ByteArray
}
