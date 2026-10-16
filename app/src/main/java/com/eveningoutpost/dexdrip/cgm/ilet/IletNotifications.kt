package com.eveningoutpost.dexdrip.cgm.ilet

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.eveningoutpost.dexdrip.Home
import com.eveningoutpost.dexdrip.R
import com.eveningoutpost.dexdrip.models.JoH
import com.eveningoutpost.dexdrip.utilitymodels.NotificationChannels
import com.eveningoutpost.dexdrip.utilitymodels.Unitized
import java.util.Locale

/**
 * iLet pump alert notifications (low cartridge, predicted low). Raised through
 * [JoH.showNotification] with sound and vibration so they behave like xDrip's
 * update-available and sensor-expiry alerts. The caller owns the once-per-episode latch.
 */
object IletNotifications {

    private const val LOW_CARTRIDGE_ID = 0x1E7A10
    private const val PREDICTED_LOW_ID = 0x1E7A11

    fun lowCartridge(context: Context, units: Double) {
        notify(
            context,
            LOW_CARTRIDGE_ID,
            context.getString(R.string.ilet_notify_low_cartridge_title),
            context.getString(R.string.ilet_notify_low_cartridge_body, String.format(Locale.US, "%.1f", units)),
        )
    }

    fun clearLowCartridge() = JoH.cancelNotification(LOW_CARTRIDGE_ID)

    fun predictedLow(context: Context, predictedMgdl: Int) {
        notify(
            context,
            PREDICTED_LOW_ID,
            context.getString(R.string.ilet_notify_predicted_low_title),
            context.getString(
                R.string.ilet_notify_predicted_low_body,
                Unitized.unitized_string_with_units_static(predictedMgdl.toDouble()),
            ),
        )
    }

    fun clearPredictedLow() = JoH.cancelNotification(PREDICTED_LOW_ID)

    private fun notify(context: Context, id: Int, title: String, body: String) {
        val pending = PendingIntent.getActivity(
            context,
            id,
            Intent(context, Home::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        JoH.showNotification(
            title,
            body,
            pending,
            id,
            NotificationChannels.GENERAL_CHANNEL,
            true, // sound
            true, // vibrate
            null,
            null,
            null,
            false,
        )
    }
}
