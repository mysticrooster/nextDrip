package com.eveningoutpost.dexdrip.cgm.ilet

import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletRealTimeControlPumpData
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletRealTimeData
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore
import org.json.JSONObject

/**
 * Latest secondary pump flags from the real-time stream (suspend, charging,
 * cartridge-empty, and — from the 0x1515 superset — meal
 * announcement and pump-patch/infusion-set session window). Persisted as one
 * JSON blob so the Nightscout uploader and status UI can read it without
 * touching the service.
 */
object IletState {

    private const val KEY = "ilet-latest-state"

    data class Snapshot(
        val timestamp: Long,
        val insulinPaused: Boolean,
        val charging: Boolean,
        val cartridgeEmpty: Boolean,
        val bgSuspendTime: Long,
        val bgExpirationTime: Long,
        val mealAnnouncementState: Int = -1,
        val silentMode: Int = -1,
    )

    @JvmStatic
    fun update(data: IletRealTimeData, now: Long = System.currentTimeMillis()) {
        write(
            now, data.isInsulinPaused, data.isCharging, data.isCartridgeEmpty,
            data.bgSuspendTime, data.bgExpirationTime
        )
    }

    @JvmStatic
    fun updateControl(data: IletRealTimeControlPumpData, now: Long = System.currentTimeMillis()) {
        write(
            now, data.isInsulinPaused, data.isCharging, data.isCartridgeEmpty,
            data.bgSuspendTime, data.bgExpirationTime,
            data.mealAnnouncementState, data.silentMode
        )
    }

    private fun write(
        now: Long,
        paused: Boolean,
        charging: Boolean,
        cartridgeEmpty: Boolean,
        bgSuspendTime: Long,
        bgExpirationTime: Long,
        mealAnnouncementState: Int = -1,
        silentMode: Int = -1,
    ) {
        val json = JSONObject()
            .put("ts", now)
            .put("paused", paused)
            .put("charging", charging)
            .put("cartridgeEmpty", cartridgeEmpty)
            .put("bgSuspendTime", bgSuspendTime)
            .put("bgExpirationTime", bgExpirationTime)
            .put("mealAnnouncementState", mealAnnouncementState)
            .put("silentMode", silentMode)
        PersistentStore.setString(KEY, json.toString())
    }

    @JvmStatic
    fun latest(): Snapshot? {
        val raw = PersistentStore.getString(KEY)
        if (raw.isNullOrEmpty()) return null
        return try {
            val j = JSONObject(raw)
            Snapshot(
                j.getLong("ts"), j.getBoolean("paused"), j.getBoolean("charging"),
                j.getBoolean("cartridgeEmpty"),
                j.getLong("bgSuspendTime"), j.getLong("bgExpirationTime"),
                j.optInt("mealAnnouncementState", -1),
                j.optInt("silentMode", -1),
            )
        } catch (e: Exception) {
            null
        }
    }
}
