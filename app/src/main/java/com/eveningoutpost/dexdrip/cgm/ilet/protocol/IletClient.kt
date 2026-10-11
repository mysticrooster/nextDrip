package com.eveningoutpost.dexdrip.cgm.ilet.protocol

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** How long to wait for the next 0x2333 sector frame before ending the stream. */
private const val BACKFILL_FRAME_TIMEOUT_MS = 2_000L

/** Raised for protocol-level failures once the channel is up. */
open class IletClientError(message: String) : Exception(message)

/** Raised when the pump answers an establish-key with a status. */
class IletPumpRejectedHandshake(message: String) : IletClientError(message)

/** A real-time packet, which the pump may send as 0x1506 or the 0x1515 superset. */
sealed class IletRealtimeSnapshot {
    data class Standard(val data: IletRealTimeData) : IletRealtimeSnapshot()
    data class Control(val data: IletRealTimeControlPumpData) : IletRealtimeSnapshot()
}

/**
 * Read-only client for the Beta Bionics iLet pump. Ported from `ilet/client.py`.
 *
 * The client exposes no write path beyond the handshake. There is no bolus, no
 * basal change, and no way to alter therapy.
 */
class IletClient(
    private val transport: IletTransport,
    private val store: IletIdentityStore,
    private val signer: IletSigner? = null,
) {

    private var channel: IletSecureChannel? = null
    private val lock = Mutex()

    private val noResponseValues: Set<Int> = NO_RESPONSE_OPCODES.map { it.value }.toSet()

    val activeChannel: IletSecureChannel
        get() = channel ?: throw IletClientError("not connected; call connect() first")

    // ------------------------------------------------------------ lifecycle

    /**
     * Open the BLE link and run the secure-channel handshake, reusing the
     * stored identity. With [allowReenrol] a fresh identity is minted when the
     * pump rejects the stored credential.
     */
    suspend fun connect(allowReenrol: Boolean = true) {
        var (identity, freshlyMinted) = resolveIdentity()
        ensureLink()
        try {
            handshake(identity)
        } catch (e: IletPumpRejectedHandshake) {
            if (freshlyMinted || !allowReenrol) throw e
            transport.disconnect()
            store.clearIdentity()
            val resolved = resolveIdentity()
            identity = resolved.first
            ensureLink()
            handshake(identity)
        }
        store.storeIdentity(identity)
    }

    suspend fun disconnect() {
        transport.disconnect()
        channel?.reset()
    }

    private suspend fun ensureLink() {
        if (!transport.isConnected) transport.connect()
    }

    private suspend fun resolveIdentity(): Pair<IletChannelIdentity, Boolean> {
        val cached = store.loadIdentity()
        if (cached != null) return cached to false

        val signer = this.signer ?: throw IletClientError(
            "no credential in the store and no signer configured. The pump will not open a " +
                "channel without a cloud signature -- supply an IletSigner, or pre-populate the store."
        )

        val draft = IletChannelIdentity.generate(cloudSignature = ByteArray(64), appUuid = store.appUuid())
        val signature = signer.sign(draft.signingInput())
        val identity = IletChannelIdentity(
            appUuid = draft.appUuid,
            privateKey = draft.privateKey,
            nonce = draft.nonce,
            cloudSignature = signature,
        )
        store.storeIdentity(identity)
        return identity to true
    }

    private suspend fun handshake(identity: IletChannelIdentity) {
        val channel = IletSecureChannel(identity)
        if (!transport.isConnected) {
            throw IletTransportError("the link dropped before the handshake could start.")
        }
        transport.writeTrigger()

        val establish = exchange(
            channel,
            channel.establishKeyFrame(),
            expect = setOf(
                IletOpcode.RSP_SECURE_CHANNEL_ESTABLISH_KEY.value,
                IletOpcode.RSP_SECURE_CHANNEL_STATUS.value,
            ),
        )
        if (establish.opcode == IletOpcode.RSP_SECURE_CHANNEL_STATUS.value) {
            throw IletPumpRejectedHandshake("pump rejected establish-key: ${statusOf(establish.payload)}")
        }
        val peer = IletPeerIdentity.parse(establish.payload)
        channel.derive(peer)

        val confirm = exchange(
            channel,
            channel.confirmKeyFrame(),
            expect = setOf(
                IletOpcode.RSP_SECURE_CHANNEL_CONFIRM_KEY.value,
                IletOpcode.RSP_SECURE_CHANNEL_STATUS.value,
            ),
        )
        val confirmParsed = parseConfirm(confirm.payload)
        if (confirmParsed.status != 0) {
            throw IletPumpRejectedHandshake("confirm-key returned ${IletSecureChannelStatus.nameOf(confirmParsed.status)}")
        }
        channel.adoptCounter(confirmParsed.counterU)
        this.channel = channel
    }

    // ------------------------------------------------------------ dispatch

    private suspend fun exchange(
        channel: IletSecureChannel,
        frame: IletFrame,
        expect: Set<Int>,
        timeoutMs: Long = IletConstants.TIMEOUT_DEFAULT,
    ): IletFrame {
        val packet = channel.seal(frame)
        transport.send(packet, expectResponse = frame.opcode !in noResponseValues)

        val deadline = System.currentTimeMillis() + timeoutMs
        while (true) {
            val remaining = deadline - System.currentTimeMillis()
            if (remaining <= 0) {
                throw IletTransportError("no expected reply to ${frame.opcodeName} within ${timeoutMs}ms")
            }
            val reply = channel.open(transport.nextResponse(remaining))
            if (reply.opcode in expect) return reply
            if (reply.opcode == IletOpcode.ERROR.value || reply.opcode == IletOpcode.RSP_INVALID_COMMAND.value) {
                throw IletClientError("${frame.opcodeName} refused: ${reply.opcodeName}")
            }
            // Otherwise it is unsolicited (e.g. an event); keep waiting.
        }
    }

    /** Send one command and wait for exactly one reply of [expect]. */
    suspend fun request(frame: IletFrame, expect: IletOpcode, timeoutMs: Long = IletConstants.TIMEOUT_DEFAULT): IletFrame =
        lock.withLock { exchange(activeChannel, frame, setOf(expect.value), timeoutMs) }

    /** Send one command and emit every reply of [expect] until the pump goes quiet. */
    fun requestStream(frame: IletFrame, expect: IletOpcode, timeoutMs: Long = IletConstants.TIMEOUT_LONG): Flow<IletFrame> = flow {
        lock.withLock {
            val channel = activeChannel
            transport.send(channel.seal(frame))
            while (true) {
                val packet = try {
                    transport.nextResponse(timeoutMs)
                } catch (e: IletTransportError) {
                    return@withLock
                }
                val reply = channel.open(packet)
                when (reply.opcode) {
                    expect.value -> emit(reply)
                    IletOpcode.ERROR.value, IletOpcode.RSP_INVALID_COMMAND.value ->
                        throw IletClientError("${frame.opcodeName} refused: ${reply.opcodeName}")
                }
            }
        }
    }

    // ------------------------------------------------------------ info / time

    suspend fun getTime(): IletGetTimeResponse =
        IletGetTimeResponse.parse(request(IletMessages.getTime(), IletOpcode.RSP_GET_TIME).payload)

    suspend fun getDeviceInfo(): IletDeviceInfoResponse =
        IletDeviceInfoResponse.parse(request(IletMessages.getDeviceInfo(), IletOpcode.RSP_GET_DEVICE_INFO).payload)

    suspend fun getDeviceInfoRaw(): ByteArray =
        request(IletMessages.getDeviceInfo(), IletOpcode.RSP_GET_DEVICE_INFO).payload

    // ------------------------------------------------------------ real-time

    /**
     * Read one real-time packet, accepting both the 0x1506 stream and the 0x1515
     * control superset. The pump emits 0x1515 on the same characteristic; it carries
     * meal-announcement state and the pump-patch (infusion set) session window.
     */
    suspend fun readRealtimeSnapshot(): IletRealtimeSnapshot =
        parseRealtimeSnapshot(transport.readRealtimeOnce())

    private fun parseRealtimeSnapshot(packet: IletBlePacket): IletRealtimeSnapshot {
        val frame = if (packet.isEncrypted) activeChannel.open(packet) else packet.toFrame()
        return when (frame.opcode) {
            IletOpcode.REAL_TIME_DATA.value ->
                IletRealtimeSnapshot.Standard(IletRealTimeData.parse(frame.payload))
            IletOpcode.REAL_TIME_CONTROL_PUMP_DATA.value ->
                IletRealtimeSnapshot.Control(IletRealTimeControlPumpData.parse(frame.payload))
            else -> throw IletClientError("unexpected real-time opcode ${frame.opcodeName}")
        }
    }

    /**
     * Read the live sequence bounds for a historical partition (0x1335/0x2335).
     * Uses [IletHistoricalRecordType] numbering.
     */
    suspend fun sequenceNumberHistory(
        recordType: IletHistoricalRecordType,
    ): IletSequenceNumberHistoryResponse = IletSequenceNumberHistoryResponse.parse(
        request(
            IletMessages.getSequenceNumberHistory(recordType),
            IletOpcode.RSP_GET_SEQUENCE_NUMBER_HISTORY,
        ).payload
    )

    // ------------------------------------------------------------ 0x1333 / 0x1334 / 0x1380

    fun backFillHistory(
        recordType: IletHistoricalRecordType,
        fromSeq: Long,
        toSeq: Long,
        timeoutMs: Long = IletConstants.TIMEOUT_LONG,
    ): Flow<IletBackFillHistoryResponse> = flow {
        val frame = IletMessages.backFillHistory(recordType, fromSeq, toSeq)
        requestStream(frame, IletOpcode.RSP_BACK_FILL_HISTORY, timeoutMs).collect {
            emit(IletBackFillHistoryResponse.parse(it.payload))
        }
    }

    /**
     * Batched history read: request a record-sequence range and reassemble the
     * 4096-byte sector(s) the pump streams back. Far fewer round-trips than one
     * 0x2334 request per record. Stops once the pump goes quiet for [frameTimeoutMs].
     */
    suspend fun readBackfillSectors(
        recordType: IletHistoricalRecordType,
        fromSeq: Long,
        toSeq: Long,
        frameTimeoutMs: Long = BACKFILL_FRAME_TIMEOUT_MS,
    ): List<ByteArray> {
        val frames = ArrayList<IletBackFillHistoryResponse>()
        lock.withLock {
            val channel = activeChannel
            transport.send(channel.seal(IletMessages.backFillHistory(recordType, fromSeq, toSeq)))
            while (true) {
                val reply = try {
                    channel.open(transport.nextResponse(frameTimeoutMs))
                } catch (e: IletTransportError) {
                    break // pump went quiet: end of stream
                }
                when (reply.opcode) {
                    IletOpcode.RSP_BACK_FILL_HISTORY.value ->
                        frames.add(IletBackFillHistoryResponse.parse(reply.payload))
                    IletOpcode.ERROR.value, IletOpcode.RSP_INVALID_COMMAND.value ->
                        throw IletClientError("backFillHistory refused: ${reply.opcodeName}")
                    else -> { /* unsolicited event: ignore */ }
                }
            }
        }
        return IletBackFillAssembler.assemble(frames)
    }

    suspend fun getHistoricalRecord(
        recordType: IletHistoricalRecordType,
        sequenceNumber: Long,
    ): IletGetHistoricalRecordResponse = IletGetHistoricalRecordResponse.parse(
        request(
            IletMessages.getHistoricalRecord(recordType, sequenceNumber),
            IletOpcode.RSP_GET_HISTORICAL_RECORD,
        ).payload
    )

    // ------------------------------------------------------------ helpers

    private class ConfirmReply(val status: Int, val counterU: Int, val counterV: Int)

    private fun parseConfirm(payload: ByteArray): ConfirmReply {
        if (payload.size < 9) {
            throw IletSecureChannelError("confirm-key reply is ${payload.size} bytes, expected 9")
        }
        return ConfirmReply(
            status = IletCodec.readU8(payload, 0),
            counterU = IletCodec.readU32(payload, 1).toInt(),
            counterV = IletCodec.readU32(payload, 5).toInt(),
        )
    }

    private fun statusOf(payload: ByteArray): String =
        if (payload.isEmpty()) "empty status payload" else IletSecureChannelStatus.nameOf(IletCodec.readU8(payload, 0))
}
