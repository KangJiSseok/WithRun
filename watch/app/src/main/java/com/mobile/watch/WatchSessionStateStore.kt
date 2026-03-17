package com.mobile.watch

import android.content.Context

internal data class WatchSessionState(
    val sessionId: String,
    val nextSeq: Long,
    val lastSnapshotAt: Long,
)

internal class WatchSessionStateStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): WatchSessionState? {
        val sessionId = preferences.getString(KEY_SESSION_ID, null) ?: return null
        val nextSeq = preferences.getLong(KEY_NEXT_SEQ, 1L)
        val lastSnapshotAt = preferences.getLong(KEY_LAST_SNAPSHOT_AT, 0L)
        return WatchSessionState(
            sessionId = sessionId,
            nextSeq = nextSeq,
            lastSnapshotAt = lastSnapshotAt,
        )
    }

    fun save(state: WatchSessionState) {
        preferences.edit()
            .putString(KEY_SESSION_ID, state.sessionId)
            .putLong(KEY_NEXT_SEQ, state.nextSeq)
            .putLong(KEY_LAST_SNAPSHOT_AT, state.lastSnapshotAt)
            .apply()
    }

    fun clear() {
        preferences.edit()
            .remove(KEY_SESSION_ID)
            .remove(KEY_NEXT_SEQ)
            .remove(KEY_LAST_SNAPSHOT_AT)
            .apply()
    }

    private companion object {
        private const val PREFS_NAME = "watch-session-state"
        private const val KEY_SESSION_ID = "session_id"
        private const val KEY_NEXT_SEQ = "next_seq"
        private const val KEY_LAST_SNAPSHOT_AT = "last_snapshot_at"
    }
}
