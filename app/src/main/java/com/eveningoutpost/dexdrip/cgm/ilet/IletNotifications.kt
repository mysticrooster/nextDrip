package com.eveningoutpost.dexdrip.cgm.ilet

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.eveningoutpost.dexdrip.Home
import com.eveningoutpost.dexdrip.R
import com.eveningoutpost.dexdrip.utilitymodels.NotificationChannels
import com.eveningoutpost.dexdrip.utilitymodels.Unitized
import java.util.Locale

/**
 * iLet pump alert notifications (low cartridge, predicted low). Gated by the iLet
 * "Notifications" settings; the caller owns the once-per-episode latch.
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

    fun clearLowCartridge(context: Context) = cancel(context, LOW_CARTRIDGE_ID)

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

    fun clearPredictedLow(context: Context) = cancel(context, PREDICTED_LOW_ID)

    private fun notify(context: Context, id: Int, title: String, body: String) {
        val pending = PendingIntent.getActivity(
            context,
            id,
            Intent(context, Home::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(context, NotificationChannels.GENERAL_CHANNEL)
            .setSmallIcon(R.drawable.ic_action_communication_invert_colors_on)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pending)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(id, builder.build())
    }

    private fun cancel(context: Context, id: Int) {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(id)
    }
}
