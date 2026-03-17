package com.mobile.watch

import org.json.JSONObject

internal object WatchSyncPaths {
    const val livePath = "/health-data/live"
    const val snapshotPath = "/health-data/snapshot"
    const val catchupPath = "/health-data/catchup"
    const val catchupRequestPath = "/health-data/request-catchup"
    const val snapshotPayloadKey = "payload"
}

internal enum class WatchPacketKind(val value: String) {
    LIVE("live"),
    SNAPSHOT("snapshot"),
    CATCHUP("catchup"),
}

internal data class WatchMetrics(
    val heartRate: Int,
    val speedMps: Double,
    val distanceM: Double,
    val caloriesKcal: Double,
    val altitudeM: Double,
    val cadenceSpm: Int,
)

internal data class WatchSyncPacket(
    val kind: WatchPacketKind,
    val sessionId: String,
    val seq: Long,
    val capturedAt: Long,
    val metrics: WatchMetrics,
) {
    fun toJsonString(): String {
        return JSONObject()
            .put("kind", kind.value)
            .put("sessionId", sessionId)
            .put("seq", seq)
            .put("capturedAt", capturedAt)
            .put("timestamp", capturedAt)
            .put("heartRate", metrics.heartRate)
            .put("speedMps", metrics.speedMps)
            .put("distanceM", metrics.distanceM)
            .put("caloriesKcal", metrics.caloriesKcal)
            .put("altitudeM", metrics.altitudeM)
            .put("cadenceSpm", metrics.cadenceSpm)
            .toString()
    }

    fun withKind(nextKind: WatchPacketKind): WatchSyncPacket = copy(kind = nextKind)
}
