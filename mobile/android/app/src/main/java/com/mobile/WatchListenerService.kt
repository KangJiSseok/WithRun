package com.mobile

import android.util.Log
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

class WatchListenerService : WearableListenerService() {

    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d("WatchBridge", "[1] WatchListenerService.onMessageReceived: path=${messageEvent.path}")
        when (messageEvent.path) {
            WatchSyncPaths.livePath, WatchSyncPaths.catchupPath -> {
                handlePayload(String(messageEvent.data), messageEvent.path)
            }

            else -> Log.w("WatchBridge", "[1] 알 수 없는 path 무시됨: ${messageEvent.path}")
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type != DataEvent.TYPE_CHANGED) {
                continue
            }

            val path = event.dataItem.uri.path ?: continue
            if (path != WatchSyncPaths.snapshotPath) {
                continue
            }

            val payload = DataMapItem.fromDataItem(event.dataItem)
                .dataMap
                .getString(WatchSyncPaths.snapshotPayloadKey)
                ?: continue
            handlePayload(payload, path)
        }
    }

    private fun handlePayload(rawPayload: String, sourcePath: String) {
        Log.d("WatchBridge", "[2] payload 수신 완료: path=$sourcePath payload=$rawPayload")
        val normalized = WatchSyncState.accept(rawPayload)
        if (normalized == null) {
            Log.d("WatchBridge", "[2] 중복 또는 파싱 실패로 무시됨")
            return
        }
        WatchBridgeModule.emitToJs(normalized)
    }
}
