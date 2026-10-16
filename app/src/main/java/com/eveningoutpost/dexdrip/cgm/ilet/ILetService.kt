package com.eveningoutpost.dexdrip.cgm.ilet

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.eveningoutpost.dexdrip.cgm.ilet.ble.IletTransport
import com.eveningoutpost.dexdrip.cgm.ilet.cloud.IletCloudSigner
import com.eveningoutpost.dexdrip.cgm.ilet.cloud.IletCredentialStore
import com.eveningoutpost.dexdrip.cgm.ilet.cloud.IletSessionKeeper
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletAlarmHistory
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletAlgoStepRecord
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletHistoricalGlucoseRecord
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletHistoricalInsulinRecord
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletHistoricalRecordType
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletClient
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletProtocolError
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletPumpRejectedHandshake
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletSector
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletSequenceNumberHistoryResponse
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

    /** Conflated wake-up signal so a user request can cut the inter-cycle delay short. */
    private val wakeups = kotlinx.coroutines.channels.Channel<Unit>(kotlinx.coroutines.channels.Channel.CONFLATED)

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

    private fun setState(state: String) {
        lastState = state
        IletStatus.updateState(state)
    }

    private suspend fun runLoop() {
        instance = this
        while (scope.isActive) {
            if (!shouldServiceRun()) {
                stopForDisabled()
                return
            }
            var failed = false
            try {
                collectOnce()
            } catch (t: Throwable) {
                failed = true
                setState("Error: ${t.message}")
                UserError.Log.e(TAG, "iLet cycle failed: ${t.message}")
            }
            if (!shouldServiceRun()) {
                stopForDisabled()
                return
            }
            // Healthy cycles idle longer; a failure retries quickly. A user request
            // (wakeups) still cuts the wait short.
            val interval = if (failed) RETRY_DELAY_MS else IDLE_INTERVAL_MS
            kotlinx.coroutines.withTimeoutOrNull(interval) { wakeups.receive() }
        }
    }

    /** Service stop path: drop any pending forward-fill so a stale flag cannot linger. */
    private fun stopForDisabled() {
        IletPrefs.setForwardFillActive(false)
        IletPrefs.setForwardFillStalledCycles(0)
        setState("Stopped")
        stopSelf()
    }

    private suspend fun collectOnce() {
        setState("Connecting")

        // Keep the Cognito session fresh so a later re-enrolment never fails signed-out. This
        // only hits the network when the id token is expired or close to expiry.
        try {
            IletSessionKeeper(store).refreshIfNeeded()
        } catch (t: Throwable) {
            UserError.Log.e(TAG, "session refresh check failed: ${t.message}")
        }

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
            processor.updateEpoch(time.clockOffset, time.localTimeEpoch)
            // Residual pump-vs-phone drift: already folded into the timestamp offset, and
            // kept here so the UI can warn that the pump clock itself needs setting.
            IletPrefs.setClockDriftSeconds(processor.clockDriftSeconds)
            IletPrefs.setLastContact(System.currentTimeMillis())
            IletStatus.markContact()
            setState("Running")

            // Read the real-time characteristic directly. Some firmware does not
            // deliver the 0x1506 stream as BLE notifications, but a plain read of
            // A0090107 returns a current snapshot (the connection test relies on this).
            readRealtimeSafely(client)
            backfill(client)
            readRealtimeSafely(client)
        } finally {
            client.disconnect()
        }
    }

    private suspend fun readRealtimeSafely(client: IletClient) {
        try {
            // Accepts both 0x1506 and the 0x1515 superset (meal announcement + patch session).
            val snapshot = client.readRealtimeSnapshot()
            processor.processRealtimeSnapshot(snapshot)
            lastInteractionTime = System.currentTimeMillis()
            IletPrefs.setLastContact(System.currentTimeMillis())
            IletStatus.markContact()
        } catch (t: Throwable) {
            UserError.Log.e(TAG, "realtime read failed: ${t.message}")
        }
    }

    /**
     * Backfill history, newest-first so recent glucose and boluses appear immediately.
     *
     * Each type keeps three cursors: [high] = newest sequence seen, [low] = oldest
     * sequence of the contiguous processed range, and a history floor to stop at. New
     * records above [high] are walked ascending (the pump needs oldest->newest order
     * for algorithm rows); older history is walked descending from [low] so the most
     * recent backlog is imported first. Algorithm rows (unsupported on this firmware)
     * stay ascending.
     */
    private suspend fun backfill(client: IletClient) {
        val requested = IletPrefs.backfillFrom()
        IletStatus.updateRequest(requested)
        val deepFloor = IletPrefs.deepBackfillFloor()
        if (requested > 0) {
            // A user request reads forward from the chosen date, so that day fills first.
            UserError.Log.d(TAG, "acting on backfill-from-date request: ${JoH.dateTimeText(requested)}")
            setFloors(client, requested, newestFirst = false)
            IletPrefs.setForwardFillActive(true)
            IletPrefs.setForwardFillStalledCycles(0)
            IletPrefs.setBackfillFrom(0L)
            // Don't let the one-time deep fill rewind over the user's chosen range.
            if (deepFloor == 0L) {
                IletPrefs.setDeepBackfillFloor(System.currentTimeMillis() - FIRST_RUN_LOOKBACK_MS)
            }
        } else if (deepFloor == 0L) {
            val targetMs = System.currentTimeMillis() - FIRST_RUN_LOOKBACK_MS
            UserError.Log.d(TAG, "running one-time deep backfill to ${JoH.dateTimeText(targetMs)}")
            if (setFloors(client, targetMs, newestFirst = true)) IletPrefs.setDeepBackfillFloor(targetMs)
        }

        for (type in BACKFILL_TYPES) {
            backfillType(client, type)
        }

        notifyIfBackfillComplete()
    }

    /**
     * After a user "backfill from date" fill finally catches up to the newest sequence,
     * tell the user once. Uses the max each type reported this cycle vs its watermark.
     * A stalled fill (no progress for several cycles) gives up quietly so a stale
     * request cannot keep toasting or hold the flag forever.
     */
    private fun notifyIfBackfillComplete() {
        if (!IletPrefs.forwardFillActive()) {
            if (IletPrefs.forwardFillStalledCycles() != 0) IletPrefs.setForwardFillStalledCycles(0)
            return
        }
        val snapshot = IletStatus.latest() ?: return
        var allDone = true
        var sawAny = false
        for ((key, progress) in snapshot.types) {
            if (progress.max <= 0) continue
            val high = when (key) {
                "glucose" -> IletPrefs.watermarkGlucose()
                "insulin" -> IletPrefs.watermarkInsulin()
                else -> continue
            }
            sawAny = true
            if (high < progress.max) {
                allDone = false
                break
            }
        }
        if (!sawAny) {
            // No comparable type yet: count the cycle and give up if it stays that way.
            noteForwardFillStall()
            return
        }
        if (allDone) {
            IletPrefs.setForwardFillActive(false)
            IletPrefs.setForwardFillStalledCycles(0)
            JoH.static_toast_long(getString(com.eveningoutpost.dexdrip.R.string.ilet_backfill_complete))
            return
        }
        // Still catching up: reset on any real progress, otherwise count toward the give-up.
        if (snapshot.types.values.any { it.processed > 0 }) {
            IletPrefs.setForwardFillStalledCycles(0)
        } else {
            noteForwardFillStall()
        }
    }

    /** Increment the no-progress counter; clear the request once it is exhausted. */
    private fun noteForwardFillStall() {
        val cycles = IletPrefs.forwardFillStalledCycles() + 1
        if (cycles >= MAX_FORWARD_FILL_STALLED_CYCLES) {
            IletPrefs.setForwardFillActive(false)
            IletPrefs.setForwardFillStalledCycles(0)
            UserError.Log.e(TAG, "giving up on forward-fill after $cycles cycles with no progress")
        } else {
            IletPrefs.setForwardFillStalledCycles(cycles)
        }
    }

    /**
     * Point each type's history floor just before [targetMs] and reset its cursors so
     * the walk restarts from the newest record and descends to the floor. This makes a
     * manual or deep backfill import the most recent records first, not the oldest.
     */
    private suspend fun setFloors(client: IletClient, targetMs: Long, newestFirst: Boolean): Boolean {
        var any = false
        for (type in BACKFILL_TYPES) {
            try {
                val bounds = client.sequenceNumberHistory(type)
                if (!bounds.isOk) continue
                val seq = findSequenceAtOrAfter(client, type, targetMs, bounds)
                val floor = if (seq != null) seq - 1 else bounds.oldestSequenceNumber - 1
                setHistoryFloor(type, floor)
                if (newestFirst) {
                    // Deep fill: descend from the newest record down to the floor.
                    setWatermark(type, bounds.newestSequenceNumber)
                    setLowWatermark(type, bounds.newestSequenceNumber + 1)
                } else {
                    // User request: start at the floor and walk forward, so the chosen
                    // date's data is imported immediately.
                    setWatermark(type, floor)
                    setLowWatermark(type, floor)
                }
                any = true
            } catch (t: Throwable) {
                UserError.Log.e(TAG, "history floor search for $type failed: ${t.message}")
            }
        }
        return any
    }

    private fun getWatermark(type: IletHistoricalRecordType): Long = when (type) {
        IletHistoricalRecordType.GLUCOSE -> IletPrefs.watermarkGlucose()
        IletHistoricalRecordType.INSULIN -> IletPrefs.watermarkInsulin()
        IletHistoricalRecordType.ALGORITHM_DATA -> IletPrefs.watermarkAlgo()
        else -> IletPrefs.watermarkAlarm()
    }

    private fun setWatermark(type: IletHistoricalRecordType, value: Long) {
        when (type) {
            IletHistoricalRecordType.GLUCOSE -> IletPrefs.setWatermarkGlucose(value)
            IletHistoricalRecordType.INSULIN -> IletPrefs.setWatermarkInsulin(value)
            IletHistoricalRecordType.ALGORITHM_DATA -> IletPrefs.setWatermarkAlgo(value)
            else -> IletPrefs.setWatermarkAlarm(value)
        }
    }

    private fun typeKey(type: IletHistoricalRecordType): String = when (type) {
        IletHistoricalRecordType.GLUCOSE -> "glucose"
        IletHistoricalRecordType.INSULIN -> "insulin"
        IletHistoricalRecordType.ALGORITHM_DATA -> "algo"
        else -> "alarm"
    }

    private fun getLowWatermark(type: IletHistoricalRecordType): Long =
        IletPrefs.lowWatermark(typeKey(type))

    private fun setLowWatermark(type: IletHistoricalRecordType, value: Long) =
        IletPrefs.setLowWatermark(typeKey(type), value)

    private fun getHistoryFloor(type: IletHistoricalRecordType): Long =
        IletPrefs.historyFloor(typeKey(type))

    private fun setHistoryFloor(type: IletHistoricalRecordType, value: Long) =
        IletPrefs.setHistoryFloor(typeKey(type), value)

    /** Unix millis of a parsed record, or null for unknown shapes. */
    private fun recordMillis(record: Any?): Long? {
        val rtc = when (record) {
            is IletHistoricalGlucoseRecord -> record.currentTimestamp
            is IletHistoricalInsulinRecord -> record.recordTimestampRtc
            is IletAlgoStepRecord -> record.recordTimestampRtc
            is IletAlarmHistory -> record.recordTimestampRtc
            else -> return null
        }
        // Insulin-derived records use the offset without the drift fold (see IletDataProcessor).
        val offset = when (record) {
            is IletHistoricalGlucoseRecord -> processor.epochOffsetSeconds
            else -> processor.insulinOffsetSeconds
        }
        return IletMapping.insulinMillis(rtc, offset)
    }

    /** Binary search for the first sequence whose record time is >= [targetMs]. */
    private suspend fun findSequenceAtOrAfter(
        client: IletClient,
        type: IletHistoricalRecordType,
        targetMs: Long,
        knownBounds: IletSequenceNumberHistoryResponse? = null,
    ): Long? {
        val bounds = knownBounds ?: client.sequenceNumberHistory(type)
        if (!bounds.isOk) return null
        var lo = bounds.oldestSequenceNumber
        var hi = bounds.newestSequenceNumber
        if (hi <= 0 || hi < lo) return null
        var found: Long? = null
        var steps = 0
        while (lo <= hi && steps++ < 40) {
            val mid = (lo + hi) / 2
            val response = client.getHistoricalRecord(type, mid)
            val ms = if (response.isOk) recordMillis(response.record()) else null
            if (ms == null) {
                lo = mid + 1 // unreadable slot: bias forward
            } else if (ms >= targetMs) {
                found = mid
                hi = mid - 1
            } else {
                lo = mid + 1
            }
        }
        return found
    }

    private suspend fun backfillType(client: IletClient, type: IletHistoricalRecordType) {
        val expected = type == IletHistoricalRecordType.GLUCOSE || type == IletHistoricalRecordType.INSULIN
        val bounds = try {
            client.sequenceNumberHistory(type)
        } catch (t: Throwable) {
            // 0x2335 is the confirmed bounds primitive; 0x2381 is unsupported on
            // current firmware (no reply, link drop), so we never depend on it.
            if (expected) UserError.Log.e(TAG, "bounds for $type unavailable: ${t.message}")
            return
        }
        if (!bounds.isOk) {
            // Only glucose and insulin answer on the sampled firmware; algorithm/alarm
            // return status 3 (unsupported). Silently skip those, no log.
            if (expected) UserError.Log.e(TAG, "bounds for $type not ok: status=${bounds.status}")
            return
        }
        val min = bounds.oldestSequenceNumber
        val max = bounds.newestSequenceNumber
        if (max <= 0 || max < min) return

        val isOrdered = type == IletHistoricalRecordType.ALGORITHM_DATA
        if (isOrdered) processor.resetAlgoOrdering()

        processor.resetHistoryStats()
        val floor = getHistoryFloor(type)
        // A fresh type, or a backlog larger than one cycle with no explicit floor, is read
        // newest-first from the top; the low cursor then walks down through the whole gap.
        // This migrates installs left mid-range by the old oldest-first walk, without
        // disturbing a user's forward "backfill from date" (which sets a floor).
        val storedHigh = getWatermark(type)
        val bigGap = storedHigh < 0 || (floor < 0 && (max - storedHigh) > MAX_BACKFILL_PER_CYCLE)
        var high = if (bigGap) max else storedHigh.coerceIn(min - 1, max)
        var low = if (bigGap) max + 1 else getLowWatermark(type)
        if (low < 0 || low > high + 1) low = high + 1
        val effectiveFloor = if (floor >= 0) maxOf(min - 1, floor) else min - 1

        var budget = MAX_BACKFILL_PER_CYCLE
        var processed = 0

        // 1) Records above the high cursor, ascending. The pump returns one 4096-byte
        //    sector per 0x2333 request, so we loop several requests per cycle until the
        //    budget is spent (previously one sector per cycle, which was very slow).
        if (high < max && budget > 0) {
            val batched = type == IletHistoricalRecordType.GLUCOSE || type == IletHistoricalRecordType.INSULIN
            if (batched) {
                var iterations = 0
                while (high < max && budget > 0 && iterations++ < MAX_SECTOR_REQUESTS_PER_CYCLE) {
                    val from = maxOf(min, high + 1)
                    val to = minOf(max, from + budget - 1)
                    val read = readHistoryViaSectors(client, type, from, to) ?: break
                    val newHigh = maxOf(high, read.third ?: to)
                    if (read.first == 0 && newHigh <= high) break
                    processed += read.first
                    budget -= minOf(read.first, budget)
                    high = newHigh
                }
            } else {
                val to = minOf(max, high + budget)
                var seq = maxOf(min, high + 1)
                while (seq <= to && budget > 0) {
                    if (!processOne(client, type, seq)) break
                    seq++
                    budget--
                    processed++
                }
                high = maxOf(high, seq - 1)
            }
        }

        // 2) Older history below the low cursor. Glucose/insulin are read in batches
        //    (a sector holds many records); anything unparseable falls back.
        if (low > effectiveFloor + 1 && budget > 0) {
            val batched = type == IletHistoricalRecordType.GLUCOSE || type == IletHistoricalRecordType.INSULIN
            if (batched) {
                var iterations = 0
                while (low > effectiveFloor + 1 && budget > 0 && iterations++ < MAX_SECTOR_REQUESTS_PER_CYCLE) {
                    val upper = low - 1
                    val lower = maxOf(effectiveFloor + 1, upper - budget + 1)
                    val read = readHistoryViaSectors(client, type, lower, upper) ?: break
                    val newLow = minOf(lower, read.second ?: lower)
                    if (read.first == 0 && newLow >= low) break
                    processed += read.first
                    budget -= minOf(read.first, budget)
                    low = newLow
                }
            } else {
                var seq = low - 1
                var lowestDone: Long? = null
                while (seq > effectiveFloor && budget > 0) {
                    if (!processOne(client, type, seq)) break
                    lowestDone = seq
                    seq--
                    budget--
                    processed++
                }
                if (lowestDone != null) low = lowestDone
            }
        }

        setWatermark(type, high)
        setLowWatermark(type, low)
        IletStatus.updateType(typeKey(type), min, max, effectiveFloor, low, high, processed, processor.historyStats())
        // Only log when something actually happened, so an idle cycle stays quiet.
        if (processed > 0) {
            UserError.Log.d(
                TAG,
                "backfill $type: processed $processed, high $high, low $low, floor $effectiveFloor; " +
                    processor.historyStats()
            )
        }
    }

    /**
     * Read [lower]..[upper] as batched sectors. Returns (records processed, lowest
     * sequence seen) or null to fall back. A sector is coarser than the requested
     * range, so all returned records are processed (dedupe handles overlap).
     */
    private suspend fun readHistoryViaSectors(
        client: IletClient,
        type: IletHistoricalRecordType,
        lower: Long,
        upper: Long,
    ): Triple<Int, Long?, Long?>? {
        return try {
            val sectors = client.readBackfillSectors(type, lower, upper)
            if (sectors.isEmpty()) return null
            var count = 0
            var lowest: Long? = null
            var highest: Long? = null
            for (sector in sectors) {
                val records = IletSector.parseSector(sector).second
                for (record in records) {
                    dispatch(record.parsed())
                    count++
                    lowest = if (lowest == null) record.sequenceNumber else minOf(lowest, record.sequenceNumber)
                    highest = if (highest == null) record.sequenceNumber else maxOf(highest, record.sequenceNumber)
                }
            }
            Triple(count, lowest, highest)
        } catch (t: Throwable) {
            UserError.Log.e(TAG, "sector backfill $type $lower..$upper failed, falling back: ${t.message}")
            null
        }
    }

    /** Dispatch a parsed historical record to the processor. */
    private fun dispatch(record: Any?) {
        when (record) {
            is IletHistoricalGlucoseRecord -> processor.processHistoricalGlucose(record)
            is IletHistoricalInsulinRecord -> processor.processHistoricalInsulin(record)
            is IletAlgoStepRecord -> processor.processAlgoStep(record)
            is IletAlarmHistory -> processor.processAlarm(record)
        }
    }

    /** Fetch and dispatch one record; false on a transport failure so the walk can stop. */
    private suspend fun processOne(client: IletClient, type: IletHistoricalRecordType, seq: Long): Boolean {
        return try {
            val response = client.getHistoricalRecord(type, seq)
            if (response.isOk) dispatch(response.record())
            true
        } catch (e: IletProtocolError) {
            // A corrupt/unknown record must not stall the walk forever.
            UserError.Log.wtf(TAG, "backfill $type seq $seq unparseable: ${e.message}")
            true
        } catch (t: Throwable) {
            UserError.Log.e(TAG, "backfill $type seq $seq failed: ${t.message}")
            false
        }
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

        // Healthy-cycle interval: the pump link drops after ~2.5 s idle, so a 60 s cadence
        // keeps glucose visible without re-handshaking every 15 s.
        private const val IDLE_INTERVAL_MS = 60_000L
        // Records attempted per type per cycle. With the batched sector path this is
        // cheap, so a multi-day "from date to now" fill completes in a few cycles.
        private const val MAX_BACKFILL_PER_CYCLE = 1500

        // The pump answers one sector per 0x2333 request, so loop several per cycle.
        private const val MAX_SECTOR_REQUESTS_PER_CYCLE = 25

        // Give up on a forward-fill after this many cycles with no progress, so a stale
        // request cannot hold the flag (and keep toasting) forever.
        private const val MAX_FORWARD_FILL_STALLED_CYCLES = 3
        private val BACKFILL_TYPES = listOf(
            IletHistoricalRecordType.GLUCOSE,
            IletHistoricalRecordType.INSULIN,
            IletHistoricalRecordType.ALGORITHM_DATA,
            IletHistoricalRecordType.ALARM,
        )

        /** First-run look-back window (time-based; record density varies by type). */
        private const val FIRST_RUN_LOOKBACK_MS = 24 * 60 * 60 * 1000L

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

        /** Rows for the System Status screen. */
        @JvmStatic
        fun megaStatus(): List<com.eveningoutpost.dexdrip.utilitymodels.StatusItem> {
            val rows = ArrayList<com.eveningoutpost.dexdrip.utilitymodels.StatusItem>()
            val snapshot = IletStatus.latest()
            if (snapshot == null) {
                rows.add(com.eveningoutpost.dexdrip.utilitymodels.StatusItem(
                    "Status", "no data yet - enable iLet to connect", com.eveningoutpost.dexdrip.utilitymodels.StatusItem.Highlight.NOTICE))
                return rows
            }
            val stateHighlight = when {
                snapshot.state.startsWith("Error") -> com.eveningoutpost.dexdrip.utilitymodels.StatusItem.Highlight.CRITICAL
                snapshot.state == "Running" -> com.eveningoutpost.dexdrip.utilitymodels.StatusItem.Highlight.GOOD
                else -> com.eveningoutpost.dexdrip.utilitymodels.StatusItem.Highlight.NOTICE
            }
            rows.add(com.eveningoutpost.dexdrip.utilitymodels.StatusItem("Connection", snapshot.state, stateHighlight))
            val contact = IletPrefs.lastContact()
            rows.add(com.eveningoutpost.dexdrip.utilitymodels.StatusItem(
                "Last contact",
                if (contact > 0) "${com.eveningoutpost.dexdrip.models.JoH.dateTimeText(contact)} (${com.eveningoutpost.dexdrip.models.JoH.msSince(contact) / 1000}s ago)" else "never",
                if (contact > 0 && com.eveningoutpost.dexdrip.models.JoH.msSince(contact) < 15 * 60 * 1000L)
                    com.eveningoutpost.dexdrip.utilitymodels.StatusItem.Highlight.GOOD
                else com.eveningoutpost.dexdrip.utilitymodels.StatusItem.Highlight.NOTICE))
            // Latest pump status (moved here from the settings screen).
            rows.add(com.eveningoutpost.dexdrip.utilitymodels.StatusItem(
                "Battery", com.eveningoutpost.dexdrip.utilitymodels.PumpStatus.getBatteryString().trim()))
            rows.add(com.eveningoutpost.dexdrip.utilitymodels.StatusItem(
                "IOB", com.eveningoutpost.dexdrip.utilitymodels.PumpStatus.getBolusIoBString().trim()))
            rows.add(com.eveningoutpost.dexdrip.utilitymodels.StatusItem(
                "Reservoir", com.eveningoutpost.dexdrip.utilitymodels.PumpStatus.getReservoirString().trim()))
            val pumpState = IletState.latest()
            if (pumpState?.insulinPaused == true) {
                val since = IletPrefs.insulinPausedSinceMs()
                rows.add(com.eveningoutpost.dexdrip.utilitymodels.StatusItem(
                    "Insulin",
                    "SUSPENDED" + if (since > 0)
                        " for " + com.eveningoutpost.dexdrip.models.JoH.niceTimeScalar(System.currentTimeMillis() - since)
                    else "",
                    com.eveningoutpost.dexdrip.utilitymodels.StatusItem.Highlight.NOTICE))
            }
            val drift = IletPrefs.clockDriftSeconds()
            val driftOff = kotlin.math.abs(drift) >= IletPrefs.CLOCK_DRIFT_WARN_SECONDS
            rows.add(com.eveningoutpost.dexdrip.utilitymodels.StatusItem(
                "Clock drift",
                (if (drift >= 0) "+" else "") + drift + "s" + if (driftOff) " (pump time is off)" else "",
                if (driftOff) com.eveningoutpost.dexdrip.utilitymodels.StatusItem.Highlight.NOTICE
                else com.eveningoutpost.dexdrip.utilitymodels.StatusItem.Highlight.GOOD))
            val patchStart = IletPrefs.patchSessionStartMs()
            if (patchStart > 0) {
                val patchEnd = IletPrefs.patchSessionEndMs()
                val expired = patchEnd > 0 && patchEnd < System.currentTimeMillis()
                rows.add(com.eveningoutpost.dexdrip.utilitymodels.StatusItem(
                    "Infusion set",
                    com.eveningoutpost.dexdrip.models.JoH.niceTimeScalar(System.currentTimeMillis() - patchStart) +
                        " old" + if (expired) " (expired)" else "",
                    if (expired) com.eveningoutpost.dexdrip.utilitymodels.StatusItem.Highlight.NOTICE
                    else com.eveningoutpost.dexdrip.utilitymodels.StatusItem.Highlight.GOOD))
            }
            rows.add(com.eveningoutpost.dexdrip.utilitymodels.StatusItem(
                "Backfill request",
                if (snapshot.requestMs > 0) com.eveningoutpost.dexdrip.models.JoH.dateTimeText(snapshot.requestMs) else "none",
                if (snapshot.requestMs > 0) com.eveningoutpost.dexdrip.utilitymodels.StatusItem.Highlight.NOTICE
                else com.eveningoutpost.dexdrip.utilitymodels.StatusItem.Highlight.NORMAL))
            val serial = IletPrefs.deviceSerial()
            val model = IletPrefs.deviceModel()
            if (serial.isNotEmpty() || model.isNotEmpty()) {
                rows.add(com.eveningoutpost.dexdrip.utilitymodels.StatusItem("Device", "$model $serial".trim()))
            }
            for ((key, p) in snapshot.types) {
                val done = p.low <= p.floor + 1
                val range = if (p.max > 0) "${p.min}..${p.max}" else "?"
                val progress = if (p.floor >= 0) "floor ${p.floor} low ${p.low} high ${p.high}" else "high ${p.high}"
                rows.add(com.eveningoutpost.dexdrip.utilitymodels.StatusItem(
                    "$key backfill",
                    if (done) "complete ($range)" else "$progress ($range)",
                    if (done) com.eveningoutpost.dexdrip.utilitymodels.StatusItem.Highlight.GOOD
                    else com.eveningoutpost.dexdrip.utilitymodels.StatusItem.Highlight.NOTICE))
                rows.add(com.eveningoutpost.dexdrip.utilitymodels.StatusItem("$key last pass", "${p.processed} records ${p.stats}"))
            }
            return rows
        }

        /**
         * Cut the inter-cycle delay short so a pending request (e.g. backfill from
         * date) is acted on immediately, and make sure the service is running.
         */
        @JvmStatic
        fun requestImmediateCycle(context: Context) {
            instance?.wakeups?.trySend(Unit)
            ILetEntry.startIfEnabled(context)
        }
    }
}
