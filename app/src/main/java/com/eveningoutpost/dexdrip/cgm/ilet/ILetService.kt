package com.eveningoutpost.dexdrip.cgm.ilet

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.eveningoutpost.dexdrip.cgm.ilet.ble.IletTransport
import com.eveningoutpost.dexdrip.cgm.ilet.cloud.IletCloudSigner
import com.eveningoutpost.dexdrip.cgm.ilet.cloud.IletCredentialStore
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletAlgoStepRecord
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletHistoricalGlucoseRecord
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletHistoricalInsulinRecord
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletHistoricalRecordType
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletClient
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletProtocolError
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletPumpRejectedHandshake
import com.eveningoutpost.dexdrip.models.JoH
import com.eveningoutpost.dexdrip.models.UserError
import com.eveningoutpost.dexdrip.services.JamBaseBluetoothService
import com.eveningoutpost.dexdrip.utilitymodels.Constants
import com.eveningoutpost.dexdrip.utils.DexCollectionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Read-only iLet collector and pump service.
 *
 * In collector mode it writes BgReading rows from the pump's CGM; in pump-only
 * mode it still reads boluses/basal/IOB but suppresses glucose writes. The BLE
 * link is short-lived (the pump drops it after ~2.5 s idle), so the service
 * reconnects and backfills on each cycle.
 */
class ILetService : JamBaseBluetoothService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var worker: Job? = null
    private val processor = IletDataProcessor()

    private val store: IletCredentialStore by lazy { IletCredentialStore.getInstance(this) }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (worker?.isActive != true) {
            worker = scope.launch { runLoop() }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        instance = null
        lastState = "Not Running"
        super.onDestroy()
    }

    private suspend fun runLoop() {
        instance = this
        while (scope.isActive) {
            if (!shouldServiceRun()) {
                lastState = "Stopped"
                stopSelf()
                return
            }
            try {
                collectOnce()
            } catch (t: Throwable) {
                lastState = "Error: ${t.message}"
                UserError.Log.e(TAG, "iLet cycle failed: ${t.message}")
            }
            if (!shouldServiceRun()) {
                lastState = "Stopped"
                stopSelf()
                return
            }
            delay(RETRY_DELAY_MS)
        }
    }

    private suspend fun collectOnce() {
        lastState = "Connecting"
        val address = resolveAddress() ?: throw IllegalStateException("no iLet address; pair or scan first")

        val transport = IletTransport(address)
        val client = IletClient(transport, store, IletCloudSigner(store))
        val reenrol = IletPrefs.canReenrol()
        try {
            client.connect(allowReenrol = reenrol)
            IletPrefs.resetReenrolAttempts()
        } catch (e: IletPumpRejectedHandshake) {
            // Only re-enrol (clear + mint + cloud-sign) on the permitted cycle, so a
            // persistently rejected credential does not hit the cloud every 15s.
            if (reenrol) IletPrefs.noteReenrolAttempt()
            throw e
        }
        try {
            lastInteractionTime = System.currentTimeMillis()

            val info = client.getDeviceInfo()
            IletPrefs.setDeviceSerial(info.deviceSerial)
            IletPrefs.setDeviceModel(info.modelId)

            val time = client.getTime()
            processor.updateEpoch(time.clockOffset)
            lastState = "Running"

            val realtimeJob = scope.launch {
                client.watchRealtime().collect { reading ->
                    processor.processRealtime(reading)
                    lastInteractionTime = System.currentTimeMillis()
                }
            }
            try {
                backfill(client)
            } finally {
                realtimeJob.cancelAndJoin()
            }
        } finally {
            client.disconnect()
        }
    }

    /** Walk newer historical sequences for glucose, insulin and algorithm records. */
    private suspend fun backfill(client: IletClient) {
        backfillType(
            client,
            IletHistoricalRecordType.GLUCOSE,
            watermark = { IletPrefs.watermarkGlucose() },
            setWatermark = { IletPrefs.setWatermarkGlucose(it) },
        )
        backfillType(
            client,
            IletHistoricalRecordType.INSULIN,
            watermark = { IletPrefs.watermarkInsulin() },
            setWatermark = { IletPrefs.setWatermarkInsulin(it) },
        )
        backfillType(
            client,
            IletHistoricalRecordType.ALGORITHM_DATA,
            watermark = { IletPrefs.watermarkAlgo() },
            setWatermark = { IletPrefs.setWatermarkAlgo(it) },
        )
    }

    private suspend fun backfillType(
        client: IletClient,
        type: IletHistoricalRecordType,
        watermark: () -> Long,
        setWatermark: (Long) -> Unit,
    ) {
        val bounds = try {
            client.sequenceNumberHistory(type)
        } catch (t: Throwable) {
            // 0x2335 is the confirmed bounds primitive; 0x2381 is unsupported on
            // current firmware (no reply, link drop), so we never depend on it.
            UserError.Log.d(TAG, "bounds for $type unavailable: ${t.message}")
            return
        }
        if (!bounds.isOk) return
        val min = bounds.oldestSequenceNumber
        val max = bounds.newestSequenceNumber
        if (max <= 0 || max < min) return

        val previous = watermark()
        val from = if (previous < 0) {
            // First run: bound the look-back so a fresh install does not replay the
            // pump's entire retained history into BgReading/Treatments/APStatus.
            maxOf(min, max - FIRST_RUN_BACKFILL_RECORDS)
        } else {
            maxOf(min, previous + 1)
        }
        if (from > max) return

        var counter = 0
        var highest = if (previous < 0) from - 1 else previous
        for (seq in from..max) {
            if (counter++ >= MAX_BACKFILL_PER_CYCLE) break
            try {
                val response = client.getHistoricalRecord(type, seq)
                if (!response.isOk) {
                    highest = maxOf(highest, seq)
                    continue
                }
                when (val record = response.record()) {
                    is IletHistoricalGlucoseRecord -> processor.processHistoricalGlucose(record)
                    is IletHistoricalInsulinRecord -> processor.processHistoricalInsulin(record)
                    is IletAlgoStepRecord -> processor.processAlgoStep(record)
                }
                highest = maxOf(highest, seq)
            } catch (e: IletProtocolError) {
                // A corrupt/unknown record must not stall the walk forever.
                UserError.Log.wtf(TAG, "backfill $type seq $seq unparseable: ${e.message}")
                highest = maxOf(highest, seq)
            } catch (t: Throwable) {
                // Transport-level failure: stop this cycle and retry from the watermark.
                UserError.Log.e(TAG, "backfill $type seq $seq failed: ${t.message}")
                break
            }
        }
        setWatermark(highest)
    }

    private fun resolveAddress(): String? {
        val remembered = IletPrefs.lastAddress()
        if (remembered.isNotEmpty()) return remembered
        val scanned = IletScanner.scanForAddress()
        if (scanned != null) IletPrefs.setLastAddress(scanned)
        return scanned
    }

    private fun shouldServiceRun(): Boolean {
        if (!IletPrefs.isEnabled()) return false
        return IletPrefs.isCollectorSelected() || IletPrefs.isPumpOnly()
    }

    companion object {
        private const val RETRY_DELAY_MS = 15_000L
        private const val MAX_BACKFILL_PER_CYCLE = 400

        /** First-run look-back cap: 24h at a 5-minute cadence. */
        private const val FIRST_RUN_BACKFILL_RECORDS = 288

        @Volatile
        @JvmStatic
        var lastState = "Not Running"

        @Volatile
        @JvmStatic
        var lastInteractionTime = 0L

        @Volatile
        private var instance: ILetService? = null

        // accessed via reflection by DexCollectionType
        @JvmStatic
        fun isRunning(): Boolean = instance != null && lastState != "Not Running" && !lastState.startsWith("Stop")

        // accessed via reflection by DexCollectionType
        @JvmStatic
        fun isCollecting(): Boolean = JoH.msSince(lastInteractionTime) < Constants.MINUTE_IN_MS * 5

        // accessed via reflection by DexCollectionType
        @JvmStatic
        fun isWatchRunning(): Boolean = false

        @JvmStatic
        fun setWakeLock(context: Context, timeoutMs: Long) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val lock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "xdrip:ilet")
            lock.acquire(timeoutMs)
        }

        /** Start the service when iLet is enabled and the collector is selected. */
        @JvmStatic
        fun startIfEnabled(context: Context) {
            if (IletPrefs.isEnabled() && DexCollectionType.getDexCollectionType() == DexCollectionType.ILet) {
                JoH.startService(ILetService::class.java)
            }
        }
    }
}
