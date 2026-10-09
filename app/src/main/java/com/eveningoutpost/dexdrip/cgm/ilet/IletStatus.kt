package com.eveningoutpost.dexdrip.cgm.ilet

import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore
import org.json.JSONObject

/**
 * Small persisted snapshot of the iLet service's connection and backfill progress,
 * surfaced on the System Status screen. Kept separate from [IletState] so the
 * status UI does not need to talk to the service.
 */
object IletStatus {

    private const val KEY = "ilet-backfill-status"

    data class TypeProgress(
        val min: Long,
        val max: Long,
        val floor: Long,
        val low: Long,
        val high: Long,
        val processed: Int,
        val stats: String,
        val updatedAt: Long,
    )

    data class Snapshot(
        val state: String,
        val stateAt: Long,
        val contactAt: Long,
        val requestMs: Long,
        val types: Map<String, TypeProgress>,
    )

    @JvmStatic
    fun updateState(state: String, now: Long = System.currentTimeMillis()) {
        val json = readJson()
        json.put("state", state)
        json.put("stateAt", now)
        write(json)
    }

    @JvmStatic
    fun markContact(now: Long = System.currentTimeMillis()) {
        val json = readJson()
        json.put("contactAt", now)
        write(json)
    }

    @JvmStatic
    fun updateRequest(requestMs: Long) {
        val json = readJson()
        json.put("requestMs", requestMs)
        write(json)
    }

    @JvmStatic
    fun updateType(
        key: String,
        min: Long,
        max: Long,
        floor: Long,
        low: Long,
        high: Long,
        processed: Int,
        stats: String = "",
        now: Long = System.currentTimeMillis(),
    ) {
        val json = readJson()
        val types = json.optJSONObject("types") ?: JSONObject()
        types.put(
            key,
            JSONObject()
                .put("min", min)
                .put("max", max)
                .put("floor", floor)
                .put("low", low)
                .put("high", high)
                .put("processed", processed)
                .put("stats", stats)
                .put("at", now),
        )
        json.put("types", types)
        write(json)
    }

    @JvmStatic
    fun latest(): Snapshot? {
        val raw = PersistentStore.getString(KEY) ?: return null
        return try {
            val j = JSONObject(raw)
            val typesJson = j.optJSONObject("types")
            val types = LinkedHashMap<String, TypeProgress>()
            if (typesJson != null) {
                for (key in typesJson.keys()) {
                    val t = typesJson.getJSONObject(key)
                    types[key] = TypeProgress(
                        t.optLong("min"), t.optLong("max"), t.optLong("floor"),
                        t.optLong("low"), t.optLong("high"), t.optInt("processed"),
                        t.optString("stats", ""), t.optLong("at"),
                    )
                }
            }
            Snapshot(
                j.optString("state", "unknown"),
                j.optLong("stateAt"),
                j.optLong("contactAt"),
                j.optLong("requestMs"),
                types,
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun readJson(): JSONObject {
        val raw = PersistentStore.getString(KEY)
        return try {
            if (raw.isNullOrEmpty()) JSONObject() else JSONObject(raw)
        } catch (e: Exception) {
            JSONObject()
        }
    }

    private fun write(json: JSONObject) {
        PersistentStore.setString(KEY, json.toString())
    }
}
