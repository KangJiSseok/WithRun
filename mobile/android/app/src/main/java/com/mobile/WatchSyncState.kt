package com.mobile

object WatchSyncState {
    @Volatile
    private var currentSessionId: String? = null

    @Volatile
    private var lastSeenSeq: Long = 0L

    @Volatile
    private var latestPayloadJson: String? = null

    @Synchronized
    fun accept(rawPayload: String): String? {
        val payload = WatchSyncPayload.fromJson(rawPayload) ?: return null
        if (currentSessionId != payload.sessionId) {
            currentSessionId = payload.sessionId
            lastSeenSeq = 0L
            latestPayloadJson = null
        }

        if (payload.seq <= lastSeenSeq) {
            return null
        }

        lastSeenSeq = payload.seq
        val normalized = payload.toBridgeJson()
        latestPayloadJson = normalized
        return normalized
    }

    fun currentLastSeenSeq(): Long = lastSeenSeq

    fun latestPayloadJson(): String? = latestPayloadJson
}
