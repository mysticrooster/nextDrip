package com.eveningoutpost.dexdrip.cgm.ilet.protocol

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Raised for protocol-level failures once the channel is up. */
open class IletClientError(message: String) : Exception(message)

/** Raised when the pump answers an establish-key with a status. */
class IletPumpRejectedHandshake(message: String) : IletClientError(message)

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

    suspend fun getRtcTime(): IletRtcTimeResponse =
        IletRtcTimeResponse.parse(request(IletMessages.getRtcTime(), IletOpcode.RSP_GET_RTC_TIME).payload)

    suspend fun getDeviceInfo(): IletDeviceInfoResponse =
        IletDeviceInfoResponse.parse(request(IletMessages.getDeviceInfo(), IletOpcode.RSP_GET_DEVICE_INFO).payload)

    suspend fun getDeviceInfoRaw(): ByteArray =
        request(IletMessages.getDeviceInfo(), IletOpcode.RSP_GET_DEVICE_INFO).payload

    // ------------------------------------------------------------ real-time

    suspend fun readRealtime(): IletRealTimeData = parseRealtime(transport.readRealtimeOnce())

    fun watchRealtime(): Flow<IletRealTimeData> = flow {
        transport.realtime().collect { packet ->
            try {
                emit(parseRealtime(packet))
            } catch (e: Exception) {
                // bad packets are skipped rather than terminating the iteration
            }
        }
    }

    private fun parseRealtime(packet: IletBlePacket): IletRealTimeData {
        val frame = if (packet.isEncrypted) activeChannel.open(packet) else packet.toFrame()
        if (frame.opcode != IletOpcode.REAL_TIME_DATA.value) {
            throw IletClientError("expected RealTimeData on the real-time channel, got ${frame.opcodeName}")
        }
        return IletRealTimeData.parse(frame.payload)
    }

    // ------------------------------------------------------------ 0x2508 / 0x2509

    suspend fun flushRecords(recordType: IletRecordType = IletRecordType.REPORT): IletRecordFlushResponse =
        IletRecordFlushResponse.parse(
            request(IletMessages.recordFlush(recordType), IletOpcode.RSP_RECORD_FLUSH).payload
        )

    suspend fun recordBounds(recordType: IletRecordType = IletRecordType.REPORT): IletRecordReadSeqResponse =
        IletRecordReadSeqResponse.parse(
            request(IletMessages.recordReadSeq(recordType), IletOpcode.RSP_RECORD_READ_SEQ).payload
        )

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

    suspend fun readRecordChunk(
        recordType: IletRecordType,
        sequenceNumber: Long,
        offset: Int,
        length: Int = IletConstants.MAX_CHUNK_BYTES,
    ): IletRecordReadResponse {
        val capped = minOf(length, IletConstants.MAX_CHUNK_BYTES)
        return IletRecordReadResponse.parse(
            request(
                IletMessages.recordRead(recordType, sequenceNumber, offset, capped),
                IletOpcode.RSP_RECORD_READ,
            ).payload
        )
    }

    /** Read one complete record by walking 1024-byte chunks. */
    suspend fun readRecord(
        recordType: IletRecordType,
        sequenceNumber: Long,
        maxBytes: Int = 1 shl 20,
        chunkSize: Int = IletConstants.MAX_CHUNK_BYTES,
    ): ByteArray {
        val out = ArrayList<Byte>()
        var offset = 0
        while (out.size < maxBytes) {
            val chunk = readRecordChunk(recordType, sequenceNumber, offset, chunkSize)
            if (!chunk.isOk) {
                throw IletClientError("record read failed at offset $offset: status ${chunk.status}")
            }
            if (chunk.data.isEmpty()) break
            for (b in chunk.data) out.add(b)
            offset += chunk.data.size
            if (chunk.data.size < chunkSize) break
        }
        return out.toByteArray()
    }

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

    suspend fun getHistoricalRecord(
        recordType: IletHistoricalRecordType,
        sequenceNumber: Long,
    ): IletGetHistoricalRecordResponse = IletGetHistoricalRecordResponse.parse(
        request(
            IletMessages.getHistoricalRecord(recordType, sequenceNumber),
            IletOpcode.RSP_GET_HISTORICAL_RECORD,
        ).payload
    )

    suspend fun patchBackFillHistory(
        recordType: IletHistoricalRecordType,
        fromSeq: Long,
        toSeq: Long,
    ): IletPatchBackFillHistoryResponse = IletPatchBackFillHistoryResponse.parse(
        request(
            IletMessages.patchBackFillHistory(recordType, fromSeq, toSeq),
            IletOpcode.RSP_PATCH_BACK_FILL_HISTORY,
            IletConstants.TIMEOUT_LONG,
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
