package com.eveningoutpost.dexdrip.cgm.ilet.ble

import android.bluetooth.BluetoothDevice
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletBlePacket
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletConstants
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletTransport
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletTransportError
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.splitRealtimeNotification
import com.eveningoutpost.dexdrip.models.UserError
import com.eveningoutpost.dexdrip.utilitymodels.RxBleProvider
import com.polidea.rxandroidble2.RxBleClient
import com.polidea.rxandroidble2.RxBleConnection
import com.polidea.rxandroidble2.RxBleDevice
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * RxAndroidBle transport for the iLet pump.
 *
 * Mirrors the verified Python transport: subscribe to the body and OOB response
 * characteristics and reassemble chunks terminated by an OOB, keep the frame
 * ahead of its OOB on the ATT layer, and read the real-time characteristic on
 * demand. Chunk writes are kept at MTU-3; the pump drops the link on an
 * out-of-order OOB or a long-write prepare/execute sequence.
 */
class IletTransport(
    private val address: String,
    private val client: RxBleClient = RxBleProvider.getSingleton(),
) : IletTransport {

    private val device: RxBleDevice get() = client.getBleDevice(address)

    private val disposables = CompositeDisposable()
    private val responseChannel = Channel<IletBlePacket>(Channel.UNLIMITED)

    private var bodyBuffer = ByteArray(1024)
    private var bodyLength = 0

    // The body and OOB characteristics are two separate notification streams, so their
    // callbacks can arrive out of order (OOB before the trailing body chunk). Hold an
    // early OOB until the frame reaches the length in its cleartext header, otherwise a
    // truncated frame is produced and AES-GCM fails. Multi-frame streams (0x2333) hit this.
    private var pendingOob: ByteArray? = null

    @Volatile
    private var connection: RxBleConnection? = null

    /** The in-flight/live establishConnection subscription; disposing it closes the GATT. */
    @Volatile
    private var connectionDisposable: Disposable? = null

    @Volatile
    private var mtu: Int = 23

    override val isConnected: Boolean
        get() = device.connectionState == RxBleConnection.RxBleConnectionState.CONNECTED

    /** MTU-3, floored at 20, matching the Python chunk size. */
    val chunkSize: Int get() = maxOf(mtu - 3, 20)

    override suspend fun connect() = withContext(Dispatchers.IO) {
        if (isConnected && connection != null) return@withContext
        // The pump link must be bonded/encrypted; an unbonded link would let a
        // spoofing peripheral feed forged data through the cleartext handshake.
        if (!ensureBonded()) {
            throw IletTransportError("iLet $address is not bonded; refusing to connect")
        }
        var lastError: Throwable? = null
        for (attempt in 0 until CONNECT_ATTEMPTS) {
            try {
                val conn = establishOnce()
                mtu = try {
                    conn.requestMtu(247).timeout(5, TimeUnit.SECONDS).blockingGet()
                } catch (e: Exception) {
                    23
                }
                connection = conn
                subscribeNotifications(conn)
                return@withContext
            } catch (t: Throwable) {
                lastError = t
                UserError.Log.w(TAG, "connect attempt ${attempt + 1}/$CONNECT_ATTEMPTS failed: ${t.message}")
                // GATT 133 (GATT_ERROR) is transient: a stale link or a busy controller.
                // Dropping the half-open attempt and waiting a moment before retrying clears
                // it far more often than retrying immediately.
                closeAttempt()
                if (attempt < CONNECT_ATTEMPTS - 1) delay(RETRY_DELAYS_MS[attempt])
            }
        }
        throw IletTransportError("connect failed after $CONNECT_ATTEMPTS attempts: ${lastError?.message}")
    }

    /** One establishConnection attempt, bounded by [CONNECT_TIMEOUT_MS]. */
    private suspend fun establishOnce(): RxBleConnection {
        val ready = CompletableDeferred<RxBleConnection>()
        connectionDisposable = device.establishConnection(false).subscribe(
            { conn -> ready.complete(conn) },
            { error -> ready.completeExceptionally(IletTransportError("connect failed: ${error.message}")) },
        )
        return withTimeoutOrNull(CONNECT_TIMEOUT_MS) { ready.await() }
            ?: throw IletTransportError("connect timed out after ${CONNECT_TIMEOUT_MS}ms")
    }

    /** Drop the in-flight attempt so the next one starts from a clean controller state. */
    private fun closeAttempt() {
        try {
            connectionDisposable?.dispose()
        } catch (t: Throwable) {
            // ignore
        }
        connectionDisposable = null
        connection = null
    }

    /** Ensure the pump is bonded, prompting the OS to bond when needed. */
    private suspend fun ensureBonded(timeoutMs: Long = 10_000): Boolean {
        val bluetoothDevice = try {
            device.bluetoothDevice
        } catch (t: Throwable) {
            null
        } ?: return false
        if (bluetoothDevice.bondState == BluetoothDevice.BOND_BONDED) return true
        try {
            if (bluetoothDevice.bondState != BluetoothDevice.BOND_BONDING) {
                bluetoothDevice.createBond()
            }
        } catch (t: Throwable) {
            UserError.Log.e(TAG, "createBond failed for $address: ${t.message}")
            return false
        }
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (bluetoothDevice.bondState == BluetoothDevice.BOND_BONDED) return true
            delay(250)
        }
        return bluetoothDevice.bondState == BluetoothDevice.BOND_BONDED
    }

    private fun subscribeNotifications(conn: RxBleConnection) {
        disposables.add(
            conn.setupNotification(IletConstants.CHAR_RESP_BODY).flatMap { it }
                .subscribe({ onBody(it) }, { onDropped() })
        )
        disposables.add(
            conn.setupNotification(IletConstants.CHAR_RESP_OOB).flatMap { it }
                .subscribe({ onOob(it) }, { onDropped() })
        )
    }

    private fun onDropped() {
        responseChannel.trySend(IletBlePacket(ByteArray(0), ByteArray(0)))
    }

    @Synchronized
    private fun onBody(value: ByteArray) {
        val needed = bodyLength + value.size
        if (bodyBuffer.size < needed) {
            bodyBuffer = bodyBuffer.copyOf(maxOf(needed, bodyBuffer.size * 2))
        }
        System.arraycopy(value, 0, bodyBuffer, bodyLength, value.size)
        bodyLength += value.size
        val oob = pendingOob
        if (oob != null && isFrameComplete()) {
            pendingOob = null
            emitPacket(oob)
        }
    }

    @Synchronized
    private fun onOob(oob: ByteArray) {
        if (bodyLength == 0) return
        if (!isFrameComplete()) {
            pendingOob = oob // trailing body chunk has not arrived yet
            return
        }
        emitPacket(oob)
    }

    private fun emitPacket(oob: ByteArray) {
        val frame = bodyBuffer.copyOf(bodyLength)
        bodyLength = 0
        responseChannel.trySend(IletBlePacket(frame, oob))
    }

    /** True when the buffered body has reached the length in the cleartext header. */
    private fun isFrameComplete(): Boolean {
        if (bodyLength < 4) return false
        val declared = (bodyBuffer[2].toInt() and 0xFF) or ((bodyBuffer[3].toInt() and 0xFF) shl 8)
        return declared <= 4 || bodyLength >= declared
    }

    override suspend fun disconnect() {
        withContext(Dispatchers.IO) {
            try {
                connectionDisposable?.dispose()
            } catch (t: Throwable) {
                // ignore
            }
            connectionDisposable = null
            disposables.clear()
            connection = null
            pendingOob = null
            bodyLength = 0
            // Drop any packets left from this link so the next cycle's handshake cannot
            // read a stale response.
            while (responseChannel.tryReceive().isSuccess) {
                // discard
            }
        }
    }

    override suspend fun writeTrigger() {
        withContext(Dispatchers.IO) {
            val conn = connection ?: throw IletTransportError("not connected")
            try {
                conn.writeCharacteristic(IletConstants.CHAR_TRIGGER, ByteArray(0))
                    .timeout(1, TimeUnit.SECONDS).blockingGet()
            } catch (e: Exception) {
                // The ATT write is emitted even when the callback is lost; proceed.
            }
            delay(IletConstants.WRITE_ORDERING_FLOOR_MS)
        }
    }

    override suspend fun send(packet: IletBlePacket, expectResponse: Boolean) {
        withContext(Dispatchers.IO) {
            val conn = connection ?: throw IletTransportError("not connected")
            writeAll(conn, IletConstants.CHAR_CMD_FRAME, packet.frame)
            // A Write Command returns before the bytes reach the ATT layer; the OOB
            // would race ahead and the pump would drop the link. Keep a floor.
            if (!expectResponse) delay(IletConstants.WRITE_ORDERING_FLOOR_MS)
            writeAll(conn, IletConstants.CHAR_CMD_OOB, packet.oob)
        }
    }

    private fun writeAll(conn: RxBleConnection, characteristic: UUID, data: ByteArray) {
        val size = chunkSize
        var offset = 0
        while (offset < data.size) {
            val end = minOf(offset + size, data.size)
            val chunk = data.copyOfRange(offset, end)
            conn.writeCharacteristic(characteristic, chunk).timeout(30, TimeUnit.SECONDS).blockingGet()
            offset = end
        }
    }

    override suspend fun nextResponse(timeoutMs: Long): IletBlePacket {
        val packet = withTimeoutOrNull(timeoutMs) { responseChannel.receive() }
        if (packet == null || packet.frame.isEmpty()) {
            throw IletTransportError("no response within ${timeoutMs}ms")
        }
        return packet
    }

    override suspend fun readRealtimeOnce(): IletBlePacket = withContext(Dispatchers.IO) {
        val conn = connection ?: throw IletTransportError("not connected")
        val data = conn.readCharacteristic(IletConstants.CHAR_REALTIME).timeout(10, TimeUnit.SECONDS).blockingGet()
        splitRealtimeNotification(data)
    }

    private companion object {
        const val TAG = "iLetTransport"

        /** Establish attempts per cycle, so a transient GATT 133 (GATT_ERROR) self-heals. */
        const val CONNECT_ATTEMPTS = 3
        const val CONNECT_TIMEOUT_MS = 20_000L
        val RETRY_DELAYS_MS = longArrayOf(1_500L, 3_000L)
    }
}
