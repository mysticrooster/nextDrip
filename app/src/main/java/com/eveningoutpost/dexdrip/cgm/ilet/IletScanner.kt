package com.eveningoutpost.dexdrip.cgm.ilet

import android.os.ParcelUuid
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletConstants
import com.eveningoutpost.dexdrip.models.UserError
import com.eveningoutpost.dexdrip.utilitymodels.RxBleProvider
import com.polidea.rxandroidble2.scan.ScanFilter
import com.polidea.rxandroidble2.scan.ScanSettings
import java.util.concurrent.TimeUnit

/**
 * Finds an iLet advertising the pump service UUID. Used only when no address has
 * been remembered; the first matching device wins.
 */
object IletScanner {

    private const val TAG = "iLet"

    fun scanForAddress(timeoutSeconds: Long = 15): String? {
        return try {
            val client = RxBleProvider.getSingleton()
            val filter = ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(IletConstants.SERVICE_UUID))
                .build()
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()
            client.scanBleDevices(settings, filter)
                .timeout(timeoutSeconds, TimeUnit.SECONDS)
                .blockingFirst()
                .bleDevice
                .macAddress
        } catch (t: Throwable) {
            UserError.Log.e(TAG, "iLet scan failed: ${t.message}")
            null
        }
    }
}
