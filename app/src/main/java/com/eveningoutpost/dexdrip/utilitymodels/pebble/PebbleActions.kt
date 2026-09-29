package com.eveningoutpost.dexdrip.utilitymodels.pebble

import android.content.Context
import android.content.Intent
import com.eveningoutpost.dexdrip.utilitymodels.pebble.watchface.InstallPebbleClassicTrendWatchface
import com.eveningoutpost.dexdrip.utilitymodels.pebble.watchface.InstallPebbleTrendClayWatchFace
import com.eveningoutpost.dexdrip.utilitymodels.pebble.watchface.InstallPebbleTrendWatchFace
import com.eveningoutpost.dexdrip.utilitymodels.pebble.watchface.InstallPebbleWatchFace

/**
 * Single implementation of the Pebble service/watchface actions, shared by the legacy
 * `AllPrefsFragment` (`enablePebble` / `installPebbleWatchface` / `restartPebble`) and the Compose
 * settings screen so the stateful start/stop logic and the watchface table cannot drift.
 */
object PebbleActions {

    @Volatile
    private var pebbleType = 1

    fun restartPebble(context: Context) {
        context.startService(Intent(context, PebbleWatchSync::class.java))
    }

    fun enablePebble(context: Context, newValueInt: Int, enabled: Boolean) {
        if (pebbleType == 1) {
            if (enabled && newValueInt != 1) {
                context.stopService(Intent(context, PebbleWatchSync::class.java))
                context.startService(Intent(context, PebbleWatchSync::class.java))
            }
        } else {
            if (!enabled || newValueInt == 1) {
                context.stopService(Intent(context, PebbleWatchSync::class.java))
            }
        }
        pebbleType = if (enabled) newValueInt else 1
        PebbleWatchSync.setPebbleType(pebbleType)
    }

    /** Confirm message for a watchface install, or null for type 1 (none). */
    fun installMessage(type: Int): String? = when (type) {
        2 -> "Install Standard Pebble Watchface?"
        3 -> "Install Pebble Trend Watchface?"
        4 -> "Install Pebble Classic Trend Watchface?"
        5 -> "Install Pebble Clay Trend Watchface?"
        else -> null
    }

    /** Installer activity for a watchface type, or null for type 1 (none). */
    fun installActivity(type: Int): Class<*>? = when (type) {
        2 -> InstallPebbleWatchFace::class.java
        3 -> InstallPebbleTrendWatchFace::class.java
        4 -> InstallPebbleClassicTrendWatchface::class.java
        5 -> InstallPebbleTrendClayWatchFace::class.java
        else -> null
    }
}
