package com.eveningoutpost.dexdrip.cgm.ilet

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import com.eveningoutpost.dexdrip.models.JoH
import com.eveningoutpost.dexdrip.models.UserError

/**
 * Bonds the iLet. Scans for the pump (service UUID, then a name containing "iLet"),
 * remembers its address and asks the OS to bond it, which shows the system pairing
 * dialog. Read-only with respect to the pump: bonding is an Android-level operation.
 */
object IletPairing {

    private const val TAG = "iLet"

    /** Blocking; call off the main thread. Returns a user-facing status message. */
    @JvmStatic
    fun pairPump(context: Context): String {
        return try {
            val address = IletPrefs.lastAddress().ifEmpty { IletScanner.findPumpMac() ?: "" }
            if (address.isEmpty()) {
                return "No iLet found nearby. Put the pump in pairing mode and try again, or pair it in Android Bluetooth settings."
            }
            IletPrefs.setLastAddress(address)
            val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
                ?: return "Bluetooth is unavailable on this device."
            if (!adapter.isEnabled) return "Turn Bluetooth on first."
            val device = adapter.getRemoteDevice(address)
            when (device.bondState) {
                BluetoothDevice.BOND_BONDED -> "iLet is already paired."
                BluetoothDevice.BOND_BONDING -> "Pairing already in progress; check the Android pairing prompt."
                else -> {
                    val started = device.createBond()
                    if (started) "Confirm the Android pairing request for the iLet."
                    else "Could not start pairing. Pair the iLet in Android Bluetooth settings."
                }
            }
        } catch (t: Throwable) {
            UserError.Log.e(TAG, "pairPump failed: ${t.message}")
            "Pairing failed: ${t.message}"
        }
    }

    @JvmStatic
    fun pairPumpWithToast(context: Context) {
        JoH.static_toast_long(pairPump(context))
    }

    /** Blocking; call off the main thread. Removes the Bluetooth bond. */
    @JvmStatic
    fun unpairPump(context: Context): String {
        val address = IletPrefs.lastAddress()
        if (address.isEmpty()) return "No iLet address is stored."
        return try {
            val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
                ?: return "Bluetooth is unavailable on this device."
            val device = adapter.getRemoteDevice(address)
            if (device.bondState != BluetoothDevice.BOND_BONDED) {
                IletPrefs.setLastAddress("")
                return "The iLet is not currently paired."
            }
            val removed = device.javaClass.getMethod("removeBond").invoke(device) as? Boolean ?: false
            IletPrefs.setLastAddress("")
            if (removed) "iLet unpaired." else "Unpair request sent; check Android Bluetooth settings."
        } catch (t: Throwable) {
            UserError.Log.e(TAG, "unpairPump failed: ${t.message}")
            "Unpair failed: ${t.message}"
        }
    }
}
