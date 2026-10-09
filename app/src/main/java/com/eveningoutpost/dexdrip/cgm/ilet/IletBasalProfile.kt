package com.eveningoutpost.dexdrip.cgm.ilet

import android.content.Context
import com.eveningoutpost.dexdrip.models.UserError
import com.eveningoutpost.dexdrip.nocturne.NocturneUploader

/**
 * Publishes the iLet's 0 U/hr basal profile to the destinations that model basal
 * profiles. The iLet delivers insulin automatically and has no scheduled basal,
 * so without this the services show the profile as unknown.
 *
 * - Nocturne: a [com.eveningoutpost.dexdrip.nocturne.NocturneUploader] basal schedule (here).
 * - Nightscout: posted from the device-status path on the next upload cycle.
 * - Tidepool: a scheduled 0 U/hr basal is added to each upload chunk.
 */
object IletBasalProfile {

    private const val TAG = "iLet"

    @JvmStatic
    fun publishIfEnabled(context: Context) {
        if (!IletPrefs.isEnabled() || !IletPrefs.publishZeroBasal()) return
        try {
            if (NocturneUploader(context).publishIletZeroBasalSchedule()) {
                UserError.Log.d(TAG, "Published iLet 0 U/hr basal schedule to Nocturne")
            }
        } catch (t: Throwable) {
            UserError.Log.e(TAG, "iLet 0 U/hr basal schedule upload failed: ${t.message}")
        }
    }
}
