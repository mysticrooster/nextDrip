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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * RxAndroidBle transport for the iLet pump.
 *
 * Mirrors the verified Python transport: subscribe to the body and OOB response
 * characteristics and reassemble chunks terminated by an OOB, keep the frame
 * ahead of its OOB on the ATT layer, and subscribe to the real-time stream.
 * Chunk writes are kept at MTU-3; the pump drops the link on an out-of-order
 * OOB or a long-write prepare/execute sequence.
 */
class IletTransport(
    private val address: String,
    private val client: RxBleClient = RxBleProvider.getSingleton(),
) : IletTransport {

    private val device: RxBleDevice get() = client.getBleDevice(address)

    private val disposables = CompositeDisposable()
    private val responseChannel = Channel<IletBlePacket>(Channel.UNLIMITED)
    private val realtimeChannel = Channel<IletBlePacket>(Channel.UNLIMITED)

    private var bodyBuffer = ByteArray(1024)
    private var bodyLength = 0

    @Volatile
    private var connection: RxBleConnection? = null

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
        val ready = CompletableDeferred<RxBleConnection>()
        disposables.add(
            device.establishConnection(false).subscribe(
                { conn -> ready.complete(conn) },
                { error -> ready.completeExceptionally(IletTransportError("connect failed: ${error.message}")) },
            )
        )
        val conn = ready.await()
        mtu = try {
            conn.requestMtu(247).timeout(5, TimeUnit.SECONDS).blockingGet()
        } catch (e: Exception) {
            23
        }
        connection = conn
        subscribeNotifications(conn)
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
        disposables.add(
            conn.setupNotification(IletConstants.CHAR_REALTIME).flatMap { it }
                .subscribe({ onRealtime(it) }, { onDropped() })
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
    }

    @Synchronized
    private fun onOob(oob: ByteArray) {
        if (bodyLength == 0) return
        val frame = bodyBuffer.copyOf(bodyLength)
        bodyLength = 0
        responseChannel.trySend(IletBlePacket(frame, oob))
    }

    private fun onRealtime(value: ByteArray) {
        try {
            realtimeChannel.trySend(splitRealtimeNotification(value))
        } catch (e: Exception) {
            // ignore malformed notifications
        }
    }

    override suspend fun disconnect() {
        withContext(Dispatchers.IO) {
            disposables.clear()
            connection = null
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

    override fun realtime(): Flow<IletBlePacket> = flow {
        while (true) {
            emit(realtimeChannel.receive())
        }
    }

    override suspend fun readRealtimeOnce(): IletBlePacket = withContext(Dispatchers.IO) {
        val conn = connection ?: throw IletTransportError("not connected")
        val data = conn.readCharacteristic(IletConstants.CHAR_REALTIME).timeout(10, TimeUnit.SECONDS).blockingGet()
        splitRealtimeNotification(data)
    }

    private companion object {
        const val TAG = "iLetTransport"
    }
}
