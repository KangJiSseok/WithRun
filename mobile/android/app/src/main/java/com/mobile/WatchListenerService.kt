package com.mobile

import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

class WatchListenerService : WearableListenerService() {

    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d("WatchBridge", "[1] WatchListenerService.onMessageReceived: path=${messageEvent.path}")
        if (messageEvent.path == "/watch-to-phone") {
            val message = String(messageEvent.data)
            Log.d("WatchBridge", "[2] 메시지 수신 완료: $message")
            // WatchBridgeModule의 companion object를 통해 JS로 이벤트 emit
            WatchBridgeModule.emitToJs(message)
        } else {
            Log.w("WatchBridge", "[1] 알 수 없는 path 무시됨: ${messageEvent.path}")
        }
    }
}
