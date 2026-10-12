package com.eveningoutpost.dexdrip.cgm.ilet

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.eveningoutpost.dexdrip.BaseAppCompatActivity
import com.eveningoutpost.dexdrip.R
import com.eveningoutpost.dexdrip.cgm.ilet.ble.IletTransport
import com.eveningoutpost.dexdrip.cgm.ilet.cloud.IletCloudSigner
import com.eveningoutpost.dexdrip.cgm.ilet.cloud.IletCredentialStore
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletClient
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletRealtimeSnapshot
import com.eveningoutpost.dexdrip.models.JoH
import com.eveningoutpost.dexdrip.utilitymodels.PumpStatus
import com.eveningoutpost.dexdrip.models.UserError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * One-shot connection test: connects to the pump, reads device info, time and a
 * single real-time packet, and shows the result. Read-only; never sends a dosing
 * or configuration command.
 */
class ILetStatusActivity : BaseAppCompatActivity() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var output: TextView
    private lateinit var refresh: Button
    private var job: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.ilet_status_activity_title)

        val pad = (16 * resources.displayMetrics.density).toInt()
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }
        output = TextView(this).apply { setText(R.string.ilet_status_connecting) }
        refresh = Button(this).apply {
            setText(R.string.ilet_status_refresh)
            setOnClickListener { run() }
        }
        column.addView(refresh)
        column.addView(output)
        setContentView(ScrollView(this).apply { addView(column) })
        run()
    }

    private fun run() {
        if (job?.isActive == true) return
        refresh.isEnabled = false
        output.setText(R.string.ilet_status_connecting)
        job = scope.launch {
            // Do not open a second BLE connection while the background service is
            // connected: the pump allows one link, so the two would collide. Instead
            // ask the service to refresh and read what it reports.
            val result = try {
                if (ILetService.isRunning()) testViaService() else connectAndRead()
            } catch (t: Throwable) {
                UserError.Log.e(TAG, "iLet status test failed: ${t.message}")
                getString(R.string.ilet_status_failed, t.message ?: t.javaClass.simpleName)
            }
            withContext(Dispatchers.Main) {
                output.text = result
                refresh.isEnabled = true
            }
        }
    }

    /** Trigger a service cycle and report the fresh contact/status it produces. */
    private suspend fun testViaService(): String {
        UserError.Log.d(TAG, "deferring connection test to the running iLet service")
        val requestedAt = System.currentTimeMillis()
        ILetService.requestImmediateCycle(this)
        val deadline = requestedAt + SERVICE_WAIT_MS
        while (System.currentTimeMillis() < deadline) {
            if (IletPrefs.lastContact() >= requestedAt) break
            delay(500)
        }
        val last = IletPrefs.lastContact()
        val state = IletState.latest()
        val sb = StringBuilder()
        if (last >= requestedAt) {
            sb.append(getString(R.string.ilet_status_ok)).append('\n')
        } else {
            sb.append(getString(R.string.ilet_status_service_no_update,
                    JoH.dateTimeText(last))).append('\n')
        }
        appendIdentity(sb)
        if (state != null) {
            sb.append("IOB: ").append(PumpStatus.getBolusIoBString().trim()).append('\n')
            sb.append("Reservoir: ").append(PumpStatus.getReservoirString().trim()).append('\n')
            sb.append("Battery: ").append(PumpStatus.getBatteryString().trim()).append('\n')
            sb.append("Suspended: ").append(state.insulinPaused).append('\n')
            sb.append("Cartridge empty: ").append(state.cartridgeEmpty).append('\n')
            sb.append("Updated: ").append(JoH.dateTimeText(state.timestamp))
        } else {
            sb.append("No real-time snapshot yet (last contact ${JoH.dateTimeText(last)})")
        }
        return sb.toString()
    }

    private fun appendIdentity(sb: StringBuilder) {
        sb.append("Serial: ").append(IletPrefs.deviceSerial().ifEmpty { "?" }).append('\n')
        sb.append("Model: ").append(IletPrefs.deviceModel().ifEmpty { "?" }).append("\n\n")
    }

    private suspend fun connectAndRead(): String {
        val address = IletPrefs.lastAddress().ifEmpty { IletScanner.scanForAddress() ?: "" }
        if (address.isEmpty()) {
            throw IllegalStateException("no iLet Bluetooth address configured or found")
        }
        val store = IletCredentialStore.getInstance(this)
        val transport = IletTransport(address)
        val client = IletClient(transport, store, IletCloudSigner(store))
        // allowReenrol=false: a test must not mutate the pump credential.
        client.connect(allowReenrol = false)
        try {
            val info = client.getDeviceInfo()
            val time = client.getTime()
            IletPrefs.setDeviceSerial(info.deviceSerial)
            IletPrefs.setDeviceModel(info.modelId)

            val sb = StringBuilder()
            sb.append(getString(R.string.ilet_status_ok)).append('\n')
            sb.append("Serial: ").append(info.deviceSerial).append('\n')
            sb.append("Model: ").append(info.modelId).append('\n')
            sb.append("Firmware: host ").append(info.hostVersion)
                .append(" · motor ").append(info.motorVersion)
                .append(" · ble ").append(info.bleVersion).append('\n')
            sb.append("Algo: ").append(info.algoVersion).append('\n')
            sb.append("Clock offset: ").append(time.clockOffset).append("s\n\n")

            sb.append(getString(R.string.ilet_status_waiting)).append('\n')
            withContext(Dispatchers.Main) { output.text = sb.toString() }
            val snapshot = withTimeout(REALTIME_TIMEOUT_MS) { client.readRealtimeSnapshot() }
            // Publish what we read so the settings status and cloud paths see it too.
            val processor = IletDataProcessor()
            processor.updateEpoch(time.clockOffset, time.localTimeEpoch)
            processor.processRealtimeSnapshot(snapshot)
            IletPrefs.setClockDriftSeconds(processor.clockDriftSeconds)
            IletPrefs.setLastContact(System.currentTimeMillis())

            when (snapshot) {
                is IletRealtimeSnapshot.Standard -> {
                    val data = snapshot.data
                    appendRealtime(sb, data, processor)
                }
                is IletRealtimeSnapshot.Control -> {
                    val data = snapshot.data
                    sb.append("Packet: 0x1515 (control)\n")
                    appendRealtime(sb, data, processor)
                    sb.append("Meal announcement state: ").append(data.mealAnnouncementState).append('\n')
                    sb.append("Patch session end: ")
                        .append(pumpTime(data.pumpPatchInfoSessionEndTime, processor)).append('\n')
                    sb.append("Silent mode: ").append(data.silentMode).append('\n')
                }
            }

            return sb.toString()
        } finally {
            try {
                client.disconnect()
            } catch (t: Throwable) {
                UserError.Log.d(TAG, "disconnect failed: ${t.message}")
            }
        }
    }

    private fun appendRealtime(
        sb: StringBuilder,
        data: com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletRealTimeData,
        processor: IletDataProcessor,
    ) {
        sb.append("Glucose: ").append(data.glucose.readingMgDl).append(" mg/dL")
            .append(" (pred ").append(data.glucose.predictedReading).append(")\n")
        sb.append("IOB: ").append(data.iobUnits).append(" U\n")
        sb.append("Reservoir: ").append(data.cartridgeRemainingUnits).append(" U\n")
        sb.append("Battery: ").append(data.batteryPct).append('%')
            .append(if (data.isCharging) " (charging)" else "").append('\n')
        sb.append("Suspended: ").append(data.isInsulinPaused).append('\n')
        sb.append("Cartridge empty: ").append(data.isCartridgeEmpty).append('\n')
        sb.append("Pump time: ").append(pumpTime(data.rtcTimestamp, processor)).append('\n')
    }

    private fun appendRealtime(
        sb: StringBuilder,
        data: com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletRealTimeControlPumpData,
        processor: IletDataProcessor,
    ) {
        sb.append("Glucose: ").append(data.glucose.readingMgDl).append(" mg/dL")
            .append(" (pred ").append(data.glucose.predictedReading).append(")\n")
        sb.append("IOB: ").append(data.iobUnits).append(" U\n")
        sb.append("Reservoir: ").append(data.cartridgeRemainingUnits).append(" U\n")
        sb.append("Battery: ").append(data.batteryPct).append('%')
            .append(if (data.isCharging) " (charging)" else "").append('\n')
        sb.append("Suspended: ").append(data.isInsulinPaused).append('\n')
        sb.append("Cartridge empty: ").append(data.isCartridgeEmpty).append('\n')
        sb.append("Pump time: ").append(pumpTime(data.rtcTimestamp, processor)).append('\n')
    }

    private fun pumpTime(epochSeconds: Long, processor: IletDataProcessor): String =
        if (epochSeconds <= 0) "-" else JoH.dateTimeText((epochSeconds + processor.epochOffsetSeconds) * 1000L)

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "iLet"
        private const val REALTIME_TIMEOUT_MS = 15_000L
        private const val SERVICE_WAIT_MS = 30_000L
    }
}
