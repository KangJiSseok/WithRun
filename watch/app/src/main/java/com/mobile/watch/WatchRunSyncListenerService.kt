package com.mobile.watch

import android.content.Intent
import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

class WatchRunSyncListenerService : WearableListenerService() {

    override fun onMessageReceived(messageEvent: MessageEvent) {
        val path = messageEvent.path
        val message = String(messageEvent.data)

        when (path) {
            "/run/start" -> {
                Log.i("WR_RunSync", "run start 수신")
                WatchRunSyncState.startFromJson(message)
                startService(Intent(this, ExerciseService::class.java).apply {
                    action = ExerciseService.ACTION_START
                })
            }
            "/run/stop" -> {
                Log.i("WR_RunSync", "run stop 수신")
                WatchRunSyncState.stop()
                startService(Intent(this, ExerciseService::class.java).apply {
                    action = ExerciseService.ACTION_STOP
                })
            }
            "/run/gps" -> {
                WatchRunSyncState.updateGpsFromJson(message)
            }
            else -> {
                Log.d("WR_RunSync", "미사용 path 무시: $path")
            }
        }
    }
}

