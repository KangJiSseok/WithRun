package com.mobile

import org.json.JSONObject

object WatchSyncPaths {
    const val livePath = "/health-data/live"
    const val snapshotPath = "/health-data/snapshot"
    const val catchupPath = "/health-data/catchup"
    const val catchupRequestPath = "/health-data/request-catchup"
    const val snapshotPayloadKey = "payload"
}

data class WatchSyncPayload(
    val kind: String,
    val sessionId: String,
    val seq: Long,
    val capturedAt: Long,
    val heartRate: Int,
    val speedMps: Double,
    val distanceM: Double,
    val caloriesKcal: Double,
    val altitudeM: Double,
    val cadenceSpm: Int,
) {
    fun toBridgeJson(): String {
        return JSONObject()
            .put("kind", kind)
            .put("sessionId", sessionId)
            .put("seq", seq)
            .put("capturedAt", capturedAt)
            .put("timestamp", capturedAt)
            .put("heartRate", heartRate)
            .put("speedMps", speedMps)
            .put("distanceM", distanceM)
            .put("caloriesKcal", caloriesKcal)
            .put("altitudeM", altitudeM)
            .put("cadenceSpm", cadenceSpm)
            .toString()
    }

    companion object {
        fun fromJson(raw: String): WatchSyncPayload? {
            return runCatching {
                val json = JSONObject(raw)
                WatchSyncPayload(
                    kind = json.getString("kind"),
                    sessionId = json.getString("sessionId"),
                    seq = json.getLong("seq"),
                    capturedAt = json.optLong("capturedAt", json.getLong("timestamp")),
                    heartRate = json.getInt("heartRate"),
                    speedMps = json.getDouble("speedMps"),
                    distanceM = json.getDouble("distanceM"),
                    caloriesKcal = json.getDouble("caloriesKcal"),
                    altitudeM = json.getDouble("altitudeM"),
                    cadenceSpm = json.getInt("cadenceSpm"),
                )
            }.getOrNull()
        }
    }
}
