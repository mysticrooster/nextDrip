package com.eveningoutpost.dexdrip.cgm.ilet

import android.content.Context
import android.content.Intent
import com.eveningoutpost.dexdrip.models.JoH
import com.eveningoutpost.dexdrip.models.UserError

/**
 * Lifecycle entry point for the iLet service, independent of the CGM collector
 * machinery so pump-only mode can run when another CGM is selected.
 */
object ILetEntry {

    private const val TAG = "iLet"

    @JvmStatic
    fun startIfEnabled(context: Context) {
        if (IletPrefs.isEnabled()) {
            UserError.Log.d(TAG, "starting iLet service (pump data source)")
            JoH.startService(ILetService::class.java)
        }
    }
    @JvmStatic
    fun immortality() {
        if (IletPrefs.isEnabled()) {
            JoH.startService(ILetService::class.java)
        }
    }

    @JvmStatic
    fun stop(context: Context) {
        context.stopService(Intent(context, ILetService::class.java))
        ILetService.lastState = "Stopped"
    }
}
